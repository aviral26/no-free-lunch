package rocky.raft.server;

import com.google.gson.Gson;
import rocky.raft.common.Config;
import rocky.raft.common.ServerState;
import rocky.raft.dto.Address;
import rocky.raft.dto.Message;
import rocky.raft.utils.LogUtils;
import rocky.raft.utils.Utils;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class RaftServer implements Server {

    private String LOG_TAG = "RAFT_SERVER-";

    private int id;

    private Address address;

    private Address leaderAddress;

    private Log log;

    private Store store;

    private int commitIndex;

    private ServerState state;

    private ServerLogic serverLogic;

    public RaftServer(int id) {
        LOG_TAG += id;
        this.id = id;
        this.address = Config.SERVERS.get(id);
        this.log = null; // TODO
        this.store = null; // TODO
        this.commitIndex = 0;
        updateState(ServerState.INACTIVE);
        this.leaderAddress = null;
    }

    @Override
    public void start() {
        LogUtils.debug(LOG_TAG, "Starting server " + address + " in FOLLOWER state.");
        updateState(ServerState.FOLLOWER);
        listenClients();
        listenServers();
    }

    private void updateState(ServerState state) {
        LogUtils.debug(LOG_TAG, "Updating server state to " + state.name());

        this.state = state;
        switch (state) {
            case INACTIVE:
                serverLogic = new InactiveLogic(id);
                break;
            case FOLLOWER:
                serverLogic = new FollowerLogic(id);
                break;
            default:
        }
    }

    private void listenClients() {
        Runnable runnable = () -> {
            try {
                ServerSocket ss = new ServerSocket(address.getClientPort());
                while (true) {
                    handleClient(ss.accept());
                }
            } catch (Exception e) {
                LogUtils.error(LOG_TAG, "Something went wrong while listening to clients", e);
            }
        };

        Utils.startThread("listen-client", runnable);
    }

    private void handleClient(Socket socket) {
        Runnable runnable = () -> {
            ObjectInputStream ois = null;
            ObjectOutputStream oos = null;

            try {
                LogUtils.debug(LOG_TAG, "Handling client");
                ois = Utils.getOis(socket);
                Message message = (Message) ois.readObject();
                LogUtils.debug(LOG_TAG, "Received message from client " + message);

                // A message from a client cannot cause the server to change state, so invoke ServerLogic directly.
                Message reply = serverLogic.process(message);

                oos = Utils.writeAndFlush(socket, reply);
            } catch (Exception e) {
                LogUtils.error(LOG_TAG, "Something went wrong while handling client", e);
            } finally {
                Utils.closeQuietly(ois);
                Utils.closeQuietly(oos);
                Utils.closeQuietly(socket);
            }
        };

        Utils.startThread("handle-client", runnable);
    }

    private void listenServers() {
        Runnable runnable = () -> {
            try {
                ServerSocket ss = new ServerSocket(address.getServerPort());
                while (true) {
                    handleServer(ss.accept());
                }
            } catch (Exception e) {
                LogUtils.error(LOG_TAG, "Something went wrong while listening to servers", e);
            }
        };

        Utils.startThread("listen-server", runnable);
    }

    private void handleServer(Socket socket) {
        Runnable runnable = () -> {
            ObjectInputStream ois = null;
            ObjectOutputStream oos = null;

            try {
                ois = Utils.getOis(socket);
                Message message = (Message) ois.readObject();

                Message reply = processServerMessage(message);

                oos = Utils.writeAndFlush(socket, reply);
            } catch (Exception e) {
                LogUtils.error(LOG_TAG, "Something went wrong while handling server", e);
            } finally {
                Utils.closeQuietly(ois);
                Utils.closeQuietly(oos);
                Utils.closeQuietly(socket);
            }
        };

        Utils.startThread("handle-server", runnable);
    }

    private Message processServerMessage(Message message) throws Exception {
        // TODO
        // if message is going to cause a state change, handle it here;
        // otherwise invoke ServerLogic.
        return null;
    }
}
