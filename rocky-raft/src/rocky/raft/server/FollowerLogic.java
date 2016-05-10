package rocky.raft.server;

import com.google.gson.Gson;
import rocky.raft.dto.Address;
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
    public Message process(Message message, ServerContext serverContext) {
        switch(message.getSender()){
            case CLIENT: return handleClient(message, serverContext.getLeaderAddress(), serverContext.getLog());

            case SERVER: return handleServer(message);

            default:
                LogUtils.error(LOG_TAG, "Unrecognised sender. Returning null.");
        }
        return null;
    }

    private Message handleClient(Message message, Address leader, Log log) {
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
                reply.setMessage(new Gson().toJson(log.lookup()));
                return reply;

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
