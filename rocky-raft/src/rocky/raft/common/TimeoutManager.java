package rocky.raft.common;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Provides methods to execute a runnable after a timeout.
 */
public class TimeoutManager {

    private static final String LOG_TAG = "TIMEOUT_MANAGER";

    private static final TimeoutManager INSTANCE = new TimeoutManager();

    private ScheduledThreadPoolExecutor executor;

    private Map<String, ScheduledFuture> scheduledFutureMap = new HashMap<>();

    private TimeoutManager() {
        executor = new ScheduledThreadPoolExecutor(1);
        executor.setRemoveOnCancelPolicy(true);
    }

    public static TimeoutManager getInstance() {
        return INSTANCE;
    }

    /**
     * Add a runnable to be executed when times out. Associated with a tag for convenience. If there is existing
     * runnable already scheduled, it cancels it and schedules again.
     *
     * @param tag
     * @param runnable
     * @param timeoutMillis
     * @return {@link ScheduledFuture} corresponding to the submitted runnable
     */
    public synchronized ScheduledFuture<?> add(String tag, Runnable runnable, long timeoutMillis) {
        remove(tag);
        ScheduledFuture<?> scheduledFuture = executor.schedule(runnable, timeoutMillis, TimeUnit.MILLISECONDS);
        scheduledFutureMap.put(tag, scheduledFuture);
        return scheduledFuture;
    }

    /**
     * Remove and cancel the runnable associated with the tag.
     *
     * @param tag
     */
    public synchronized void remove(String tag) {
        ScheduledFuture scheduledFuture = scheduledFutureMap.remove(tag);
        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
        }
    }
}
