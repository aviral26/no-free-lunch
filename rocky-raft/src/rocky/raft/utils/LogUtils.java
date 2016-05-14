package rocky.raft.utils;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class LogUtils {

    private static final String LOG_PREFIX = "rocky-raft";

    private static LogLevel LOG_LEVEL = LogLevel.DEBUG;

    private static Logger LOGGER = new PrintLogger();

    public enum LogLevel {
        NONE(0), ERROR(1), WARN(2), DEBUG(3);

        private static Map<Integer, LogLevel> MAP = new HashMap<>();

        static {
            for (LogLevel logLevel : LogLevel.values()) {
                MAP.put(logLevel.getId(), logLevel);
            }
        }

        private int id;

        LogLevel(int id) {
            this.id = id;
        }

        public int getId() {
            return this.id;
        }

        public static LogLevel getLogLevelById(int id) {
            return MAP.get(id);
        }
    }

    private interface Logger {

        void debug(String message, Throwable throwable);

        void debug(String message);

        void warn(String message, Throwable throwable);

        void warn(String message);

        void error(String message, Throwable throwable);

        void error(String message);
    }


    public static void debug(String tag, String message, Throwable throwable) {
        LOGGER.debug(getLogMessage(tag, message), throwable);
    }

    public static void debug(String tag, String message) {
        LOGGER.warn(getLogMessage(tag, message));
    }

    public static void warn(String tag, String message, Throwable throwable) {
        LOGGER.debug(getLogMessage(tag, message), throwable);
    }

    public static void warn(String tag, String message) {
        LOGGER.warn(getLogMessage(tag, message));
    }

    public static void error(String tag, String message, Throwable throwable) {
        LOGGER.error(getLogMessage(tag, message), throwable);
    }

    public static void error(String tag, String message) {
        LOGGER.error(getLogMessage(tag, message));
    }

    private static String getLogMessage(String tag, String message) {
        return getCaller(tag) + message;
    }

    private static String getCaller(String tag) {
        String caller = "<unknown>";
        StackTraceElement[] trace = new Throwable().fillInStackTrace().getStackTrace();
        if (trace.length < 3) {
            return caller;
        }
        // Walk up the stack looking for the first caller outside of LogUtils.
        // It will be at least 2 frames up, so start there.
        for (int i = 2; i < trace.length; i++) {
            String clazzName = trace[i].getClassName();
            if (!clazzName.contains("Log")) {
                String callingClass = clazzName;
                callingClass = callingClass.substring(callingClass.lastIndexOf('.') + 1);
                callingClass = callingClass.substring(callingClass.lastIndexOf('$') + 1);
                caller = callingClass + "." + trace[i].getMethodName();
                break;
            }
        }

        String threadName = Thread.currentThread().getName();
        return String.format(Locale.US, "[%s] [%s] %s: %s ", LOG_PREFIX, threadName, tag, caller);
    }

    private static class NoOpLogger implements Logger {

        @Override
        public void debug(String message, Throwable throwable) {

        }

        @Override
        public void debug(String message) {

        }

        @Override
        public void warn(String message, Throwable throwable) {

        }

        @Override
        public void warn(String message) {

        }

        @Override
        public void error(String message, Throwable throwable) {

        }

        @Override
        public void error(String message) {

        }
    }

    private static class PrintLogger implements Logger {

        @Override
        public void debug(String message, Throwable throwable) {
            if (LOG_LEVEL.ordinal() >= LogLevel.DEBUG.ordinal()) {
                print(message, throwable);
            }
        }

        @Override
        public void debug(String message) {
            if (LOG_LEVEL.ordinal() >= LogLevel.DEBUG.ordinal()) {
                print(message, null);
            }
        }

        @Override
        public void warn(String message, Throwable throwable) {
            if (LOG_LEVEL.ordinal() >= LogLevel.WARN.ordinal()) {
                print(message, throwable);
            }
        }

        @Override
        public void warn(String message) {
            if (LOG_LEVEL.ordinal() >= LogLevel.WARN.ordinal()) {
                print(message, null);
            }
        }

        @Override
        public void error(String message, Throwable throwable) {
            if (LOG_LEVEL.ordinal() >= LogLevel.ERROR.ordinal()) {
                print(message, throwable);
            }
        }

        @Override
        public void error(String message) {
            if (LOG_LEVEL.ordinal() >= LogLevel.ERROR.ordinal()) {
                print(message, null);
            }
        }

        private void print(String message, Throwable throwable) {
            if (message != null) {
                System.out.println(message);
            }
            if (throwable != null) {
                throwable.printStackTrace();
            }
        }
    }
}
