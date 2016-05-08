package rocky.raft.server;

import rocky.raft.dto.LogEntry;
import rocky.raft.dto.Message;

public class LeaderLogic implements ServerLogic {

    private int[] nextIndex;

    private int[] matchIndex;

    public LeaderLogic(int serverCount, Log log) {
        nextIndex = new int[serverCount];
        matchIndex = new int[serverCount];

        LogEntry entry = log.peek();
        for (int i = 0; i < serverCount; ++i) {
            nextIndex[i] = entry.getIndex() + 1;
            matchIndex[i] = 0;
        }
    }

    @Override
    public Message process(Message message) {
        return null;
    }
}
