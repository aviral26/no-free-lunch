package rocky.raft.scripts;

import rocky.raft.common.Config;
import rocky.raft.server.RaftServer;
import rocky.raft.server.Server;
import rocky.raft.utils.LogUtils;

public class StartServers {

    private static final String LOG_TAG = "START_SERVERS";

    public static void main(String[] args) {

        try {
            for (int i = 0; i < Config.SERVERS.size(); ++i) {
                Server server = new RaftServer(i);
                server.start();
            }
        } catch (Exception e) {
            LogUtils.error(LOG_TAG, "Something went wrong", e);
        }
    }
}
