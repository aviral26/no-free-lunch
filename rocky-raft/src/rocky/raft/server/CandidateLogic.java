package rocky.raft.server;

import rocky.raft.common.TimeoutListener;
import rocky.raft.common.TimeoutManager;
import rocky.raft.dto.Message;
import rocky.raft.dto.RequestVoteRpc;
import rocky.raft.dto.RequestVoteRpcReply;
import rocky.raft.dto.ServerConfig;
import rocky.raft.utils.LogUtils;
import rocky.raft.utils.NetworkUtils;

import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CandidateLogic extends BaseLogic {

    public interface OnMajorityReachedListener {
        void onMajorityReached();
    }

    private String LOG_TAG = "CANDIDATE_LOGIC-";

    private static final int MAX_VOTER_THREADS = 5;

    private ExecutorService voteExecutor;

    private TimeoutListener timeoutListener;

    private OnMajorityReachedListener onMajorityReachedListener;

    private List<ServerConfig> voters;

    private boolean released;

    CandidateLogic(ServerContext serverContext, TimeoutListener timeoutListener, OnMajorityReachedListener onMajorityReachedListener) {
        super(serverContext);
        LOG_TAG += serverContext.getId();
        this.timeoutListener = timeoutListener;
        this.onMajorityReachedListener = onMajorityReachedListener;
        this.voters = new ArrayList<>();
        serverContext.setLeaderConfig(null);

        resetVoteExecutor();
    }

    @Override
    public void init() {
        // Increment term, start election, vote for myself and set timeout thread.
        startElectionAndSetTimeout();
    }

    private void resetVoteExecutor() {
        if (voteExecutor != null) voteExecutor.shutdownNow();
        voteExecutor = Executors.newFixedThreadPool(MAX_VOTER_THREADS);
    }

    private void startElectionAndSetTimeout() {
        TimeoutManager.getInstance().add(LOG_TAG, timeoutListener::onTimeout, getElectionTimeout());

        int newTerm = serverContext.getCurrentTerm() + 1;
        serverContext.setCurrentTerm(newTerm);
        serverContext.setVotedFor(-1);
        LogUtils.debug(LOG_TAG, "Starting election for term " + newTerm);

        for (ServerConfig serverConfig : serverContext.getConfig().getServerConfigs()) {
            int id = serverConfig.getId();
            if (id == serverContext.getId()) {
                grantVote(serverContext.getServerConfig());
            } else {
                voteExecutor.submit(new SendVoteRequest(serverConfig));
            }
        }
    }

    private synchronized void grantVote(ServerConfig serverConfig) {
        if (released) return;

        voters.add(serverConfig);
        if (serverContext.getConfig().isMajority(voters)) {
            onMajorityReachedListener.onMajorityReached();
        }
    }

    @Override
    public void release() {
        released = true;
        voteExecutor.shutdownNow();
        TimeoutManager.getInstance().remove(LOG_TAG);
    }

    @Override
    protected Message handleMessage(Message message, ServerContext serverContext) throws Exception {
        switch (message.getType()) {

            case GET_LEADER_CONFIG:
                LogUtils.debug(LOG_TAG, "Don't know about any leader yet. Returning null.");
                return null;

            case REQUEST_VOTE_RPC:
                // Must be same or lesser term. Not granting vote.
                LogUtils.debug(LOG_TAG, "Received vote request for term " + ((RequestVoteRpc) message.getMeta()).getTerm() + ". Not granting vote.");
                return new Message.Builder().setType(Message.Type.REQUEST_VOTE_RPC_REPLY).setStatus(Message.Status
                        .OK).setMeta(new RequestVoteRpcReply(serverContext.getCurrentTerm(), false)).build();

            case REQUEST_VOTE_RPC_REPLY:
                // Either these are from current term, in which case a majority should have been handled, or lesser
                // term. Can safely ignore.
                LogUtils.debug(LOG_TAG, "Received vote request reply for term " + ((RequestVoteRpc) message.getMeta()).getTerm() + ". Ignoring message.");
                break;

            default:
                LogUtils.error(LOG_TAG, "Unknown message. Returning null.");
        }
        return null;
    }

    private class SendVoteRequest implements Runnable {

        private ServerConfig serverConfig;

        public SendVoteRequest(ServerConfig serverConfig) {
            this.serverConfig = serverConfig;
        }

        @Override
        public void run() {
            Socket socket = null;

            try {
                socket = new Socket(serverConfig.getAddress().getIp(), serverConfig.getAddress().getServerPort());
                NetworkUtils.writeMessage(socket, new Message.Builder().setType(Message.Type.REQUEST_VOTE_RPC)
                        .setMeta(new RequestVoteRpc(serverContext.getCurrentTerm(), serverContext.getId(), serverContext.getLastIndex(), serverContext.getLastTerm())).build());
                Message reply = NetworkUtils.readMessage(socket);

                if (reply.getStatus() == Message.Status.OK) {
                    RequestVoteRpcReply requestVoteRpcReply = (RequestVoteRpcReply) reply.getMeta();
                    if (requestVoteRpcReply.isVoteGranted()) {
                        grantVote(serverConfig);
                    }
                }
            } catch (Exception e) {
                LogUtils.error(LOG_TAG, "Could not send vote request to " + serverConfig);
            } finally {
                NetworkUtils.closeQuietly(socket);
            }
        }
    }
}
