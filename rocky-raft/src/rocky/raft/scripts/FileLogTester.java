package rocky.raft.scripts;

import rocky.raft.common.Assertions;
import rocky.raft.dto.LogEntry;
import rocky.raft.log.FileLog;
import rocky.raft.utils.LogUtils;

import java.io.File;

public class FileLogTester {

    private static final String LOG_TAG = "FILE_LOG_TESTER";

    private static final String FILE = "test_file";

    public static void main(String[] args) throws Exception {
        LogUtils.debug(LOG_TAG, "Starting test");

        FileLogTester fileLogTester = new FileLogTester();
        fileLogTester.testAppend();
        fileLogTester.testGet();
        fileLogTester.testResize();

        LogUtils.debug(LOG_TAG, "Test complete");
    }

    private void testAppend() throws Exception {
        LogUtils.debug(LOG_TAG, "Testing append");

        File file = new File(FILE);
        file.delete();
        FileLog fileLog = new FileLog(file);

        fileLog.append(new LogEntry(0, 1, "m01"));
        fileLog.append(new LogEntry(1, 2, "m12"));

        Assertions.checkIfEquals(fileLog.last(), new LogEntry(1, 2, "m12"), "Last LogEntry didn't match");

        file.delete();
    }

    private void testGet() throws Exception {
        LogUtils.debug(LOG_TAG, "Testing get");

        File file = new File(FILE);
        file.delete();
        FileLog fileLog = new FileLog(file);

        fileLog.append(new LogEntry(0, 1, "m01"));
        fileLog.append(new LogEntry(1, 1, "m11"));
        fileLog.append(new LogEntry(2, 2, "m22"));

        Assertions.checkIfEquals(fileLog.get(0), new LogEntry(0, 1, "m01"), "LogEntry didn't match");
        Assertions.checkIfEquals(fileLog.get(1), new LogEntry(1, 1, "m11"), "LogEntry didn't match");
        Assertions.checkIfEquals(fileLog.get(2), new LogEntry(2, 2, "m22"), "LogEntry didn't match");

        file.delete();
    }

    private void testResize() throws Exception {
        LogUtils.debug(LOG_TAG, "Testing resize");

        File file = new File(FILE);
        file.delete();
        FileLog fileLog = new FileLog(file);

        for (int i = 0; i < 10; ++i) {
            fileLog.append(new LogEntry(i, i, String.valueOf(i)));
        }

        fileLog.resize(1);

        Assertions.checkIfEquals(fileLog.last(), new LogEntry(0, 0, "0"), "LogEntry didn't match");

        file.delete();
    }
}
