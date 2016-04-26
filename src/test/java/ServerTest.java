import com.sun.xml.internal.bind.v2.runtime.reflect.opt.Const;
import common.Constants;
import dsblog.Config;
import dsblog.Message;
import dsblog.Server;
import dsblog.StartServers;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

/**
 * Created by aviral on 4/25/16.
 */
public class ServerTest {
    public static void main(String[] args) throws Exception {

        ObjectOutputStream objectOutputStream;
        ObjectInputStream objectInputStream;
        Socket socket;

        int server_id = 0;
        FileInputStream dbFileReader = new FileInputStream(Constants.DB_FILE + "-" + server_id);
        FileInputStream logFileReader = new FileInputStream(Constants.LOG_FILE + "-" + server_id);
        String reply, temp;

        // Post to server.
        System.out.println("Posting to server " + server_id);
        socket = new Socket("127.0.0.1", 9000);
        objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
        objectOutputStream.writeObject(new Message(Message.Type.POST, "This is the first test message.", Message.Sender.CLIENT, -1));
        objectInputStream = new ObjectInputStream(socket.getInputStream());
        reply = ((Message) objectInputStream.readObject()).getMessage();
        System.out.println("Reply to first POST from server: " + reply);
        assert reply.equals(Constants.STATUS_OK);

        // Lookup database.
        socket = new Socket("127.0.0.1", 9000);
        objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
        System.out.println("Looking up from server " + server_id);
        objectOutputStream.writeObject(new Message(Message.Type.LOOKUP, "The content of this message does not matter.", Message.Sender.CLIENT, -1));
        objectInputStream = new ObjectInputStream(socket.getInputStream());
        reply = ((Message) objectInputStream.readObject()).getMessage();
        System.out.println("Reply to first LOOKUP from server: " + reply);
        assert reply.equals(Constants.STATUS_OK);

        // Check content of database and log files.
        System.out.println("DB contents: ");
        byte[] file_db = new byte[(int) new File(Constants.DB_FILE + "-" + server_id).length()];
        dbFileReader.read(file_db);
        System.out.println(new String(file_db));

        System.out.println("Log contents: ");
        byte[] file_log = new byte[(int) new File(Constants.LOG_FILE + "-" + server_id).length()];
        logFileReader.read(file_log);
        System.out.println(new String(file_log));

        System.out.println("Done.");
    }
}
