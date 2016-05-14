package rocky.raft.server;

import rocky.raft.dto.Message;
import rocky.raft.utils.LogUtils;

public class InactiveLogic extends BaseLogic {

    private static String LOG_TAG = "InactiveLogic-";

    InactiveLogic(ServerContext serverContext) {
        super(serverContext);
        LOG_TAG += serverContext.getId();
    }

    @Override
    protected Message handleMessage(Message message, ServerContext serverContext) throws Exception {
        LogUtils.debug(LOG_TAG, "Do nothing");
        return null;
    }

    @Override
    public void release() {
        // Do nothing
    }
}
