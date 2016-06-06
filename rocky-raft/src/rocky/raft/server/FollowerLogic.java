package rocky.raft.server;

import rocky.raft.common.TimeoutListener;
import rocky.raft.common.TimeoutManager;
import rocky.raft.dto.*;
import rocky.raft.log.Log;
import rocky.raft.utils.LogUtils;

public class FollowerLogic extends BaseLogic {

    private String LOG_TAG = "FOLLOWER_LOGIC-";
    private TimeoutListener timeoutListener;

    FollowerLogic(ServerContext serverContext, TimeoutListener timeoutListener) {
        super(serverContext);
        LOG_TAG += serverContext.getId();
        this.timeoutListener = timeoutListener;
    }

    @Override
    public void init() {
        // Initialize time out thread.
        TimeoutManager.getInstance().add(LOG_TAG, timeoutListener::onTimeout, getElectionTimeout());
    }

    @Override
    public void release() {
        TimeoutManager.getInstance().remove(LOG_TAG);
    }

    @Override
    protected Message handleMessage(Message message, ServerContext serverContext) throws Exception {
        Log log = serverContext.getLog();
        int currentTerm = serverContext.getCurrentTerm();

        switch (message.getType()) {

            // Assumes entries are in chronological order.
            case APPEND_ENTRIES_RPC:

                // Heartbeat received. Reset time out thread.
                TimeoutManager.getInstance().add(LOG_TAG, timeoutListener::onTimeout, getElectionTimeout());

                AppendEntriesRpc appendEntriesRpc = (AppendEntriesRpc) message.getMeta();

                // Update leaderAddress
                serverContext.setLeaderConfig(serverContext.getConfig().getServerConfig(appendEntriesRpc.getLeaderId()));

                AppendEntriesRpcReply appendEntriesRpcReply;
                LogEntry logEntryAtPrevLogIndex = log.get(appendEntriesRpc.getPrevLogIndex());
                int appendEntriesPrevLogTerm = logEntryAtPrevLogIndex == null ? 0 : logEntryAtPrevLogIndex.getTerm();

                if ((currentTerm > appendEntriesRpc.getTerm()) || (appendEntriesPrevLogTerm != appendEntriesRpc.getPrevLogTerm())) {
                    appendEntriesRpcReply = new AppendEntriesRpcReply(currentTerm, false);
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
                        int min = Math.min(appendEntriesRpc.getLeaderCommit(), serverContext.getLastIndex());
                        serverContext.setCommitIndex(min);
                    }

                    appendEntriesRpcReply = new AppendEntriesRpcReply(currentTerm, true);
                    LogUtils.debug(LOG_TAG, "Replying true to AppendEntriesRPC.");
                }

                return new Message.Builder().setType(Message.Type.APPEND_ENTRIES_RPC_REPLY)
                        .setStatus(Message.Status.OK)
                        .setMeta(appendEntriesRpcReply).build();

            case REQUEST_VOTE_RPC:
                RequestVoteRpc requestVoteRpc = (RequestVoteRpc) message.getMeta();
                RequestVoteRpcReply requestVoteRpcReply;

                if (requestVoteRpc.getTerm() < currentTerm) {
                    LogUtils.debug(LOG_TAG, "I have higher term, so not granting vote to candidate " + requestVoteRpc.getCandidateId());
                    requestVoteRpcReply = new RequestVoteRpcReply(currentTerm, false);
                } else {
                    int votedFor = serverContext.getVotedFor();
                    boolean hasVotedFor = votedFor == -1 || votedFor == requestVoteRpc.getCandidateId();
                    boolean latestLog = serverContext.getLastIndex() <= requestVoteRpc.getLastLogIndex() && serverContext.getLastTerm() <= requestVoteRpc.getLastLogTerm();
                    if (hasVotedFor && latestLog) {
                        LogUtils.debug(LOG_TAG, "Granting vote to candidate: " + requestVoteRpc.getCandidateId());
                        serverContext.setVotedFor(requestVoteRpc.getCandidateId());

                        // Reset timeout thread.
                        TimeoutManager.getInstance().add(LOG_TAG, timeoutListener::onTimeout, getElectionTimeout());
                        requestVoteRpcReply = new RequestVoteRpcReply(currentTerm, true);
                    } else {
                        LogUtils.debug(LOG_TAG, "My log is more updated. Not granting vote to candidate " + requestVoteRpc.getCandidateId());
                        requestVoteRpcReply = new RequestVoteRpcReply(currentTerm, false);
                    }
                }

                return new Message.Builder().setType(Message.Type.REQUEST_VOTE_RPC_REPLY)
                        .setStatus(Message.Status.OK)
                        .setMeta(requestVoteRpcReply).build();

            default:
                LogUtils.error(LOG_TAG, "Unrecognised message type received from server. Returning null. ");
        }
        return null;
    }
}

