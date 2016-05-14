package rocky.raft.server;

import rocky.raft.common.Config;
import rocky.raft.common.TimeoutListener;
import rocky.raft.common.TimeoutManager;
import rocky.raft.dto.Message;
import rocky.raft.dto.RequestVoteRpc;
import rocky.raft.dto.RequestVoteRpcReply;
import rocky.raft.utils.LogUtils;
import rocky.raft.utils.NetworkUtils;

import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CandidateLogic extends BaseLogic {

    public interface OnMajorityReachedListener {
        void onMajorityReached();
    }

    private String LOG_TAG = "CANDIDATE_LOGIC-";
    private ExecutorService voteExecutor;
    private TimeoutListener timeoutListener;
    private OnMajorityReachedListener onMajorityReachedListener;
    private int clusterSize;
    private int voteCount;

    CandidateLogic(ServerContext serverContext, TimeoutListener timeoutListener, OnMajorityReachedListener onMajorityReachedListener) {
        super(serverContext);
        LOG_TAG += serverContext.getId();
        this.timeoutListener = timeoutListener;
        this.onMajorityReachedListener = onMajorityReachedListener;
        this.clusterSize = Config.SERVERS.size();
        this.voteCount = 0;
        serverContext.setLeaderAddress(null);

        resetVoteExecutor();
    }

    @Override
    public void init() {
        // Increment term, start election, vote for myself and set timeout thread.
        startElectionAndSetTimeout();
    }

    private void resetVoteExecutor() {
        if (voteExecutor != null) voteExecutor.shutdownNow();
        voteExecutor = Executors.newFixedThreadPool(Math.max(1, clusterSize - 1));
    }

    private void startElectionAndSetTimeout() {
        TimeoutManager.getInstance().add(LOG_TAG, timeoutListener::onTimeout, getElectionTimeout());

        serverContext.setCurrentTerm(serverContext.getCurrentTerm() + 1);

        for (int i = 0; i < Config.SERVERS.size(); i++) {
            if (i == serverContext.getId()) {
                incrementVoteCount();
            } else {
                voteExecutor.submit(new SendVoteRequest(i));
            }
        }
    }

    private synchronized void incrementVoteCount() {
        voteCount++;
        if (voteCount > clusterSize / 2) {
            onMajorityReachedListener.onMajorityReached();
        }
    }

    @Override
    public void release() {
        voteExecutor.shutdownNow();
        TimeoutManager.getInstance().remove(LOG_TAG);
    }

    @Override
    protected Message handleMessage(Message message, ServerContext serverContext) throws Exception {
        switch (message.getType()) {

            case GET_LEADER_ADDR:
                LogUtils.debug(LOG_TAG, "Leader not elected yet. Returning null.");
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

            default:
                LogUtils.error(LOG_TAG, "Unknown message. Returning null.");
        }
        return null;
    }

    private class SendVoteRequest implements Runnable {

        private int sendTo;

        public SendVoteRequest(int id) {
            this.sendTo = id;
        }

        @Override
        public void run() {
            Socket socket = null;

            try {
                socket = new Socket(Config.SERVERS.get(sendTo).getIp(), Config.SERVERS.get(sendTo).getServerPort());
                NetworkUtils.writeMessage(socket, new Message.Builder().setType(Message.Type.REQUEST_VOTE_RPC)
                        .setMeta(new RequestVoteRpc(serverContext.getCurrentTerm(), serverContext.getId(), serverContext.getLastIndex(), serverContext.getLastTerm())).build());
                Message reply = NetworkUtils.readMessage(socket);
                if (reply.getStatus() == Message.Status.OK) {
                    RequestVoteRpcReply requestVoteRpcReply = (RequestVoteRpcReply) reply.getMeta();
                    if (requestVoteRpcReply.isVoteGranted()) {
                        incrementVoteCount();
                    }
                }
            } catch (Exception e) {
                LogUtils.error(LOG_TAG, "Could not send vote request to " + Config.SERVERS.get(sendTo));
            } finally {
                NetworkUtils.closeQuietly(socket);
            }
        }
    }
}
