package rocky.raft.server;

import rocky.raft.dto.Message;
import rocky.raft.utils.LogUtils;

public class CandidateLogic implements ServerLogic {

    private String LOG_TAG = "CandidateLogic-";
    private ServerContext serverContext;

    CandidateLogic(ServerContext serverContext){
        this.serverContext = serverContext;
        LOG_TAG += this.serverContext;
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
                // TODO
            case GET_POSTS:
                // TODO
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

}
