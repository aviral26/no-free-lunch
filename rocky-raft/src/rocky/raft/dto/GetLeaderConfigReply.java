package rocky.raft.dto;

public class GetLeaderConfigReply extends Message.Meta {

    private ServerConfig leaderConfig;

    public GetLeaderConfigReply(ServerConfig leaderConfig) {
        this.leaderConfig = leaderConfig;
    }

    public ServerConfig getLeaderConfig() {
        return leaderConfig;
    }

    @Override
    public String toString() {
        return "GetLeaderConfigReply{" +
                "leaderConfig=" + leaderConfig +
                '}';
    }
}
