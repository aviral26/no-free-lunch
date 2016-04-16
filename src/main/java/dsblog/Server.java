package dsblog;

import common.Address;
import common.Constants;
import utils.CommonUtils;
import utils.LogUtils;
import exceptions.UnidentifiedSyncMessageException;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

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
                handleMessage((Message) objectInputStream.readObject(), objectInputStream, objectOutputStream);
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

    private void handleMessage(Message message, ObjectInputStream objectInputStream, ObjectOutputStream
            objectOutputStream) throws
            Exception {
        switch (message.getType()) {
            case POST:
                break;
            case LOOKUP:
                break;
            case SYNC:
                try{
                    handleSyncMessage(message, objectInputStream, objectOutputStream);
                }
                catch(UnidentifiedSyncMessageException | IOException e){
                    LogUtils.error(LOG_TAG, "Could not handle sync message.", e);
                }
                break;
        }
    }

    private void handleSyncMessage (Message message, ObjectInputStream
            objectInputStream, ObjectOutputStream
            objectOutputStream) throws UnidentifiedSyncMessageException, IOException {

        switch (message.getSender()) {
            case SERVER:
                handleSyncMessageFromServer(message, objectInputStream, objectOutputStream);
                break;
            case CLIENT:
                handleSyncMessageFromClient(message, objectOutputStream);
                break;
            default:
                throw new UnidentifiedSyncMessageException();

        }

    }

    private void handleSyncMessageFromServer(Message message, ObjectInputStream objectInputStream, ObjectOutputStream
            objectOutputStream){
        // TODO

    }

    private void handleSyncMessageFromClient (Message message, ObjectOutputStream objectOutputStream) throws
            IOException {

        // Assuming content of message is an integer value indexing into the list of servers.
        Address serverToSyncWith = Config.getServerAddresses().get(Integer.parseInt(message.getMessage()));

        try{
            // Send sync message to other server.
            Socket socket = new Socket(serverToSyncWith.getIp(), serverToSyncWith.getServerPort());
            ObjectOutputStream serverWriter = new ObjectOutputStream(socket.getOutputStream());
            serverWriter.writeObject(new Message(Message.Type.SYNC, Constants.STATUS_SYNC, Message.Sender.SERVER));

            // Wait for the other server to respond back with its TT and a subset of its log entries.
            ObjectInputStream serverReader = new ObjectInputStream(socket.getInputStream());
            LogUtils.debug(LOG_TAG, "Waiting for reply to SYNC message from remote server.");
            Message response = (Message) serverReader.readObject();

            // Acknowledge message received to other server.
            serverWriter.writeObject(new Message(Message.Type.SYNC, Constants.STATUS_OK, Message.Sender.SERVER));
            LogUtils.debug(LOG_TAG, "Acknowledged receipt of TT and log.");

            // Update self TT and Log.
            String[] response_str = response.getMessage().split(Constants.OBJECT_DELIMITER);
            updateSelfTT(TimeTable.fromString(response_str[0]));
            updateSelfLog(listOfEventsFromString(response_str[1]));

            // Reply to client status OK.
            objectOutputStream.writeObject(new Message(Message.Type.SYNC, Constants.STATUS_OK, Message.Sender.SERVER));
            LogUtils.debug(LOG_TAG, "Acknowledging SYNC complete to client.");
        }
        catch(Exception e){
            LogUtils.error(LOG_TAG, "Something went wrong while syncing with the following remote server: " +
                    serverToSyncWith, e);
            // Reply to client sync unsuccessful.
            objectOutputStream.writeObject(new Message(Message.Type.SYNC, Constants.STATUS_FAIL, Message.Sender
                    .SERVER));
        }
    }

    private void updateSelfTT(TimeTable other){
        // TODO
    }

    private void updateSelfLog(List<Event> events){
        // TODO
    }

    private List<Event> listOfEventsFromString(String str){
        // TODO
        return null;
    }

}
