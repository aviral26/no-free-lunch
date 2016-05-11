package rocky.raft.server;

import com.google.gson.Gson;
import rocky.raft.common.Constants;
import rocky.raft.common.TimeoutListener;
import rocky.raft.common.TimeoutManager;
import rocky.raft.dto.*;
import rocky.raft.log.Log;
import rocky.raft.utils.LogUtils;

public class FollowerLogic implements ServerLogic {

    private static String LOG_TAG = "FollowerLogic-";
    private ServerContext serverContext;
    private TimeoutListener timeoutListener;

    FollowerLogic(ServerContext serverContext, TimeoutListener timeoutListener){
        LOG_TAG += serverContext.getId();
        this.serverContext = serverContext;
        this.timeoutListener = timeoutListener;
    }

    @Override
    public Message process(Message message) {
        try{
            switch(message.getSender()){
                case CLIENT: return handleClient(message, serverContext.getLeaderAddress(), serverContext.getLog());

                case SERVER: return handleServer(message);

                default:
                    LogUtils.error(LOG_TAG, "Unrecognised sender. Returning null.");
            }
        }
        catch(Exception e){
            LogUtils.error(LOG_TAG, "Something went wrong while processing message. Returning null.", e);
        }
        return null;
    }

    private Message handleClient(Message message, Address leader, Log log) throws Exception{
        Message reply;

        switch (message.getMessageType()){

            case GET_LEADER_ADDR:
                reply = new Message(Message.Sender.SERVER, Message.Type.LEADER_ADDR);
                reply.setStatus(Message.Status.OK);
                reply.setMessage(new Gson().toJson(leader));
                return reply;

            case GET_POSTS:
                reply = new Message(Message.Sender.SERVER, Message.Type.POSTS);
                reply.setStatus(Message.Status.OK);
                reply.setMessage(new Gson().toJson(log.getAll()));
                return reply;

            default: LogUtils.error(LOG_TAG, "Unrecognised message type received from a client. Returning null");
        }
        return null;
    }

    private Message handleServer(Message message) throws Exception {
        Message reply;
        Log log = serverContext.getLog();
        int term = log.last().getTerm();

        switch (message.getMessageType()){

            // Assumes entries are in chronological order.
            case APPEND_ENTRIES_RPC:

                // Heartbeat received. Reset time out thread.
                TimeoutManager.getInstance().add(LOG_TAG, new TimeOut(timeoutListener), Constants.TIMEOUT);

                AppendEntriesRpc appendEntriesRpc = new Gson().fromJson(message.getMessage(), AppendEntriesRpc.class);
                AppendEntriesRpcReply appendEntriesRpcReply = new AppendEntriesRpcReply();
                LogEntry logEntryAtPrevLogIndex = log.get(appendEntriesRpc.getPrevLogIndex());

                if((term > appendEntriesRpc.getTerm()) || (logEntryAtPrevLogIndex.getTerm() != appendEntriesRpc.getTerm
                    ())){
                    appendEntriesRpcReply.setSuccess(false);
                    appendEntriesRpcReply.setTerm(term);
                    LogUtils.debug(LOG_TAG, "Replying false to AppendEntriesRPC.");
                }

                else {
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

                if(requestVoteRpc.getTerm() < term){
                    LogUtils.debug(LOG_TAG, "I have higher term, so not granting vote to candidate " + requestVoteRpc
                            .getCandidateId());
                    requestVoteRpcReply.setTerm(term);
                    requestVoteRpcReply.setVoteGranted(false);
                }
                else{
                    if((serverContext.getVotedFor() == -1 || serverContext.getVotedFor() == requestVoteRpc.getCandidateId()) && log.last().getIndex() <= requestVoteRpc.getLastLogIndex() && log.last().getTerm() <= requestVoteRpc.getLastLogTerm() ){
                        LogUtils.debug(LOG_TAG, "Granting vote to candidate: " + requestVoteRpc.getCandidateId());
                        serverContext.setVotedFor(requestVoteRpc.getCandidateId());
                        TimeoutManager.getInstance().add(LOG_TAG, new TimeOut(timeoutListener), Constants.TIMEOUT);
                        requestVoteRpcReply.setTerm(term);
                        requestVoteRpcReply.setVoteGranted(true);
                    }
                    else{
                        LogUtils.debug(LOG_TAG, "My log is more updated. Not granting vote to candidate: " +
                                requestVoteRpc.getCandidateId());
                        requestVoteRpcReply.setTerm(term);
                        requestVoteRpcReply.setVoteGranted(false);
                    }
                }

                reply = new Message(Message.Sender.SERVER, Message.Type.REQUEST_VOTE_RPC_REPLY);
                reply.setStatus(Message.Status.OK);
                reply.setMessage(new Gson().toJson(requestVoteRpcReply));

                return reply;

            default: LogUtils.error(LOG_TAG, "Unrecognised message type received from server. Returning null. ");
        }
        return null;
    }

    class TimeOut implements Runnable{

        private TimeoutListener timeoutListener;

        TimeOut(TimeoutListener timeoutListener){
            this.timeoutListener = timeoutListener;
        }

        @Override
        public void run() {
            LogUtils.debug(LOG_TAG, "Timed out.");
            timeoutListener.onTimeout();
        }
    }
}

