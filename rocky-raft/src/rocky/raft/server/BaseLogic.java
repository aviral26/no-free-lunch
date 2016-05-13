package rocky.raft.server;

import com.google.gson.Gson;
import rocky.raft.common.Constants;
import rocky.raft.dto.Message;
import rocky.raft.utils.LogUtils;
import rocky.raft.utils.Utils;

public abstract class BaseLogic implements ServerLogic {

    private String LOG_TAG = "BASE_LOGIC-";

    protected ServerContext serverContext;

    public BaseLogic(ServerContext serverContext) {
        LOG_TAG += serverContext.getId();
        this.serverContext = serverContext;
    }

    @Override
    public Message process(Message message) {
        try {
            switch (message.getSender()) {
                case CLIENT:
                    return processClient(message, serverContext);
                case SERVER:
                    return processServer(message, serverContext);
                default:
                    LogUtils.error(LOG_TAG, "Unrecognised sender. Returning null.");
            }
        } catch (Exception e) {
            LogUtils.error(LOG_TAG, "Something went wrong while processing message. Returning null.", e);
        }
        return null;
    }

    private Message processClient(Message message, ServerContext serverContext) throws Exception {
        Message reply = null;
        switch (message.getMessageType()) {
            case GET_POSTS:
                reply = new Message(Message.Sender.SERVER, Message.Type.POSTS);
                reply.setStatus(Message.Status.OK);
                reply.setMessage(new Gson().toJson(serverContext.getLog().getAll()));
                break;
        }

        if (reply == null) {
            reply = handleClient(message, serverContext);
        }
        return reply;
    }

    private Message processServer(Message message, ServerContext serverContext) throws Exception {
        Message reply = null;

        // Any common server message handling goes here

        if (reply == null) {
            reply = handleServer(message, serverContext);
        }
        return reply;
    }

    protected abstract Message handleClient(Message message, ServerContext serverContext) throws Exception;

    protected abstract Message handleServer(Message message, ServerContext serverContext) throws Exception;

    protected long getElectionTimeout() {
        return Utils.getRandomLong(Constants.TIMEOUT_MIN, Constants.TIMEOUT_MAX + 1);
    }
}
