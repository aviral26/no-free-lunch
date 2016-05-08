package rocky.raft.server;

import rocky.raft.dto.Message;

public class InactiveLogic implements ServerLogic {

    @Override
    public Message process(Message message) {
        return null;
    }
}
