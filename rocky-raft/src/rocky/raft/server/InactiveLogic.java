package rocky.raft.server;

import rocky.raft.dto.Message;
import rocky.raft.utils.LogUtils;

public class InactiveLogic implements ServerLogic {

    private static String LOG_TAG = "InactiveLogic-";
    private ServerContext serverContext;

    InactiveLogic(ServerContext serverContext) {
        this.serverContext = serverContext;
        LOG_TAG += this.serverContext.getId();
    }

    @Override
    public Message process(Message message) {
        LogUtils.debug(LOG_TAG, "Cannot process message in inactive state. Returning null.");
        return null;
    }
}
