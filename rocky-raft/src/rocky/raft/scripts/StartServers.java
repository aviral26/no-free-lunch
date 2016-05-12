package rocky.raft.scripts;

import rocky.raft.common.Config;
import rocky.raft.server.RaftServer;
import rocky.raft.server.Server;

public class StartServers {

    public static void main(String[] args) throws Exception {

        for (int i = 0; i < Config.SERVERS.size(); ++i) {
            Server server = new RaftServer(i);
            server.start();
        }
    }
}
