package rocky.raft.server;

import rocky.raft.dto.Message;

public interface ServerLogic {

    void init();

    Message process(Message message);

    void release();
}
