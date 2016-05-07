package rocky.raft.client;

import rocky.raft.dto.Address;
import rocky.raft.utils.LogUtils;

import java.net.Socket;
import java.util.List;

public class RaftClient implements Client {

    private static final String LOG_TAG = "RAFT_CLIENT";

    private List<Address> servers;

    private Socket leaderSocket;

    public RaftClient(List<Address> servers) {
        this.servers = servers;
    }

    @Override
    public void connect() {
        for (Address address : servers) {
            try {
                Socket socket = new Socket(address.getIp(), address.getClientPort());
                // TODO
            } catch (Exception e) {
                LogUtils.error(LOG_TAG, "Connect failed", e);
            }
        }
    }

    @Override
    public void disconnect() {

    }

    @Override
    public List<String> lookup() {
        return null;
    }

    @Override
    public void post(String message) {

    }
}
