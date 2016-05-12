package rocky.raft.server;

import rocky.raft.common.TimeoutListener;
import rocky.raft.dto.Message;
import rocky.raft.utils.LogUtils;

public class CandidateLogic implements ServerLogic {

    private String LOG_TAG = "CandidateLogic-";
    private ServerContext serverContext;
    private TimeoutListener timeoutListener;

    CandidateLogic(ServerContext serverContext, TimeoutListener timeoutListener){
        this.serverContext = serverContext;
        LOG_TAG += this.serverContext;
        this.timeoutListener = timeoutListener;
        // TODO start election and vote for myself.
    }

    @Override
    public void release() {
        // TODO
    }

    @Override
    public Message process(Message message) {
        switch (message.getSender()){
            case CLIENT: return handleClient(message);

            case SERVER: return handleServer(message);

            default:
                LogUtils.error(LOG_TAG, "Unrecognised sender. Returning null. ");
        }
        return null;
    }

    private Message handleClient(Message message) {
        switch (message.getMessageType()){

            case GET_LEADER_ADDR:
                LogUtils.debug(LOG_TAG, "Leader not elected yet. Returning null.");
                return null;

            case GET_POSTS:
                LogUtils.debug(LOG_TAG, "May not have up-to-date data. Returning null. ");
                return null;

            default: LogUtils.error(LOG_TAG, "Unrecognised message type received from a client. Returning null.");
        }
        return null;
    }

    private Message handleServer(Message message) {
        switch (message.getMessageType()){

            case APPEND_ENTRIES_RPC:
                // TODO

            case REQUEST_VOTE_RPC:
                // TODO

            case REQUEST_VOTE_RPC_REPLY:
                // TODO

            default: LogUtils.error(LOG_TAG, "Unrecognised message type received from server. Returning null. ");
        }
        return null;
    }

    class TimeOut implements Runnable{

        private TimeoutListener timeoutListener;
        private ServerContext serverContext;

        TimeOut(ServerContext serverContext, TimeoutListener timeoutListener){
            this.timeoutListener = timeoutListener;
            this.serverContext = serverContext;
        }

        @Override
        public void run() {
            LogUtils.debug(LOG_TAG, "Timed out.");
            serverContext.setLeaderAddress(null);
            timeoutListener.onTimeout();
        }
    }
}
