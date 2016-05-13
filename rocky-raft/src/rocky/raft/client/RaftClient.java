package rocky.raft.client;

import com.google.gson.Gson;
import rocky.raft.dto.Address;
import rocky.raft.dto.Message;
import rocky.raft.utils.LogUtils;
import rocky.raft.utils.Utils;

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
                Utils.getOos(socket).writeObject(new Message(Message.Sender.CLIENT, Message.Type.GET_LEADER_ADDR));

                Message msg = (Message) Utils.getOis(socket).readObject();
                Utils.closeQuietly(socket);

                if (msg.getStatus() == Message.Status.OK) {
                    Address leaderAddress = new Gson().fromJson(msg.getMessage(), Address.class);
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
        Utils.getOos(socket).writeObject(new Message(Message.Sender.CLIENT, Message.Type.GET_POSTS));

        Message message = (Message) Utils.getOis(socket).readObject();
        Utils.closeQuietly(socket);

        if (message.getStatus() == Message.Status.OK) {
            return new Gson().fromJson(message.getMessage(), List.class);
        }
        LogUtils.debug(LOG_TAG, "Failed to get posts: " + message);
        return null;
    }

    @Override
    public void post(String message) throws Exception {
        Address leaderAddress = findLeader();
        Socket socket = new Socket(leaderAddress.getIp(), leaderAddress.getClientPort());

        Message msg = new Message(Message.Sender.CLIENT, Message.Type.DO_POST);
        msg.setMessage(message);

        Utils.getOos(socket).writeObject(msg);

        Message response = (Message) Utils.getOis(socket).readObject();
        Utils.closeQuietly(socket);

        if (response.getStatus() != Message.Status.OK) {
            throw new Exception("Failed to post " + message);
        }
    }
}
