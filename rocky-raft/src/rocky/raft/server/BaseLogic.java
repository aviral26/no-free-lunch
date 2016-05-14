package rocky.raft.server;

import rocky.raft.common.Constants;
import rocky.raft.dto.GetPostsReply;
import rocky.raft.dto.LogEntry;
import rocky.raft.dto.Message;
import rocky.raft.utils.LogUtils;
import rocky.raft.utils.Utils;

import java.util.ArrayList;
import java.util.List;

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
            return doProcess(message, serverContext);
        } catch (Exception e) {
            LogUtils.error(LOG_TAG, "Something went wrong while processing message. Returning null.", e);
        }
        return null;
    }

    private Message doProcess(Message message, ServerContext serverContext) throws Exception {
        Message reply = null;
        switch (message.getType()) {
            case GET_POSTS:
                reply = new Message.Builder().setType(Message.Type.GET_POSTS_REPLY)
                        .setStatus(Message.Status.OK)
                        .setMeta(new GetPostsReply(parsePosts(serverContext.getLog().getAll()))).build();
                break;
        }

        if (reply == null) {
            reply = handleMessage(message, serverContext);
        }
        return reply;
    }

    private List<String> parsePosts(List<LogEntry> entries) {
        // TODO Remove non-post entries
        List<String> posts = new ArrayList<>();
        for (LogEntry logEntry : entries) {
            posts.add(logEntry.getValue());
        }
        return posts;
    }

    protected abstract Message handleMessage(Message message, ServerContext serverContext) throws Exception;

    protected long getElectionTimeout() {
        return Utils.getRandomLong(Constants.TIMEOUT_MIN, Constants.TIMEOUT_MAX + 1);
    }
}
