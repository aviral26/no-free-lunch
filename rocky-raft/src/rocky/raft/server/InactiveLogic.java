package rocky.raft.server;

import rocky.raft.dto.Address;
import rocky.raft.dto.Message;
import rocky.raft.utils.LogUtils;

public class InactiveLogic implements ServerLogic {

    private int id;
    private static String LOG_TAG = "InactiveLogic-";

    InactiveLogic(int id){
        this.id = id;
        LOG_TAG += this.id;
    }

    @Override
    public Message process(Message message, ServerContext serverContext) {
        LogUtils.debug(LOG_TAG, "Cannot process message in inactive state. Returning null.");
        return null;
    }
}
