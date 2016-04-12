package dsblog;

import utils.LogUtils;

import java.net.ServerSocket;
import java.net.Socket;

public class Server {

    private static String LOG_TAG = "Server";
    private int port;

    Server(int port){
        this.port = port;
        LOG_TAG += "-" + port;
    }

    public void run(){

        try{
            ServerSocket serverSocket = new ServerSocket(port);

            while(true) {
                LogUtils.debug(LOG_TAG, "Waiting for new request.");
                Thread handleClientRequestThread = new Thread(new HandleClientRequest(serverSocket.accept()));
                handleClientRequestThread.start();
            }

        }
        catch(Exception e){
            LogUtils.error(LOG_TAG, "Could not create server socket.", e);
        }
    }

    private class HandleClientRequest extends Thread{
        HandleClientRequest(Socket client){
            LogUtils.debug(LOG_TAG, "Accepted connection from client: " + client);
        }
    }

}
