package rocky.raft.server;

import com.google.gson.Gson;
import rocky.raft.common.Config;
import rocky.raft.common.TimeoutListener;
import rocky.raft.common.TimeoutManager;
import rocky.raft.dto.Message;
import rocky.raft.dto.RequestVoteRpc;
import rocky.raft.utils.LogUtils;
import rocky.raft.utils.Utils;

import java.io.ObjectOutputStream;
import java.net.Socket;

public class CandidateLogic extends BaseLogic {

    private String LOG_TAG = "CANDIDATE_LOGIC-";
    private TimeoutListener timeoutListener;
    private int voteCount;

    CandidateLogic(ServerContext serverContext, TimeoutListener timeoutListener) {
        super(serverContext);
        LOG_TAG += serverContext.getId();
        this.timeoutListener = timeoutListener;
        this.voteCount = 0;
        serverContext.setLeaderAddress(null);

        // Increment term, start election, vote for myself and set timeout thread.
        startElectionAndSetTimeout();
    }


    private void startElectionAndSetTimeout() {
        serverContext.setCurrentTerm(serverContext.getCurrentTerm() + 1);

        for (int i = 0; i < Config.SERVERS.size(); i++) {
            if (i == serverContext.getId())
                voteCount++;
            else
                Utils.startThread(LOG_TAG + "-vote-request", new SendVoteRequest(i));
        }

        TimeoutManager.getInstance().add(LOG_TAG, timeoutListener::onTimeout, getElectionTimeout());
    }


    @Override
    public void release() {
        // TODO
    }

    @Override
    protected Message handleClient(Message message, ServerContext serverContext) throws Exception {
        switch (message.getMessageType()) {

            case GET_LEADER_ADDR:
                LogUtils.debug(LOG_TAG, "Leader not elected yet. Returning null.");
                return null;

            default:
                LogUtils.error(LOG_TAG, "Unrecognised message type received from a client. Returning null.");
        }
        return null;
    }

    @Override
    protected Message handleServer(Message message, ServerContext serverContext) throws Exception {
        switch (message.getMessageType()) {

            case REQUEST_VOTE_RPC:
                // TODO Must be same or lesser term. Not granting vote.

            case REQUEST_VOTE_RPC_REPLY:
                // TODO If term is same, increment voteCount else do nothing.

            default:
                LogUtils.error(LOG_TAG, "Not an election message. Returning null. ");
        }
        return null;
    }

    private class SendVoteRequest implements Runnable {

        int sendTo;

        SendVoteRequest(int id) {
            this.sendTo = id;
        }

        @Override
        public void run() {
            Socket socket = null;
            ObjectOutputStream objectOutputStream = null;

            try {
                socket = new Socket(Config.SERVERS.get(sendTo).getIp(), Config.SERVERS.get(sendTo).getServerPort());
                Message voteRequest = new Message(Message.Sender.SERVER, Message.Type.REQUEST_VOTE_RPC);
                RequestVoteRpc requestVoteRpc = new RequestVoteRpc();

                requestVoteRpc.setTerm(serverContext.getCurrentTerm());
                requestVoteRpc.setCandidateId(serverContext.getId());
                requestVoteRpc.setLastLogIndex(serverContext.getLog().last().getIndex());
                requestVoteRpc.setLastLogTerm(serverContext.getLog().last().getTerm());

                voteRequest.setMessage(new Gson().toJson(requestVoteRpc));
                objectOutputStream = Utils.getOos(socket);
                objectOutputStream.writeObject(voteRequest);

                // TODO We should not close the socket immediately after writing. We need to wait until the receiver has finished reading.

                LogUtils.debug(LOG_TAG, "Vote request sent to " + Config.SERVERS.get(sendTo));
            } catch (Exception e) {
                LogUtils.error(LOG_TAG, "Could not send vote request to " + Config.SERVERS.get(sendTo));
            } finally {
                Utils.closeQuietly(objectOutputStream);
                Utils.closeQuietly(socket);
            }

        }
    }
}
