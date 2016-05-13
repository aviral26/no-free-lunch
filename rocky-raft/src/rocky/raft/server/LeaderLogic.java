package rocky.raft.server;

import rocky.raft.dto.LogEntry;
import rocky.raft.dto.Message;
import rocky.raft.utils.LogUtils;

import java.io.IOException;

public class LeaderLogic extends BaseLogic {

    private int[] nextIndex;

    private int[] matchIndex;

    private static String LOG_TAG = "LeaderLogic-";

    public LeaderLogic(int serverCount, ServerContext serverContext) throws IOException {
        super(serverContext);
        LOG_TAG += serverContext.getId();

        nextIndex = new int[serverCount];
        matchIndex = new int[serverCount];

        try {
            LogEntry entry = serverContext.getLog().last();

            for (int i = 0; i < serverCount; ++i) {
                nextIndex[i] = entry.getIndex() + 1;
                matchIndex[i] = 0;
            }
        } catch (Exception e) {
            LogUtils.error(LOG_TAG, "Could not read log. This will cause errors.");
        }
    }

    @Override
    public void release() {

    }

    @Override
    protected Message handleClient(Message message, ServerContext serverContext) throws Exception {
        return null;
    }

    @Override
    protected Message handleServer(Message message, ServerContext serverContext) throws Exception {
        return null;
    }
}
