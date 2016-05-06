package utils;

import java.io.Closeable;
import java.util.List;

public class CommonUtils {

    private static final String LOG_TAG = "CommonUtils";

    public static void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception e) {
                LogUtils.error(LOG_TAG, "Error while closing stream.", e);
            }
        }
    }

    public static Thread startThreadWithName(Runnable runnable, String name) {
        Thread thread = new Thread(runnable);
        thread.setName(name);
        thread.start();
        return thread;
    }

    public static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (Exception e) {
            LogUtils.error(LOG_TAG, "Failed to sleep.", e);
        }
    }

    public static <T> void printList(List<T> list) {
        System.out.println("---list---");
        for (T item : list) {
            System.out.println(item);
        }
        System.out.println("----------");
    }

}
