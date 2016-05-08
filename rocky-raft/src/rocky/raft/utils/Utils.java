package rocky.raft.utils;

import java.io.*;
import java.net.Socket;
import java.util.List;

public class Utils {

    private static final String LOG_TAG = "UTILS";

    public static ObjectOutputStream writeAndFlush(Socket socket, Object object) throws IOException {
        return writeAndFlush(getOos(socket), object);
    }

    public static ObjectOutputStream writeAndFlush(ObjectOutputStream oos, Object object) throws IOException {
        oos.writeObject(object);
        oos.flush();
        return oos;
    }

    public static ObjectOutputStream getOos(Socket socket) throws IOException {
        OutputStream os = socket.getOutputStream();
        return new ObjectOutputStream(os);
    }

    public static ObjectInputStream getOis(Socket socket) throws IOException {
        InputStream is = socket.getInputStream();
        return new ObjectInputStream(is);
    }

    public static Thread startThread(String name, Runnable runnable) {
        Thread thread = new Thread(runnable);
        thread.setName(name);
        thread.start();
        return thread;
    }

    public static <T> void dumpList(List<T> list) {
        for (T obj : list) {
            LogUtils.debug(LOG_TAG, obj.toString());
        }
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
