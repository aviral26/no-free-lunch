import com.sun.xml.internal.bind.v2.runtime.reflect.opt.Const;
import common.Address;
import common.Constants;
import dsblog.*;
import utils.LogUtils;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Scanner;

/**
 * Created by aviral on 4/25/16.
 */
public class ServerTest {
    public static void main(String[] args) throws Exception {

        int zero = 0, one = 1;
        Config.init();
        ObjectOutputStream objectOutputStream;
        ObjectInputStream objectInputStream;
        Socket socket;

        FileInputStream dbFileReader = new FileInputStream(Constants.DB_FILE + "-" + zero);
        FileInputStream logFileReader = new FileInputStream(Constants.LOG_FILE + "-" + zero);
        String reply, temp;

        // Post to server 0.
        Address address = Config.getServerAddressById(0);
        System.out.println("Posting to server " + zero);
        socket = new Socket(address.getIp(), address.getClientPort());
        objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
        objectOutputStream.writeObject(new Message(Message.Type.POST, "This is the first test message.", Message.Sender.CLIENT, -1));
        objectInputStream = new ObjectInputStream(socket.getInputStream());
        reply = ((Message) objectInputStream.readObject()).getMessage();
        System.out.println("Reply to first POST from server: " + reply);
        assert reply.equals(Constants.STATUS_OK);

        // Lookup database of server 0.
        socket = new Socket(address.getIp(), address.getClientPort());
        objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
        System.out.println("Looking up from server " + zero);
        objectOutputStream.writeObject(new Message(Message.Type.LOOKUP, "The content of this message does not matter.", Message.Sender.CLIENT, -1));
        objectInputStream = new ObjectInputStream(socket.getInputStream());
        reply = ((Message) objectInputStream.readObject()).getMessage();
        System.out.println("Reply to first LOOKUP from server: " + reply);
        assert reply.equals(Constants.STATUS_OK);

        // Check content of database and log files.
        System.out.println("DB contents: ");
        byte[] file_db = new byte[(int) new File(Constants.DB_FILE + "-" + zero).length()];
        dbFileReader.read(file_db);
        System.out.println(new String(file_db, StandardCharsets.US_ASCII));

        System.out.println("Log contents: ");
        byte[] file_log = new byte[(int) new File(Constants.LOG_FILE + "-" + zero).length()];
        logFileReader.read(file_log);
        System.out.println(new String(file_log, StandardCharsets.US_ASCII));

        System.out.println("Done initialising server " + zero);

        // Post to server 1.
        address = Config.getServerAddressById(1);
        System.out.println("Posting to server " + one);
        socket = new Socket(address.getIp(), address.getClientPort());
        objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
        objectOutputStream.writeObject(new Message(Message.Type.POST, "This is the second test message.", Message.Sender
                .CLIENT, -1));
        objectInputStream = new ObjectInputStream(socket.getInputStream());
        reply = ((Message) objectInputStream.readObject()).getMessage();
        System.out.println("Reply to second POST from server: " + reply);
        assert reply.equals(Constants.STATUS_OK);

        // Lookup database of server 1.
        socket = new Socket(address.getIp(), address.getClientPort());
        objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
        System.out.println("Looking up from server " + one);
        objectOutputStream.writeObject(new Message(Message.Type.LOOKUP, "The content of this message does not matter.", Message.Sender.CLIENT, -1));
        objectInputStream = new ObjectInputStream(socket.getInputStream());
        reply = ((Message) objectInputStream.readObject()).getMessage();
        System.out.println("Reply to second LOOKUP from server: " + reply);
        assert reply.equals(Constants.STATUS_OK);

        // Check content of database and log files.
        System.out.println("DB contents: ");
        file_db = new byte[(int) new File(Constants.DB_FILE + "-" + one).length()];
        dbFileReader.read(file_db);
        System.out.println(new String(file_db, StandardCharsets.US_ASCII));

        System.out.println("Log contents: ");
        file_log = new byte[(int) new File(Constants.LOG_FILE + "-" + one).length()];
        logFileReader.read(file_log);
        System.out.println(new String(file_log, StandardCharsets.US_ASCII));

        System.out.println("Done initialising server " + one);

        // Test sync - sync server 1 with server 0.
        socket = new Socket(address.getIp(), address.getClientPort());
        objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
        objectOutputStream.writeObject(new Message(Message.Type.SYNC, String.valueOf(zero), Message.Sender.CLIENT, -1));
        objectInputStream = new ObjectInputStream(socket.getInputStream());
        reply = ((Message) objectInputStream.readObject()).getMessage();
        System.out.println("Reply to SYNC from server: " + reply);
        assert reply.equals(Constants.STATUS_OK);

        // Check content of database and log files.
        System.out.println("DB contents of server 1: ");
        file_db = new byte[(int) new File(Constants.DB_FILE + "-" + one).length()];
        dbFileReader.read(file_db);
        System.out.println(new String(file_db, StandardCharsets.US_ASCII));

        System.out.println("Log contents of server 1: ");
        file_log = new byte[(int) new File(Constants.LOG_FILE + "-" + one).length()];
        logFileReader.read(file_log);
        System.out.println(new String(file_log, StandardCharsets.US_ASCII));


        // Test sync - sync server 0 with server 1.
        address = Config.getServerAddressById(0);
        socket = new Socket(address.getIp(), address.getClientPort());
        objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
        objectOutputStream.writeObject(new Message(Message.Type.SYNC, String.valueOf(one), Message.Sender.CLIENT, -1));
        objectInputStream = new ObjectInputStream(socket.getInputStream());
        reply = ((Message) objectInputStream.readObject()).getMessage();
        System.out.println("Reply to SYNC from server: " + reply);
        assert reply.equals(Constants.STATUS_OK);

        // Check content of database and log files.
        System.out.println("DB contents of server 0: ");
        file_db = new byte[(int) new File(Constants.DB_FILE + "-" + zero).length()];
        dbFileReader.read(file_db);
        System.out.println(new String(file_db, StandardCharsets.US_ASCII));

        System.out.println("Log contents of server 0: ");
        file_log = new byte[(int) new File(Constants.LOG_FILE + "-" + zero).length()];
        logFileReader.read(file_log);
        System.out.println(new String(file_log, StandardCharsets.US_ASCII));


        Client client = new Client();
        client.doPost(2, "First test message to 2.");
        client.doPost(2, "Second test message to 2.");

        System.out.println("Lookup server 2: " + client.doLookup(2));

        client.doSync(2, 0);

        System.out.println("Lookup server 2 after syncing with server 0: " + client.doLookup(2));

        System.out.println("Done.");
    }
}
