package rocky.raft.utils;

import rocky.raft.dto.Message;

import java.io.Closeable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class NetworkUtils {

    private static final String LOG_TAG = "NETWORK_UTILS";

    public static Message readMessage(Socket socket) throws IOException, ClassNotFoundException {
        Message message = (Message) new ObjectInputStream(socket.getInputStream()).readObject();
        //LogUtils.debug(LOG_TAG, "Read " + message);
        return message;
    }

    public static void writeMessage(Socket socket, Message message) throws IOException {
        new ObjectOutputStream(socket.getOutputStream()).writeObject(message);
        //LogUtils.debug(LOG_TAG, "Written " + message);
    }

    public static void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception e) {
            }
        }
    }
}
