package rocky.raft.utils;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class Utils {

    private static final String LOG_TAG = "UTILS";

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

    public static long getRandomLong(long min, long max) {
        return ThreadLocalRandom.current().nextLong(min, max);
    }
}
