package rocky.raft.server;

import rocky.raft.common.ServerState;
import rocky.raft.dto.BaseRpc;
import rocky.raft.dto.Message;
import rocky.raft.utils.LogUtils;
import rocky.raft.utils.Utils;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class RaftServer implements Server {

    private String LOG_TAG = "RAFT_SERVER-";

    private ServerContext serverContext;

    private ServerState state;

    private ServerLogic serverLogic;

    public RaftServer(int id) throws IOException {
        LOG_TAG += id;
        serverContext = new ServerContext(id);
        updateState(ServerState.INACTIVE);
    }

    @Override
    public void start() {
        LogUtils.debug(LOG_TAG, "Starting server " + serverContext.getAddress() + " in FOLLOWER state.");
        updateState(ServerState.FOLLOWER);
        listenClients();
        listenServers();
    }

    private synchronized void updateState(ServerState state) {
        if (serverLogic != null) serverLogic.release();

        LogUtils.debug(LOG_TAG, "Updating server state to " + state.name());

        this.state = state;
        switch (state) {
            case INACTIVE:
                serverLogic = new InactiveLogic(serverContext);
                break;
            case FOLLOWER:
                serverLogic = new FollowerLogic(serverContext, () -> updateState(ServerState.CANDIDATE));
                break;
            case CANDIDATE:
                serverLogic = new CandidateLogic(serverContext, () -> updateState(ServerState.CANDIDATE));
                break;
            case LEADER:
                try {
                    serverLogic = new LeaderLogic(serverContext, () -> updateState(ServerState.FOLLOWER));
                } catch (IOException e) {
                    LogUtils.error(LOG_TAG, "Failed to update to leader state.", e);
                }
                break;
            default:
                LogUtils.debug(LOG_TAG, "Failed to update to unknown state.");
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
            Message reply = null;

            try {
                ois = Utils.getOis(socket);
                Message message = (Message) ois.readObject();
                LogUtils.debug(LOG_TAG, "Received message from client " + message);

                // A message from a client cannot cause the server to change state, so invoke ServerLogic directly.
                reply = serverLogic.process(message);
                if (reply == null) {
                    throw new NullPointerException("Got null reply from serverLogic");
                }
            } catch (Exception e) {
                LogUtils.error(LOG_TAG, "Something went wrong while handling client. Notifying client...", e);
                reply = new Message.Builder().setStatus(Message.Status.ERROR).build();
            }

            try {
                oos = Utils.getOos(socket);
                oos.writeObject(reply);
            } catch (Exception e) {
                LogUtils.error(LOG_TAG, "Something went really really wrong while handling client. Client not notified of failure.", e);
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
                ServerSocket ss = new ServerSocket(serverContext.getAddress().getServerPort());
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
            Message reply = null;

            try {
                ois = Utils.getOis(socket);
                Message message = (Message) ois.readObject();
                reply = processServerMessage(message);
                if (reply == null) {
                    throw new NullPointerException("Got null reply from serverLogic");
                }
            } catch (Exception e) {
                LogUtils.error(LOG_TAG, "Something went wrong while handling server. Notifying server...", e);
                reply = new Message.Builder().setStatus(Message.Status.ERROR).build();
            }

            try {
                oos = Utils.getOos(socket);
                oos.writeObject(reply);
            } catch (Exception e) {
                LogUtils.error(LOG_TAG, "Something went really really wrong while handling server. Server not notified of failure.", e);
            } finally {
                Utils.closeQuietly(ois);
                Utils.closeQuietly(oos);
                Utils.closeQuietly(socket);
            }
        };

        Utils.startThread("handle-server", runnable);
    }

    private Message processServerMessage(Message message) throws Exception {
        BaseRpc baseRpc = (BaseRpc) message.getMeta();
        boolean updateToFollower = false;

        switch (message.getType()) {
            case APPEND_ENTRIES_RPC:
            case APPEND_ENTRIES_RPC_REPLY:
                updateToFollower = baseRpc.getTerm() >= serverContext.getCurrentTerm();
                break;
            case REQUEST_VOTE_RPC:
            case REQUEST_VOTE_RPC_REPLY:
                updateToFollower = baseRpc.getTerm() > serverContext.getCurrentTerm();
                break;
        }

        if (updateToFollower) {
            serverContext.setCurrentTerm(baseRpc.getTerm());
            updateState(ServerState.FOLLOWER);
        }
        return serverLogic.process(message);
    }
}