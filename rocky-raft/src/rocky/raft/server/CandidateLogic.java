package rocky.raft.server;

import rocky.raft.common.TimeoutListener;
import rocky.raft.dto.Message;
import rocky.raft.utils.LogUtils;

public class CandidateLogic extends BaseLogic {

    private String LOG_TAG = "CandidateLogic-";
    private TimeoutListener timeoutListener;

    CandidateLogic(ServerContext serverContext, TimeoutListener timeoutListener) {
        super(serverContext);
        LOG_TAG += serverContext.getId();
        this.timeoutListener = timeoutListener;
        serverContext.setLeaderAddress(null);
        // TODO start election and vote for myself.
    }

    @Override
    public void release() {
        // TODO
    }

    @Override
    protected Message handleClient(Message message, ServerContext serverContext) throws Exception {
        switch (message.getMessageType()) {

            case GET_LEADER_ADDR:
                LogUtils.debug(LOG_TAG, "Leader not elected yet. Returning null.");
                return null;

            case GET_POSTS:
                LogUtils.debug(LOG_TAG, "May not have up-to-date data. Returning null. ");
                return null;

            default:
                LogUtils.error(LOG_TAG, "Unrecognised message type received from a client. Returning null.");
        }
        return null;
    }

    @Override
    protected Message handleServer(Message message, ServerContext serverContext) throws Exception {
        switch (message.getMessageType()) {

            case APPEND_ENTRIES_RPC:
                // TODO

            case REQUEST_VOTE_RPC:
                // TODO

            case REQUEST_VOTE_RPC_REPLY:
                // TODO

            default:
                LogUtils.error(LOG_TAG, "Unrecognised message type received from server. Returning null. ");
        }
        return null;
    }
}
