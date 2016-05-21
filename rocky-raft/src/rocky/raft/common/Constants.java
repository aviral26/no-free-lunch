package rocky.raft.common;


public class Constants {

    public static final long TIMEOUT_MAX = 300;

    public static final long TIMEOUT_MIN = 150;

    public static final long HEARTBEAT_DELAY = 100;

    public static final long CLIENT_TIMEOUT = 10 * HEARTBEAT_DELAY;
}
