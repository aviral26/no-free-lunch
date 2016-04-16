package dsblog;

import common.Address;
import utils.CommonUtils;
import utils.LogUtils;

import java.net.ServerSocket;
import java.net.Socket;

public class Server {

    private static String LOG_TAG = "SERVER";

    private Address address;

    private int id;

    Server(int id) {
        this.id = id;
        this.address = Config.getServerAddresses().get(id);

        LOG_TAG += "-" + id;

        acceptServers();
    }

    public void acceptServers() {
        Runnable runnable = () -> {
            try {
                ServerSocket serverSocket = new ServerSocket(address.getServerPort());
                while (true) {
                    Socket socket = serverSocket.accept();
                    CommonUtils.startThreadWithName(() -> {
                        handleServerRequest(socket);
                    }, "handle-server-thread");
                }
            } catch (Exception e) {
                LogUtils.error(LOG_TAG, "Could not create server socket", e);
            }
        };

        CommonUtils.startThreadWithName(runnable, "accept-server-thread");
        LogUtils.debug(LOG_TAG, "Started server accept thread");
    }

    private void handleServerRequest(Socket socket) {
        Runnable runnable = () -> {
            try {

            } finally {
                CommonUtils.closeQuietly(socket);
            }
        };

        CommonUtils.startThreadWithName(runnable, "handle-server-thread");
    }
}
