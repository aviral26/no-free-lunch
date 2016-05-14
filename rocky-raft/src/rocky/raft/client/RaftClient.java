package rocky.raft.client;

import rocky.raft.dto.*;
import rocky.raft.utils.LogUtils;
import rocky.raft.utils.NetworkUtils;

import java.net.Socket;
import java.util.List;

public class RaftClient implements Client {

    private static final String LOG_TAG = "RAFT_CLIENT";

    private List<Address> servers;

    public RaftClient(List<Address> servers) {
        this.servers = servers;
    }

    private Address findLeader() throws Exception {
        for (Address address : servers) {
            try {
                Socket socket = new Socket(address.getIp(), address.getClientPort());
                NetworkUtils.writeMessage(socket, new Message.Builder().setType(Message.Type.GET_LEADER_ADDR).build());
                Message reply = NetworkUtils.readMessage(socket);
                NetworkUtils.closeQuietly(socket);

                if (reply.getStatus() == Message.Status.OK) {
                    Address leaderAddress = ((GetLeaderAddrReply) reply.getMeta()).getLeaderAddress();
                    LogUtils.debug(LOG_TAG, "Got leader addr " + leaderAddress);
                    return leaderAddress;
                }
            } catch (Exception e) {
                LogUtils.error(LOG_TAG, "Connect failed", e);
            }
        }

        throw new Exception("Failed to find a leader");
    }

    @Override
    public List<String> lookup() throws Exception {
        Address leaderAddress = findLeader();
        return lookup(leaderAddress);
    }

    @Override
    public List<String> lookup(Address address) throws Exception {
        Socket socket = new Socket(address.getIp(), address.getClientPort());
        NetworkUtils.writeMessage(socket, new Message.Builder().setType(Message.Type.GET_POSTS).build());
        Message reply = NetworkUtils.readMessage(socket);
        NetworkUtils.closeQuietly(socket);

        if (reply.getStatus() == Message.Status.OK) {
            return ((GetPostsReply) reply.getMeta()).getPosts();
        }
        LogUtils.debug(LOG_TAG, "Failed to get posts: " + reply);
        return null;
    }

    @Override
    public void post(String message) throws Exception {
        Address leaderAddress = findLeader();
        Socket socket = new Socket(leaderAddress.getIp(), leaderAddress.getClientPort());
        NetworkUtils.writeMessage(socket, new Message.Builder().setType(Message.Type.DO_POST)
                .setMeta(new DoPost(message)).build());
        Message reply = NetworkUtils.readMessage(socket);
        NetworkUtils.closeQuietly(socket);

        if (reply.getStatus() != Message.Status.OK) {
            throw new Exception("Failed to post " + message);
        }
    }
}
