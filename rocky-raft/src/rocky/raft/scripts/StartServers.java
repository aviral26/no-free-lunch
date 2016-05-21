package rocky.raft.scripts;

import rocky.raft.common.Config;
import rocky.raft.dto.ServerConfig;
import rocky.raft.server.RaftServer;
import rocky.raft.server.Server;
import rocky.raft.utils.LogUtils;

public class StartServers {

    private static final String LOG_TAG = "START_SERVERS";

    public static void main(String[] args) {
        try {
            Config config = Config.buildDefault();
            for (ServerConfig serverConfig : config.getServerConfigs()) {
                Server server = new RaftServer(serverConfig.getId());
                server.start();
            }
        } catch (Exception e) {
            LogUtils.error(LOG_TAG, "Something went wrong", e);
        }
    }
}
