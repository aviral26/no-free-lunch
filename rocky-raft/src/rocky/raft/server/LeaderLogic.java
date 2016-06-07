package rocky.raft.server;

import com.google.gson.Gson;
import rocky.raft.common.Config;
import rocky.raft.common.Constants;
import rocky.raft.common.InterruptibleSemaphore;
import rocky.raft.common.TimeoutManager;
import rocky.raft.dto.*;
import rocky.raft.log.Log;
import rocky.raft.utils.LogUtils;
import rocky.raft.utils.NetworkUtils;
import rocky.raft.utils.Utils;

import java.io.IOException;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;

class LeaderLogic extends BaseLogic {

    private String LOG_TAG = "LEADER_LOGIC-";

    private ExecutorService heartbeatExecutor;

    private Map<Integer, Integer> nextIndex;

    private Map<Integer, Integer> matchIndex;

    private Map<Integer, InterruptibleSemaphore> semaphoreMap = new ConcurrentHashMap<>();

    private Set<InterruptibleSemaphore> readSemaphores = Collections.synchronizedSet(new HashSet<>());

    private List<ServerConfig> heartbeatSentTo;

    LeaderLogic(ServerContext serverContext) {
        super(serverContext);
        LOG_TAG += serverContext.getId();

        serverContext.setLeaderConfig(serverContext.getServerConfig());
        nextIndex = new HashMap<>();
        matchIndex = new HashMap<>();
        heartbeatSentTo = new ArrayList<>();

        // If current configuration is joint, commit new configuration.
        final Config config = serverContext.getConfig();
        Utils.startThread("commit-new-config", () -> {
            if (config.isJointConfig()) {
                try {
                    LogEntry entry = generateLogEntry(new Gson().toJson(config), Utils.getRandomUuid(), true);
                    commitToLog(entry);
                    LogUtils.debug(LOG_TAG, "Committed new config");
                } catch (Exception e) {
                    LogUtils.error(LOG_TAG, "Failed to commit new config", e);
                }
            }
        });
    }

    @Override
    public void init() {
        sendHeartbeat();
    }

    private synchronized void resetHeartbeat() {
        if (heartbeatExecutor != null) heartbeatExecutor.shutdownNow();
        heartbeatExecutor = Executors.newFixedThreadPool(Math.max(1, serverContext.getConfig().getAll().size() - 1));
        heartbeatSentTo.clear();
    }

    @Override
    public void release() {
        heartbeatExecutor.shutdownNow();
        interruptAllClients();
        TimeoutManager.getInstance().remove(LOG_TAG);
    }

    @Override
    protected Message handleMessage(Message message, ServerContext serverContext) throws Exception {
        LogEntry entry;
        switch (message.getType()) {
            case GET_POSTS:
                waitForMajorityHeartbeats();
                return getPostsReply(serverContext.getLog());

            case DO_POST:
                DoPost doPost = (DoPost) message.getMeta();
                entry = generateLogEntry(doPost.getPost(), doPost.getId(), false);
                if (commitToLog(entry)) {
                    waitForCommit(entry);
                }
                return new Message.Builder().setType(Message.Type.DO_POST_REPLY)
                        .setStatus(Message.Status.OK).build();

            case CHANGE_CONFIG:
                ChangeConfig changeConfig = (ChangeConfig) message.getMeta();
                Config oldConfig = serverContext.getConfig();
                Config newConfig = changeConfig.getConfig();
                Config jointConfig = new Config(oldConfig.getServerConfigs(), newConfig.getServerConfigs(), true);

                entry = generateLogEntry(new Gson().toJson(jointConfig), changeConfig.getId(), true);
                if (commitToLog(entry)) {
                    waitForCommit(entry);
                }
                LogUtils.debug(LOG_TAG, "Joint configuration committed.");

                entry = generateLogEntry(new Gson().toJson(newConfig), Utils.getRandomUuid(), true);
                if (commitToLog(entry)) {
                    waitForCommit(entry);
                }
                LogUtils.debug(LOG_TAG, "New configuration committed.");

                return new Message.Builder().setType(Message.Type.CHANGE_CONFIG_REPLY)
                        .setStatus(Message.Status.OK).build();
        }
        return null;
    }

    private synchronized void onHeartbeatSent(ServerConfig serverConfig) {
        heartbeatSentTo.add(serverConfig);
        if (serverContext.getConfig().isMajority(heartbeatSentTo)) {
            Set<InterruptibleSemaphore> readSemaphoresCopy = new HashSet<>(readSemaphores);
            readSemaphoresCopy.forEach(InterruptibleSemaphore::release);
        }
    }

    private void interruptAllClients() {
        semaphoreMap.values().forEach(InterruptibleSemaphore::interrupt);
        Set<InterruptibleSemaphore> readSemaphoresCopy = new HashSet<>(readSemaphores);
        readSemaphoresCopy.forEach(InterruptibleSemaphore::interrupt);
    }

    private void waitForMajorityHeartbeats() throws InterruptedException, TimeoutException {
        LogUtils.debug(LOG_TAG, "Waiting until I hear majority of heartbeats");
        InterruptibleSemaphore semaphore = new InterruptibleSemaphore(0);
        readSemaphores.add(semaphore);
        try {
            if (!semaphore.tryAcquire(Constants.CLIENT_TIMEOUT, TimeUnit.MILLISECONDS)) {
                throw new TimeoutException("Failed to hear majority of heartbeats");
            }
        } finally {
            readSemaphores.remove(semaphore);
        }
    }

    private void waitForCommit(LogEntry entry) throws InterruptedException, TimeoutException {
        LogUtils.debug(LOG_TAG, "Waiting until entry is committed");
        InterruptibleSemaphore semaphore = new InterruptibleSemaphore(0);
        semaphoreMap.put(entry.getIndex(), semaphore);
        try {
            if (!semaphore.tryAcquire(Constants.CLIENT_TIMEOUT, TimeUnit.MILLISECONDS)) {
                throw new TimeoutException("Failed to commit: " + entry);
            }
        } finally {
            semaphoreMap.remove(entry.getIndex());
        }
    }

    private LogEntry generateLogEntry(String value, String id, boolean isConfigEntry) throws Exception {
        int lastIndex = serverContext.getLastIndex();
        int term = serverContext.getCurrentTerm();
        return new LogEntry(lastIndex + 1, term, value, id, isConfigEntry);
    }

    private synchronized boolean commitToLog(LogEntry entry) throws Exception {
        int leaderId = serverContext.getId();
        boolean appended = serverContext.getLog().append(entry);
        setNextAndMatchIndex(leaderId, entry.getIndex());
        return appended;
    }

    private synchronized void sendHeartbeat() {
        // Terminate previous heartbeats
        resetHeartbeat();

        try {
            // Send new heartbeats
//            LogUtils.debug(LOG_TAG, "Sending heartbeat...");
            int index = serverContext.getLastIndex();
            int term = serverContext.getCurrentTerm();
            int leaderId = serverContext.getId();
            int commitIndex = serverContext.getCommitIndex();

            for (ServerConfig serverConfig : serverContext.getConfig().getAll()) {
                int followerId = serverConfig.getId();
                if (followerId != leaderId) {
                    heartbeatExecutor.submit(new SendHeartbeatTask(followerId, index, term, leaderId, commitIndex));
                } else {
                    onHeartbeatSent(serverConfig);
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
        semaphoreMap.keySet().stream().filter(index -> index <= newCommitIndex).forEach(index -> semaphoreMap.get(index).release());
    }

    private void updateCommitIndex(ServerContext serverContext) throws IOException {
        Config config = serverContext.getConfig();
        Log log = serverContext.getLog();
        int lastIndex = serverContext.getLastIndex();
        int commitIndex = serverContext.getCommitIndex();
        int currentTerm = serverContext.getCurrentTerm();
        List<ServerConfig> replicas = new ArrayList<>();

        for (int n = lastIndex; n > commitIndex; --n) {
            for (ServerConfig serverConfig : config.getAll()) {
                if (matchIndex.getOrDefault(serverConfig.getId(), 0) >= n) replicas.add(serverConfig);
            }

            boolean majority = config.isMajority(replicas);
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
        nextIndex.put(id, index + 1);
        matchIndex.put(id, index);
    }

    private class SendHeartbeatTask implements Runnable {

        private int followerId;

        private int index;

        private int term;

        private int leaderId;

        private int commitIndex;

        public SendHeartbeatTask(int followerId, int index, int term, int leaderId, int commitIndex) {
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
                onHeartbeatSent(serverContext.getConfig().getServerConfig(followerId));
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

        private Message prepareHeartbeatMessage(int nextIndexDefault) throws IOException {
            int prevLogIndex = nextIndex.getOrDefault(followerId, nextIndexDefault) - 1;
            LogEntry prevEntry = serverContext.getLog().get(prevLogIndex);
            int prevLogTerm = prevEntry == null ? 0 : prevEntry.getTerm();
            List<LogEntry> entries = new ArrayList<>();
            Message.Builder message = new Message.Builder().setType(Message.Type.APPEND_ENTRIES_RPC);

            if (index >= nextIndex.getOrDefault(followerId, nextIndexDefault)) {
                // Prepare entries
                entries = serverContext.getLog().getAll(nextIndex.getOrDefault(followerId, nextIndexDefault));
            }

            message.setMeta(new AppendEntriesRpc(term, leaderId, prevLogIndex, prevLogTerm, entries, commitIndex));

            return message.build();
        }

        private void doSendHeartbeat() throws Exception {
            Socket socket = null;

            try {
                Address address = serverContext.getConfig().getServer(followerId);
                socket = new Socket(address.getIp(), address.getServerPort());

                // Send AppendEntriesRpc
                int nextIndexDefault = serverContext.getLastIndex() + 1;
                NetworkUtils.writeMessage(socket, prepareHeartbeatMessage(nextIndexDefault));
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
                    nextIndex.put(followerId, nextIndex.getOrDefault(followerId, nextIndexDefault) - 1);
                }
            } finally {
                // Close connection
                NetworkUtils.closeQuietly(socket);
            }
        }
    }
}
