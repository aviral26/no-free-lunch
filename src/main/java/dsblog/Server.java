package dsblog;

import common.Address;
import utils.CommonUtils;
import utils.LogUtils;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {

    private static String LOG_TAG = "SERVER";

    private Address address;

    private int id;

    public Server(int id) {
        this.id = id;
        this.address = Config.getServerAddresses().get(id);

        LOG_TAG += "-" + id;

        acceptServers();
        acceptClients();
    }

    public void acceptServers() {
        Runnable runnable = () -> {
            try {
                ServerSocket serverSocket = new ServerSocket(address.getServerPort());
                while (true) {
                    Socket socket = serverSocket.accept();
                    CommonUtils.startThreadWithName(() -> {
                        handleSocket(socket);
                    }, "handle-server-thread");
                }
            } catch (Exception e) {
                LogUtils.error(LOG_TAG, "Could not create server socket", e);
            }
        };

        CommonUtils.startThreadWithName(runnable, "accept-server-thread");
    }

    public void acceptClients() {
        Runnable runnable = () -> {
            try {
                ServerSocket serverSocket = new ServerSocket(address.getClientPort());
                while (true) {
                    Socket socket = serverSocket.accept();
                    CommonUtils.startThreadWithName(() -> {
                        handleSocket(socket);
                    }, "handle-client-thread");
                }
            } catch (Exception e) {
                LogUtils.error(LOG_TAG, "Could not create client socket", e);
            }
        };

        CommonUtils.startThreadWithName(runnable, "accept-client-thread");
    }

    private void handleSocket(Socket socket) {
        Runnable runnable = () -> {
            ObjectInputStream objectInputStream = null;
            ObjectOutputStream objectOutputStream = null;

            try {
                objectInputStream = new ObjectInputStream(socket.getInputStream());
                objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
                handleMessage((Message) objectInputStream.readObject(), objectOutputStream);
            } catch (Exception e) {
                LogUtils.error(LOG_TAG, "Something went wrong in handling server message", e);
            } finally {
                CommonUtils.closeQuietly(objectInputStream);
                CommonUtils.closeQuietly(objectOutputStream);
                CommonUtils.closeQuietly(socket);
            }
        };

        CommonUtils.startThreadWithName(runnable, "handle-server-thread");
    }

    private void handleMessage(Message message, ObjectOutputStream objectOutputStream) throws Exception {
        switch (message.getType()) {
            case POST:
                break;
            case LOOKUP:
                break;
            case SYNC:
                break;
        }
    }
}
