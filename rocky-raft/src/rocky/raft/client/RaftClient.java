package rocky.raft.client;

import rocky.raft.common.Config;
import rocky.raft.dto.*;
import rocky.raft.utils.LogUtils;
import rocky.raft.utils.NetworkUtils;

import java.net.Socket;
import java.util.List;

public class RaftClient implements Client {

    private static final String LOG_TAG = "RAFT_CLIENT";

    private Config config;

    public RaftClient(Config config) {
        this.config = config;
    }

    private ServerConfig findLeader() throws Exception {
        for (ServerConfig serverConfig : config.getServerConfigs()) {
            try {
                Address address = serverConfig.getAddress();
                Socket socket = new Socket(address.getIp(), address.getClientPort());
                NetworkUtils.writeMessage(socket, new Message.Builder().setType(Message.Type.GET_LEADER_CONFIG).build());
                Message reply = NetworkUtils.readMessage(socket);
                NetworkUtils.closeQuietly(socket);

                if (reply.getStatus() == Message.Status.OK) {
                    ServerConfig leaderConfig = ((GetLeaderConfigReply) reply.getMeta()).getLeaderConfig();
                    if (leaderConfig != null) {
                        LogUtils.debug(LOG_TAG, "Got leader config " + leaderConfig);
                        return leaderConfig;
                    }
                }
            } catch (Exception e) {
                LogUtils.error(LOG_TAG, "Connection failed: " + serverConfig);
            }
        }

        throw new Exception("Failed to find a leader");
    }

    @Override
    public List<String> lookup() throws Exception {
        ServerConfig leaderConfig = findLeader();
        return lookup(leaderConfig);
    }

    @Override
    public List<String> lookup(ServerConfig serverConfig) throws Exception {
        Address address = serverConfig.getAddress();
        Socket socket = new Socket(address.getIp(), address.getClientPort());
        NetworkUtils.writeMessage(socket, new Message.Builder().setType(Message.Type.GET_POSTS).build());
        Message reply = NetworkUtils.readMessage(socket);
        NetworkUtils.closeQuietly(socket);

        if (reply.getStatus() != Message.Status.OK) {
            throw new Exception("Failed to lookup");
        }
        return ((GetPostsReply) reply.getMeta()).getPosts();
    }

    @Override
    public void post(String message) throws Exception {
        ServerConfig leaderConfig = findLeader();
        Address leaderAddress = leaderConfig.getAddress();
        Socket socket = new Socket(leaderAddress.getIp(), leaderAddress.getClientPort());
        NetworkUtils.writeMessage(socket, new Message.Builder().setType(Message.Type.DO_POST)
                .setMeta(new DoPost(message)).build());
        Message reply = NetworkUtils.readMessage(socket);
        NetworkUtils.closeQuietly(socket);

        if (reply.getStatus() != Message.Status.OK) {
            throw new Exception("Failed to post " + message);
        }
    }

    @Override
    public void configChange(Config newConfig) throws Exception {
        ServerConfig leaderConfig = findLeader();
        Address leaderAddress = leaderConfig.getAddress();
        Socket socket = new Socket(leaderAddress.getIp(), leaderAddress.getClientPort());
        NetworkUtils.writeMessage(socket, new Message.Builder().setType(Message.Type.CHANGE_CONFIG)
                .setMeta(new ChangeConfig(newConfig)).build());
        Message reply = NetworkUtils.readMessage(socket);
        NetworkUtils.closeQuietly(socket);

        if (reply.getStatus() != Message.Status.OK) {
            throw new Exception("Failed to change config.");
        }

        this.config = newConfig;
    }
}
