package rocky.raft.server;

import rocky.raft.dto.Message;

public interface ServerLogic {

    Message process(Message message, ServerContext serverContext);
}
