package rocky.raft.server;

import com.google.gson.Gson;
import rocky.raft.common.TimeoutListener;
import rocky.raft.common.TimeoutManager;
import rocky.raft.dto.*;
import rocky.raft.log.Log;
import rocky.raft.utils.LogUtils;

public class FollowerLogic extends BaseLogic {

    private static String LOG_TAG = "FOLLOWER_LOGIC-";
    private TimeoutListener timeoutListener;

    FollowerLogic(ServerContext serverContext, TimeoutListener timeoutListener) {
        super(serverContext);
        LOG_TAG += serverContext.getId();
        this.timeoutListener = timeoutListener;

        // Initialize time out thread.
        TimeoutManager.getInstance().add(LOG_TAG, timeoutListener::onTimeout, getElectionTimeout());
    }

    @Override
    public void release() {
        TimeoutManager.getInstance().remove(LOG_TAG);
    }

    @Override
    protected Message handleClient(Message message, ServerContext serverContext) throws Exception {
        Message reply;

        switch (message.getMessageType()) {
            case GET_LEADER_ADDR:
                reply = new Message(Message.Sender.SERVER, Message.Type.LEADER_ADDR);
                reply.setStatus(Message.Status.OK);
                reply.setMessage(new Gson().toJson(serverContext.getLeaderAddress()));
                return reply;

            default:
                LogUtils.error(LOG_TAG, "Unrecognised message type received from a client. Returning null");
        }
        return null;
    }

    @Override
    protected Message handleServer(Message message, ServerContext serverContext) throws Exception {
        Message reply;
        Log log = serverContext.getLog();
        int term = log.last().getTerm();

        switch (message.getMessageType()) {

            // Assumes entries are in chronological order.
            case APPEND_ENTRIES_RPC:

                // Heartbeat received. Reset time out thread.
                TimeoutManager.getInstance().add(LOG_TAG, timeoutListener::onTimeout, getElectionTimeout());

                AppendEntriesRpc appendEntriesRpc = new Gson().fromJson(message.getMessage(), AppendEntriesRpc.class);
                AppendEntriesRpcReply appendEntriesRpcReply = new AppendEntriesRpcReply();
                LogEntry logEntryAtPrevLogIndex = log.get(appendEntriesRpc.getPrevLogIndex());

                if ((term > appendEntriesRpc.getTerm()) || (logEntryAtPrevLogIndex.getTerm() != appendEntriesRpc.getTerm())) {
                    appendEntriesRpcReply.setSuccess(false);
                    appendEntriesRpcReply.setTerm(term);
                    LogUtils.debug(LOG_TAG, "Replying false to AppendEntriesRPC.");
                } else {
                    for (LogEntry entry : appendEntriesRpc.getEntries()) {
                        if (log.get(entry.getIndex()) == null)
                            log.append(entry);
                        else {
                            LogUtils.debug(LOG_TAG, "Truncating log.");
                            log.resize(entry.getIndex() - 1);
                            log.append(entry);
                        }
                    }

                    if (appendEntriesRpc.getLeaderCommit() > serverContext.getCommitIndex()) {
                        int min = appendEntriesRpc.getLeaderCommit() > log.last().getIndex() ? log.last().getIndex() : appendEntriesRpc.getLeaderCommit();
                        serverContext.setCommitIndex(min);
                    }

                    appendEntriesRpcReply.setSuccess(true);
                    appendEntriesRpcReply.setTerm(term);
                    LogUtils.debug(LOG_TAG, "Replying true to AppendEntriesRPC.");
                }

                reply = new Message(Message.Sender.SERVER, Message.Type.APPEND_ENTRIES_RPC_REPLY);
                reply.setStatus(Message.Status.OK);
                reply.setMessage(new Gson().toJson(appendEntriesRpcReply));

                return reply;

            case REQUEST_VOTE_RPC:
                RequestVoteRpc requestVoteRpc = new Gson().fromJson(message.getMessage(), RequestVoteRpc.class);
                RequestVoteRpcReply requestVoteRpcReply = new RequestVoteRpcReply();

                if (requestVoteRpc.getTerm() < term) {
                    LogUtils.debug(LOG_TAG, "I have higher term, so not granting vote to candidate " + requestVoteRpc.getCandidateId());
                    requestVoteRpcReply.setTerm(term);
                    requestVoteRpcReply.setVoteGranted(false);
                } else {
                    if ((serverContext.getVotedFor() == -1 || serverContext.getVotedFor() == requestVoteRpc.getCandidateId()) && log.last().getIndex() <= requestVoteRpc.getLastLogIndex() && log.last().getTerm() <= requestVoteRpc.getLastLogTerm()) {
                        LogUtils.debug(LOG_TAG, "Granting vote to candidate: " + requestVoteRpc.getCandidateId());
                        serverContext.setVotedFor(requestVoteRpc.getCandidateId());

                        // Reset timeout thread.
                        TimeoutManager.getInstance().add(LOG_TAG, timeoutListener::onTimeout, getElectionTimeout());
                        requestVoteRpcReply.setTerm(term);
                        requestVoteRpcReply.setVoteGranted(true);
                    } else {
                        LogUtils.debug(LOG_TAG, "My log is more updated. Not granting vote to candidate " +
                                requestVoteRpc.getCandidateId());
                        requestVoteRpcReply.setTerm(term);
                        requestVoteRpcReply.setVoteGranted(false);
                    }
                }

                reply = new Message(Message.Sender.SERVER, Message.Type.REQUEST_VOTE_RPC_REPLY);
                reply.setStatus(Message.Status.OK);
                reply.setMessage(new Gson().toJson(requestVoteRpcReply));

                return reply;

            default:
                LogUtils.error(LOG_TAG, "Unrecognised message type received from server. Returning null. ");
        }
        return null;
    }
}

