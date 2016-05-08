package rocky.raft.server;

import rocky.raft.dto.LogEntry;
import rocky.raft.dto.Message;

public class LeaderLogic implements ServerLogic {

    private int[] nextIndex;

    private int[] matchIndex;

    private int id;
    private static String LOG_TAG = "LeaderLogic-";

    public LeaderLogic(int serverCount, Log log, int id) {
        this.id = id;
        LOG_TAG += this.id;

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
