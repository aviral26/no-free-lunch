package rocky.raft.server;

import rocky.raft.common.Config;
import rocky.raft.common.Constants;
import rocky.raft.common.InterruptibleSemaphore;
import rocky.raft.common.TimeoutManager;
import rocky.raft.dto.*;
import rocky.raft.log.Log;
import rocky.raft.utils.LogUtils;
import rocky.raft.utils.NetworkUtils;

import java.io.IOException;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class LeaderLogic extends BaseLogic {

    private String LOG_TAG = "LEADER_LOGIC-";

    private ExecutorService heartbeatExecutor;

    private int clusterSize;

    private int[] nextIndex;

    private int[] matchIndex;

    private Map<Integer, InterruptibleSemaphore> semaphoreMap = new ConcurrentHashMap<>();

    private Set<InterruptibleSemaphore> readSemaphores = Collections.synchronizedSet(new HashSet<>());

    private int heartbeatCounter;

    public LeaderLogic(ServerContext serverContext) {
        super(serverContext);
        LOG_TAG += serverContext.getId();

        serverContext.setLeaderAddress(serverContext.getAddress());
        clusterSize = Config.SERVERS.size();
        nextIndex = new int[clusterSize];
        matchIndex = new int[clusterSize];

        try {
            int index = serverContext.getLastIndex();

            for (int i = 0; i < clusterSize; ++i) {
                nextIndex[i] = index + 1;
                matchIndex[i] = 0;
            }
        } catch (Exception e) {
            LogUtils.error(LOG_TAG, "Could not read log. This will cause errors.");
        }
    }

    @Override
    public void init() {
        sendHeartbeat();
    }

    private synchronized void resetHeartbeat() {
        if (heartbeatExecutor != null) heartbeatExecutor.shutdownNow();
        heartbeatExecutor = Executors.newFixedThreadPool(Math.max(1, clusterSize - 1));

        heartbeatCounter = 0;
    }

    @Override
    public void release() {
        heartbeatExecutor.shutdownNow();
        interruptAllClients();
        TimeoutManager.getInstance().remove(LOG_TAG);
    }

    @Override
    protected Message handleMessage(Message message, ServerContext serverContext) throws Exception {
        switch (message.getType()) {
            case GET_POSTS:
                waitForMajorityHeartbeats();
                return getPostsReply(serverContext.getLog());
            case DO_POST:
                String post = ((DoPost) message.getMeta()).getPost();
                LogEntry entry = doPost(post);
                waitForCommit(entry);
                return new Message.Builder().setType(Message.Type.DO_POST_REPLY)
                        .setStatus(Message.Status.OK).build();
        }
        return null;
    }

    private synchronized void incrementHeartbeatCounter() {
        heartbeatCounter++;
        if (heartbeatCounter == clusterSize / 2 + 1) {
            Set<InterruptibleSemaphore> readSemaphoresCopy = new HashSet<>(readSemaphores);
            for (InterruptibleSemaphore semaphore : readSemaphoresCopy) {
                semaphore.release();
            }
        }
    }

    private void interruptAllClients() {
        for (InterruptibleSemaphore semaphore : semaphoreMap.values()) {
            semaphore.interrupt();
        }

        Set<InterruptibleSemaphore> readSemaphoresCopy = new HashSet<>(readSemaphores);
        for (InterruptibleSemaphore semaphore : readSemaphoresCopy) {
            semaphore.interrupt();
        }
    }

    private void waitForMajorityHeartbeats() throws InterruptedException {
        LogUtils.debug(LOG_TAG, "Waiting until I hear majority of heartbeats");
        InterruptibleSemaphore semaphore = new InterruptibleSemaphore(0);
        readSemaphores.add(semaphore);
        try {
            semaphore.tryAcquire(Constants.CLIENT_TIMEOUT, TimeUnit.MILLISECONDS);
        } finally {
            readSemaphores.remove(semaphore);
        }
    }

    private void waitForCommit(LogEntry entry) throws InterruptedException {
        LogUtils.debug(LOG_TAG, "Waiting until entry is committed");
        InterruptibleSemaphore semaphore = new InterruptibleSemaphore(0);
        semaphoreMap.put(entry.getIndex(), semaphore);
        try {
            semaphore.tryAcquire(Constants.CLIENT_TIMEOUT, TimeUnit.MILLISECONDS);
            // TODO Any cleanup to do on timeout?
        } finally {
            semaphoreMap.remove(entry.getIndex());
        }
    }

    private synchronized LogEntry doPost(String post) throws Exception {
        int lastIndex = serverContext.getLastIndex();
        int term = serverContext.getCurrentTerm();
        int leaderId = serverContext.getId();

        LogEntry entry = new LogEntry(lastIndex + 1, term, post);
        serverContext.getLog().append(entry);
        setNextAndMatchIndex(leaderId, lastIndex + 1);
        return entry;
    }

    private synchronized void sendHeartbeat() {
        // Terminate previous heartbeats
        resetHeartbeat();

        try {
            // Send new heartbeats
            LogUtils.debug(LOG_TAG, "Sending heartbeat to followers");
            int index = serverContext.getLastIndex();
            int term = serverContext.getCurrentTerm();
            int leaderId = serverContext.getId();
            int commitIndex = serverContext.getCommitIndex();

            for (int followerId = 0; followerId < clusterSize; ++followerId) {
                if (followerId != leaderId) {
                    heartbeatExecutor.submit(new SendHearbeatTask(followerId, index, term, leaderId, commitIndex));
                }
            }

            // Update commit index
            updateCommitIndex(serverContext);
        } catch (IOException e) {
            LogUtils.error(LOG_TAG, "Failed to update commitIndex", e);
        }

        // Enqueue next heartbeat
        TimeoutManager.getInstance().add(LOG_TAG, this::sendHeartbeat, Constants.HEARTBEAT_DELAY);
    }

    private void notifyClients(int newCommitIndex) {
        for (Integer index : semaphoreMap.keySet()) {
            if (index <= newCommitIndex) {
                semaphoreMap.get(index).release();
            }
        }
    }

    private void updateCommitIndex(ServerContext serverContext) throws IOException {
        Log log = serverContext.getLog();
        int lastIndex = serverContext.getLastIndex();
        int commitIndex = serverContext.getCommitIndex();
        int currentTerm = serverContext.getCurrentTerm();

        for (int n = lastIndex; n > commitIndex; --n) {
            int count = 0;
            for (int i = 0; i < clusterSize; ++i) {
                if (matchIndex[i] >= n) count++;
            }

            boolean majority = count > clusterSize / 2;
            if (majority) {
                LogEntry entry = log.get(n);
                if (entry != null && entry.getTerm() == currentTerm) {
                    LogUtils.debug(LOG_TAG, "Updating commitIndex to " + n);
                    serverContext.setCommitIndex(n);
                    notifyClients(n);
                    break;
                }
            }
        }
    }

    private void setNextAndMatchIndex(int id, int index) {
        nextIndex[id] = index + 1;
        matchIndex[id] = index;
    }

    private class SendHearbeatTask implements Runnable {

        private int followerId;

        private int index;

        private int term;

        private int leaderId;

        private int commitIndex;

        public SendHearbeatTask(int followerId, int index, int term, int leaderId, int commitIndex) {
            this.followerId = followerId;
            this.index = index;
            this.term = term;
            this.leaderId = leaderId;
            this.commitIndex = commitIndex;
        }

        @Override
        public void run() {
            try {
                doSendHeartbeat();
                incrementHeartbeatCounter();
            } catch (Exception e) {
                if (!(e instanceof InterruptedException)) {
                    LogUtils.error(LOG_TAG, "Error occurred in sending heartbeat to " + followerId + ": " + e.getMessage());
                }
            }
        }

        private void throwIfInterrupted() throws InterruptedException {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
        }

        private Message prepareHeartbeatMessage() throws IOException {
            int prevLogIndex = nextIndex[followerId] - 1;
            LogEntry prevEntry = serverContext.getLog().get(prevLogIndex);
            int prevLogTerm = prevEntry == null ? 0 : prevEntry.getTerm();
            List<LogEntry> entries = new ArrayList<>();
            Message.Builder message = new Message.Builder().setType(Message.Type.APPEND_ENTRIES_RPC);

            if (index >= nextIndex[followerId]) {
                // Prepare entries
                entries = serverContext.getLog().getAll(nextIndex[followerId]);
            }

            message.setMeta(new AppendEntriesRpc(term, leaderId, prevLogIndex, prevLogTerm, entries, commitIndex));

            return message.build();
        }

        private void doSendHeartbeat() throws Exception {
            Socket socket = null;

            try {
                Address address = Config.SERVERS.get(followerId);
                socket = new Socket(address.getIp(), address.getServerPort());

                // Send AppendEntriesRpc
                NetworkUtils.writeMessage(socket, prepareHeartbeatMessage());
                throwIfInterrupted();

                // Get AppendEntriesRpcReply
                Message reply = NetworkUtils.readMessage(socket);
                throwIfInterrupted();

                if (reply.getStatus() != Message.Status.OK) {
                    throw new Exception("Received error message from follower(" + followerId + "): " + reply);
                }

                // Process reply
                AppendEntriesRpcReply appendEntriesRpcReply = (AppendEntriesRpcReply) reply.getMeta();
                if (appendEntriesRpcReply.isSuccess()) {
                    setNextAndMatchIndex(followerId, index);
                } else {
                    nextIndex[followerId]--;
                }
            } finally {
                // Close connection
                NetworkUtils.closeQuietly(socket);
            }
        }
    }
}
