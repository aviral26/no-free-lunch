package rocky.raft.server;

import com.google.gson.Gson;
import rocky.raft.common.Config;
import rocky.raft.common.Constants;
import rocky.raft.common.TimeoutListener;
import rocky.raft.common.TimeoutManager;
import rocky.raft.dto.*;
import rocky.raft.log.Log;
import rocky.raft.utils.LogUtils;
import rocky.raft.utils.Utils;

import java.io.IOException;
import java.net.Socket;
import java.util.List;

public class LeaderLogic extends BaseLogic {

    private static String LOG_TAG = "LEADER_LOGIC-";

    private int clusterSize;

    private int[] nextIndex;

    private int[] matchIndex;

    public LeaderLogic(ServerContext serverContext, TimeoutListener timeoutListener) throws IOException {
        super(serverContext);
        LOG_TAG += serverContext.getId();

        clusterSize = Config.SERVERS.size();
        nextIndex = new int[clusterSize];
        matchIndex = new int[clusterSize];

        try {
            LogEntry entry = serverContext.getLog().last();
            int index = entry == null ? 0 : entry.getIndex();

            for (int i = 0; i < clusterSize; ++i) {
                nextIndex[i] = index + 1;
                matchIndex[i] = 0;
            }
        } catch (Exception e) {
            LogUtils.error(LOG_TAG, "Could not read log. This will cause errors.");
        }

        sendHeartbeat();
    }

    @Override
    public void release() {
        TimeoutManager.getInstance().remove(LOG_TAG);
    }

    @Override
    protected Message handleClient(Message message, ServerContext serverContext) throws Exception {
        return null;
    }

    @Override
    protected Message handleServer(Message message, ServerContext serverContext) throws Exception {
        return null;
    }

    private void sendHeartbeat() {
        LogUtils.debug(LOG_TAG, "Sending heartbeat to followers");

        int leaderId = serverContext.getId();

        for (int id = 0; id < clusterSize; ++id) {
            if (id != leaderId) {
                final int followerId = id;
                Utils.startThread("heartbeat-f" + followerId, () -> {
                    try {
                        doSendHeartbeat(followerId);
                    } catch (Exception e) {
                        LogUtils.error(LOG_TAG, "Error occurred in sending heartbeat to " + followerId, e);
                    }
                });
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

    private void doSendHeartbeat(int followerId) throws Exception {
        LogEntry last = serverContext.getLog().last();
        int index = last == null ? 0 : last.getIndex();
        int term = serverContext.getCurrentTerm();
        int id = serverContext.getId();
        int commitIndex = serverContext.getCommitIndex();

        if (index >= nextIndex[followerId]) {
            Address address = Config.SERVERS.get(followerId);
            Socket socket = new Socket(address.getIp(), address.getServerPort());

            // Prepare message
            Message message = new Message(Message.Sender.SERVER, Message.Type.APPEND_ENTRIES_RPC);
            int prevLogIndex = nextIndex[followerId] - 1;
            LogEntry prevEntry = serverContext.getLog().get(prevLogIndex);
            int prevLogTerm = prevEntry == null ? 0 : prevEntry.getTerm();
            List<LogEntry> entries = serverContext.getLog().getAll(nextIndex[followerId]);
            AppendEntriesRpc appendEntriesRpc = new AppendEntriesRpc.Builder()
                    .setTerm(term)
                    .setLeaderId(id)
                    .setPrevLogIndex(prevLogIndex)
                    .setPrevLogTerm(prevLogTerm)
                    .setEntries(entries)
                    .setLeaderCommit(commitIndex).build();
            message.setMessage(new Gson().toJson(appendEntriesRpc));

            // Send AppendEntriesRpc
            Utils.getOos(socket).writeObject(message);

            // Get AppendEntriesRpcReply
            Message reply = (Message) Utils.getOis(socket).readObject();
            if (reply.getStatus() != Message.Status.OK) {
                throw new Exception("Received error message from follower(" + followerId + "): " + reply);
            }

            // Process reply
            AppendEntriesRpcReply appendEntriesRpcReply = new Gson().fromJson(reply.getMessage(), AppendEntriesRpcReply.class);
            if (appendEntriesRpcReply.isSuccess()) {
                nextIndex[followerId] = index;
                matchIndex[followerId] = index;
            } else {
                nextIndex[followerId]--;
            }

            // Close connection
            Utils.closeQuietly(socket);
        }
    }

    private void updateCommitIndex(ServerContext serverContext) throws IOException {
        Log log = serverContext.getLog();
        LogEntry last = log.last();
        int lastIndex = last == null ? 0 : last.getIndex();
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
                    break;
                }
            }
        }
    }
}
