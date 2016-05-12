package rocky.raft.server;

import rocky.raft.dto.LogEntry;
import rocky.raft.dto.Message;

import java.io.IOException;

public class LeaderLogic implements ServerLogic {

    private int[] nextIndex;

    private int[] matchIndex;

    private ServerContext serverContext;
    private static String LOG_TAG = "LeaderLogic-";

    public LeaderLogic(int serverCount, ServerContext serverContext) throws IOException {
        this.serverContext = serverContext;
        LOG_TAG += this.serverContext.getId();

        nextIndex = new int[serverCount];
        matchIndex = new int[serverCount];

        LogEntry entry = serverContext.getLog().last();
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
