package rocky.raft.scripts;

import rocky.raft.server.RaftServer;
import rocky.raft.server.Server;
import rocky.raft.utils.LogUtils;

public class StartServer {

    private static final String LOG_TAG = "START_SERVER";

    public static void main(String[] args) {

        try {
            Server server = new RaftServer(Integer.parseInt(args[0]));
            server.start();
        } catch (Exception e) {
            LogUtils.error(LOG_TAG, "Something went wrong", e);
        }
    }
}
