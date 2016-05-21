package rocky.raft.server;

import rocky.raft.common.Constants;
import rocky.raft.dto.GetLeaderConfigReply;
import rocky.raft.dto.GetPostsReply;
import rocky.raft.dto.LogEntry;
import rocky.raft.dto.Message;
import rocky.raft.log.Log;
import rocky.raft.utils.LogUtils;
import rocky.raft.utils.Utils;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

abstract class BaseLogic implements ServerLogic {

    private String LOG_TAG = "BASE_LOGIC-";

    ServerContext serverContext;

    BaseLogic(ServerContext serverContext) {
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
            case GET_LEADER_CONFIG:
                reply = new Message.Builder().setType(Message.Type.GET_LEADER_CONFIG_REPLY)
                        .setStatus(Message.Status.OK)
                        .setMeta(new GetLeaderConfigReply(serverContext.getLeaderConfig())).build();
                break;
            case GET_POSTS:
                if (!(this instanceof LeaderLogic)) {
                    reply = getPostsReply(serverContext.getLog());
                }
                break;
        }

        if (reply == null) {
            reply = handleMessage(message, serverContext);
        }

        return reply;
    }

    Message getPostsReply(Log log) throws IOException {
        return new Message.Builder().setType(Message.Type.GET_POSTS_REPLY)
                .setStatus(Message.Status.OK)
                .setMeta(new GetPostsReply(parsePosts(log.getAll(1)))).build();
    }

    private List<String> parsePosts(List<LogEntry> entries) {
        return entries.stream().filter(logEntry -> !logEntry.isConfigEntry())
                .map(LogEntry::getValue)
                .collect(Collectors.toList());
    }

    protected abstract Message handleMessage(Message message, ServerContext serverContext) throws Exception;

    long getElectionTimeout() {
        return Utils.getRandomLong(Constants.TIMEOUT_MIN, Constants.TIMEOUT_MAX + 1);
    }
}
