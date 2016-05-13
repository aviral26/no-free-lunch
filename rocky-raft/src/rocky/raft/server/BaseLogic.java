package rocky.raft.server;

import rocky.raft.dto.Message;
import rocky.raft.utils.LogUtils;

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
                    return handleClient(message, serverContext);
                case SERVER:
                    return handleServer(message, serverContext);
                default:
                    LogUtils.error(LOG_TAG, "Unrecognised sender. Returning null.");
            }
        } catch (Exception e) {
            LogUtils.error(LOG_TAG, "Something went wrong while processing message. Returning null.", e);
        }
        return null;
    }

    protected abstract Message handleClient(Message message, ServerContext serverContext) throws Exception;

    protected abstract Message handleServer(Message message, ServerContext serverContext) throws Exception;
}
