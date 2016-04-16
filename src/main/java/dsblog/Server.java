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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Server {

    private static String LOG_TAG = "SERVER";
    private Address address;
    private int id;
    private Log log;
    private Database database;
    private TimeTable timeTable;
    private final ReadWriteLock readWriteLock;

    public Server(int id) {
        this.id = id;
        this.address = Config.getServerAddresses().get(id);

        LOG_TAG += "-" + id;

        log = new Log(id);
        database = new Database(id);
        timeTable = new TimeTable();
        readWriteLock = new ReentrantReadWriteLock();

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
            objectOutputStream) throws IOException {

        StringBuilder events_str = new StringBuilder("");

        // Determine which events to send to other server by examining each event in log, and send them.
        try {
            readWriteLock.readLock().lock();
            LogUtils.debug(LOG_TAG, "Acquired read-lock. Reading...");
            List<Event> events = log.readLog();
            for (Event e : events)
                if (!timeTable.hasrec(e, message.getNode()))
                    events_str.append(e.toString() + Constants.LIST_DELIMITER);

            objectOutputStream.writeObject(new Message(Message.Type.SYNC, timeTable.toString() + Constants
                    .OBJECT_DELIMITER + events_str, Message.Sender.SERVER, id));
        }
        finally {
            readWriteLock.readLock().unlock();
            LogUtils.debug(LOG_TAG, "Read-lock released.");
        }

        // Wait for acknowledgement from other server.
        try{
            Message ack = (Message) objectInputStream.readObject();

            if(ack.getMessage().equals(Constants.STATUS_OK)){
                try{
                    LogUtils.debug(LOG_TAG, "Waiting for write-lock to garbage collect log.");
                    readWriteLock.writeLock().lock();
                    garbageCollectLog();
                }
                finally {
                    readWriteLock.writeLock().unlock();
                }
            }

        }
        catch(ClassNotFoundException e){
            LogUtils.error(LOG_TAG, "Something went wrong while handling acknowledgement of SYNC.", e);
        }

    }

    private void handleSyncMessageFromClient (Message message, ObjectOutputStream objectOutputStream) throws
            IOException {

        // Assuming content of message is an integer value indexing into the list of servers.
        Address serverToSyncWith = Config.getServerAddresses().get(Integer.parseInt(message.getMessage()));
        ObjectOutputStream serverOutputStream = null;
        Message response;
        Socket socket = null;

        try {
            // Send sync message to other server.
            socket = new Socket(serverToSyncWith.getIp(), serverToSyncWith.getServerPort());
            serverOutputStream = new ObjectOutputStream(socket.getOutputStream());
            serverOutputStream.writeObject(new Message(Message.Type.SYNC, Constants.STATUS_SYNC, Message.Sender.SERVER, id));

            // Wait for the other server to respond back with its TT and a subset of its log entries.
            ObjectInputStream serverReader = new ObjectInputStream(socket.getInputStream());
            LogUtils.debug(LOG_TAG, "Waiting for reply to SYNC message from remote server.");
            response = (Message) serverReader.readObject();

            // Acknowledge message received to other server. (Not blocking remote server here for update on this end.)
            serverOutputStream.writeObject(new Message(Message.Type.SYNC, Constants.STATUS_OK, Message.Sender.SERVER, id));
            LogUtils.debug(LOG_TAG, "Acknowledged receipt of TT and log.");
        }
        catch(Exception e) {
            LogUtils.error(LOG_TAG, "Something went wrong while syncing with the following remote server: " +
                    serverToSyncWith, e);
            if(serverOutputStream != null)
                serverOutputStream.writeObject(new Message(Message.Type.SYNC, Constants.STATUS_FAIL, Message.Sender.SERVER, id));

            objectOutputStream.writeObject(new Message(Message.Type.SYNC, Constants.STATUS_FAIL, Message.Sender.SERVER, id));
            return;
        }
        finally {
            CommonUtils.closeQuietly(socket);
            CommonUtils.closeQuietly(serverOutputStream);
        }

        // Update self TT, Database and Log atomically.
        String[] response_str = response.getMessage().split(Constants.OBJECT_DELIMITER);
        try {
            LogUtils.debug(LOG_TAG, "Waiting for write-log to update log, database and timetable.");
            readWriteLock.writeLock().lock();
            timeTable.updateSelf(TimeTable.fromString(response_str[0]), id, response.getNode());
            updateSelfLogAndDatabase(listOfEventsFromString(response_str[1]));
        } finally {
            readWriteLock.writeLock().unlock();
            LogUtils.debug(LOG_TAG, "Write-lock released.");
        }
        LogUtils.debug(LOG_TAG, "Updated local log, database and timetable.");

        // Reply to client status OK.
        objectOutputStream.writeObject(new Message(Message.Type.SYNC, Constants.STATUS_OK, Message.Sender.SERVER, id));

        LogUtils.debug(LOG_TAG, "Acknowledged SYNC complete to client.");
    }

    private void updateSelfLogAndDatabase(List<Event> events) throws IOException{
        // Append all events to log and insert in database.
        for(Event e : events) {
            log.append(e);
            database.insert(e.getValue());
        }
    }

    private List<Event> listOfEventsFromString(String str){
        String[] events_str = str.split(Constants.LIST_DELIMITER);
        List<Event> events = new ArrayList<>();

        for(String event_str : events_str)
            events.add(Event.fromString(event_str));
        return null;
    }

    private void garbageCollectLog(){
        // TODO
    }

}
