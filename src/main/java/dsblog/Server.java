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

    private String LOG_TAG = "SERVER";
    private Address address;
    private int id;
    private Log log;
    private Database database;
    private TimeTable timeTable;
    private final ReadWriteLock readWriteLock;

    /**
     * Constructor.
     * @param id We use this id to index into the time table.
     */
    public Server(int id) {
        this.id = id;
        this.address = Config.getServerAddresses().get(id);
        LOG_TAG += "-" + id;
        LogUtils.debug(LOG_TAG, "Server ID: " + id + " IP Address: " + address.getIp() + " Client port: " + address.getClientPort() + " Server port: " + address.getServerPort());

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
                LogUtils.error(LOG_TAG, "Could not create server socket.", e);
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
                LogUtils.error(LOG_TAG, "Could not create client socket.", e);
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
                LogUtils.error(LOG_TAG, "Something went wrong while handling server message.", e);
            } finally {
                CommonUtils.closeQuietly(objectInputStream);
                CommonUtils.closeQuietly(objectOutputStream);
                CommonUtils.closeQuietly(socket);
            }
        };

        CommonUtils.startThreadWithName(runnable, "handle-server-thread");
    }

    private void handleMessage(Message message, ObjectInputStream objectInputStream, ObjectOutputStream
            objectOutputStream) throws IOException {
        switch (message.getType()) {
            case POST:
                try{
                    LogUtils.debug(LOG_TAG, "Calling handler for POST message.");
                    handlePostMessage(message, objectOutputStream);
                }
                catch(Exception e){
                    LogUtils.error(LOG_TAG, "Something went wrong while handling post message.", e);
                    objectOutputStream.writeObject(new Message(Message.Type.POST, Constants.STATUS_FAIL, Message.Sender.SERVER, id));
                }
                break;
            case LOOKUP:
                try {
                    LogUtils.debug(LOG_TAG, "Calling handler for LOOKUP message.");
                    handleLookupMessage(message, objectOutputStream);
                }
                catch(Exception e){
                    LogUtils.error(LOG_TAG, "Something went wrong while handling look-up message.", e);
                    objectOutputStream.writeObject(new Message(Message.Type.POST, Constants.STATUS_FAIL, Message.Sender.SERVER, id));
                }
                break;
            case SYNC:
                try{
                    LogUtils.debug(LOG_TAG, "Calling handler for SYNC message.");
                    handleSyncMessage(message, objectInputStream, objectOutputStream);
                }
                catch (Exception e){
                    LogUtils.error(LOG_TAG, "Something went wrong while handling sync message.", e);
                    objectOutputStream.writeObject(new Message(Message.Type.POST, Constants.STATUS_FAIL, Message.Sender.SERVER, id));
                }
                break;
        }
    }

    private void handlePostMessage(Message message, ObjectOutputStream objectOutputStream) throws IOException {
        String post = message.getMessage();
        Event e = new Event(post, id, timeTable.incrementAndReadMyTimestamp(id));

        // Acquire write log and update self log and database.
        try{
            LogUtils.debug(LOG_TAG, "Waiting for write lock...");
            readWriteLock.writeLock().lock();
            writeSelfLogAndDatabase(e);
        }
        finally {
            readWriteLock.writeLock().unlock();
            LogUtils.debug(LOG_TAG, "Update finished. Released write lock.");
        }

        // Send acknowledgement to client.
        objectOutputStream.writeObject(new Message(Message.Type.POST, Constants.STATUS_OK, Message.Sender.SERVER, id));
    }

    private void handleLookupMessage(Message message, ObjectOutputStream objectOutputStream) throws IOException {
        // Acquire read lock and send the whole data file to the client.
        try {
            LogUtils.debug(LOG_TAG, "Waiting to acquire read lock...");
            readWriteLock.readLock().lock();
            objectOutputStream.writeObject(new Message(Message.Type.LOOKUP, "All posts:\n" + database.lookUp(), Message.Sender.SERVER, id));
        }
        finally {
            readWriteLock.readLock().unlock();
            LogUtils.debug(LOG_TAG, "Read lock released.");
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
        LogUtils.debug(LOG_TAG, "Received SYNC message from server " + message.getNode());
        // Determine which events to send to other server by examining each event in log, and send them.
        try {
            LogUtils.debug(LOG_TAG, "Acquiring read-lock...");
            readWriteLock.readLock().lock();
            List<Event> events = log.readLog();
            int debug_counter = 0;
            for (Event e : events)
                if (!timeTable.hasrec(e, message.getNode())) {
                    events_str.append(e.toString() + Constants.LIST_DELIMITER);
                    debug_counter++;
                }
            LogUtils.debug(LOG_TAG, "Number of events to SYNC: " + debug_counter);
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
                LogUtils.debug(LOG_TAG, "Received status OK to SYNC reply.");
                /* Skipping garbage collection for now. I asked Vaibhav and he said that it's not required.
                try{
                    LogUtils.debug(LOG_TAG, "Waiting for write-lock for garbage collecting log.");
                    readWriteLock.writeLock().lock();
                    garbageCollectLog();
                }
                finally {
                    readWriteLock.writeLock().unlock();
                }
                */
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
            LogUtils.debug(LOG_TAG, "Waiting for reply to SYNC message from remote server...");
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
        String resp = response.getMessage();
        String[] response_str = resp.split(Constants.OBJECT_DELIMITER);
        try {
            LogUtils.debug(LOG_TAG, "Waiting for write-log to update log, database and timetable.");
            readWriteLock.writeLock().lock();
            if(response_str.length >1)
                writeSelfLogAndDatabase(listOfEventsFromString(response_str[1]));
            else
                LogUtils.debug(LOG_TAG, "No new events to add.");
            timeTable.updateSelf(TimeTable.fromString(response_str[0]), id, response.getNode());
        } finally {
            readWriteLock.writeLock().unlock();
            LogUtils.debug(LOG_TAG, "Write-lock released.");
        }
        LogUtils.debug(LOG_TAG, "Updated local log, database and timetable.");

        // Reply to client status OK.
        objectOutputStream.writeObject(new Message(Message.Type.SYNC, Constants.STATUS_OK, Message.Sender.SERVER, id));

        LogUtils.debug(LOG_TAG, "Acknowledged SYNC complete to client.");
    }

    private void writeSelfLogAndDatabase(List<Event> events) throws IOException{
        // Append new events to log and insert in database.
        for(Event e : events) {
            LogUtils.debug(LOG_TAG, "Considering event : " + e);
            if(!timeTable.hasrec(e, id)) {
                LogUtils.debug(LOG_TAG, "Adding this one.");
                writeSelfLogAndDatabase(e);
            }
        }
    }

    private void writeSelfLogAndDatabase(Event e) throws IOException{
        log.append(e);
        database.insert(e.getValue());
    }

    private List<Event> listOfEventsFromString(String str){
        String[] events_str = str.split(Constants.LIST_DELIMITER);
        List<Event> events = new ArrayList<>();

        for(String event_str : events_str)
            events.add(Event.fromString(event_str));
        return events;
    }

    private void garbageCollectLog(){
        // TODO
    }

}
