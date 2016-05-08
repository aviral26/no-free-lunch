package rocky.raft.server;

import rocky.raft.dto.Message;
import rocky.raft.utils.LogUtils;

public class FollowerLogic implements ServerLogic {

    private int id;
    private static String LOG_TAG = "FollowerLogic-";

    FollowerLogic(int id){
        this.id = id;
        LOG_TAG += this.id;
    }

    @Override
    public Message process(Message message) {
        switch(message.getSender()){
            case CLIENT: return handleClient(message);

            case SERVER: return handleServer(message);

            default:
                LogUtils.error(LOG_TAG, "Unrecognised sender. Returning null.");
        }
        return null;
    }

    private Message handleClient(Message message) {
        switch (message.getMessageType()){
            case GET_LEADER_ADDR:
                // TODO
            case GET_POSTS:
                // TODO
            default: LogUtils.error(LOG_TAG, "Unrecognised message type received from a client. Returning null");
        }
        return null;
    }

    private Message handleServer(Message message) {
        switch (message.getMessageType()){
            case APPEND_ENTRIES_RPC:
                // TODO
            case REQUEST_VOTE_RPC:
                // TODO
            default: LogUtils.error(LOG_TAG, "Unrecognised message type received from server. Returning null. ");
        }
        return null;
    }
}
