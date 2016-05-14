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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LeaderLogic extends BaseLogic {

    private String LOG_TAG = "LEADER_LOGIC-";

    private ExecutorService hearbeatExecutor;

    private int clusterSize;

    private int[] nextIndex;

    private int[] matchIndex;

    private Map<Integer, InterruptibleSemaphore> semaphoreMap = new ConcurrentHashMap<>();

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

    private void resetHeartbeatExecutor() {
        if (hearbeatExecutor != null) hearbeatExecutor.shutdownNow();
        hearbeatExecutor = Executors.newFixedThreadPool(Math.max(1, clusterSize - 1));
    }

    @Override
    public void release() {
        hearbeatExecutor.shutdownNow();
        interruptAllClients();
        TimeoutManager.getInstance().remove(LOG_TAG);
    }

    @Override
    protected Message handleMessage(Message message, ServerContext serverContext) throws Exception {
        switch (message.getType()) {
            case DO_POST:
                String post = ((DoPost) message.getMeta()).getPost();
                LogEntry entry = doPost(post);
                waitForCommit(entry);
                return new Message.Builder().setType(Message.Type.DO_POST_REPLY)
                        .setStatus(Message.Status.OK).build();
        }
        return null;
    }

    private void interruptAllClients() {
        for (InterruptibleSemaphore semaphore : semaphoreMap.values()) {
            semaphore.interrupt();
        }
    }

    private void waitForCommit(LogEntry entry) throws InterruptedException {
        LogUtils.debug(LOG_TAG, "Waiting until entry is committed");
        InterruptibleSemaphore semaphore = new InterruptibleSemaphore(0);
        semaphoreMap.put(entry.getIndex(), semaphore);
        try {
            semaphore.acquire();
            // TODO No timeout here. What to do?
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
        nextIndex[leaderId] = lastIndex + 1;
        matchIndex[leaderId] = lastIndex + 1;
        return entry;
    }

    private void sendHeartbeat() {
        // Terminate previous heartbeats
        resetHeartbeatExecutor();

        // Send new heartbeats
        LogUtils.debug(LOG_TAG, "Sending heartbeat to followers");
        int leaderId = serverContext.getId();

        for (int id = 0; id < clusterSize; ++id) {
            if (id != leaderId) {
                hearbeatExecutor.submit(new SendHearbeatTask(id));
            }
        }

        try {
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
                LogEntry entry = log.get(n - 1);
                if (entry != null && entry.getTerm() == currentTerm) {
                    LogUtils.debug(LOG_TAG, "Updating commitIndex to " + n);
                    serverContext.setCommitIndex(n);
                    notifyClients(n);
                    break;
                }
            }
        }
    }

    private class SendHearbeatTask implements Runnable {

        private int followerId;

        public SendHearbeatTask(int followerId) {
            this.followerId = followerId;
        }

        @Override
        public void run() {
            try {
                doSendHeartbeat(followerId);
            } catch (Exception e) {
                LogUtils.error(LOG_TAG, "Error occurred in sending heartbeat to " + followerId, e);
            }
        }

        private void doSendHeartbeat(int followerId) throws Exception {
            int index = serverContext.getLastIndex();
            int term = serverContext.getCurrentTerm();
            int id = serverContext.getId();
            int commitIndex = serverContext.getCommitIndex();

            if (index >= nextIndex[followerId]) {
                Address address = Config.SERVERS.get(followerId);
                Socket socket = new Socket(address.getIp(), address.getServerPort());

                // Prepare message
                int prevLogIndex = nextIndex[followerId] - 1;
                LogEntry prevEntry = serverContext.getLog().get(prevLogIndex);
                int prevLogTerm = prevEntry == null ? 0 : prevEntry.getTerm();
                List<LogEntry> entries = serverContext.getLog().getAll(nextIndex[followerId]);
                Message message = new Message.Builder().setType(Message.Type.APPEND_ENTRIES_RPC)
                        .setMeta(new AppendEntriesRpc(term, id, prevLogIndex, prevLogTerm, entries, commitIndex)).build();

                // Send AppendEntriesRpc
                NetworkUtils.writeMessage(socket, message);

                // Get AppendEntriesRpcReply
                Message reply = NetworkUtils.readMessage(socket);
                if (reply.getStatus() != Message.Status.OK) {
                    throw new Exception("Received error message from follower(" + followerId + "): " + reply);
                }

                // Process reply
                AppendEntriesRpcReply appendEntriesRpcReply = (AppendEntriesRpcReply) reply.getMeta();
                if (appendEntriesRpcReply.isSuccess()) {
                    nextIndex[followerId] = index;
                    matchIndex[followerId] = index;
                } else {
                    nextIndex[followerId]--;
                }

                // Close connection
                NetworkUtils.closeQuietly(socket);
            }
        }
    }
}
