package rocky.raft.server;

import rocky.raft.common.Config;
import rocky.raft.common.ServerState;
import rocky.raft.dto.Message;
import rocky.raft.utils.LogUtils;
import rocky.raft.utils.Utils;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class RaftServer implements Server {

    private String LOG_TAG = "RAFT_SERVER-";

    private ServerContext serverContext;

    private ServerState state;

    private ServerLogic serverLogic;

    public RaftServer(int id) {
        LOG_TAG += id;
        serverContext = new ServerContext();
        serverContext.setId(id);
        serverContext.setAddress(Config.SERVERS.get(id));
        serverContext.setLog(null); // TODO
        serverContext.setStore(null); // TODO
        serverContext.setCommitIndex(0);
        serverContext.setVotedFor(-1);
        updateState(ServerState.INACTIVE);
        serverContext.setLeaderAddress(null); // Will be set after election.
    }

    @Override
    public void start() {
        LogUtils.debug(LOG_TAG, "Starting server " + serverContext.getAddress() + " in FOLLOWER state.");
        updateState(ServerState.FOLLOWER);
        listenClients();
        listenServers();
    }

    private void updateState(ServerState state) {
        LogUtils.debug(LOG_TAG, "Updating server state to " + state.name());

        this.state = state;
        switch (state) {
            case INACTIVE:
                serverLogic = new InactiveLogic(serverContext);
                break;
            case FOLLOWER:
                serverLogic = new FollowerLogic(serverContext, () -> updateState(ServerState.CANDIDATE));
                break;
            default:
                LogUtils.debug(LOG_TAG, "Unknown state.");
        }
    }

    private void listenClients() {
        Runnable runnable = () -> {
            try {
                ServerSocket ss = new ServerSocket(serverContext.getAddress().getClientPort());
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
                LogUtils.error(LOG_TAG, "Something went wrong while handling client. Client not notified of failure.",
                        e);
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
                ServerSocket ss = new ServerSocket(serverContext.getLeaderAddress().getServerPort());
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
