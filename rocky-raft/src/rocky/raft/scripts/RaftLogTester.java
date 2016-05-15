package rocky.raft.scripts;

import rocky.raft.dto.LogEntry;
import rocky.raft.log.RaftLog;
import rocky.raft.utils.Assertions;
import rocky.raft.utils.LogUtils;

import java.io.File;

public class RaftLogTester {

    private static final String LOG_TAG = "FILE_LOG_TESTER";

    private static final String FILE = "test_file";

    public static void main(String[] args) throws Exception {
        LogUtils.debug(LOG_TAG, "Starting test");

        RaftLogTester raftLogTester = new RaftLogTester();
        raftLogTester.testAppend();
        raftLogTester.testGet();
        raftLogTester.testResize();

        LogUtils.debug(LOG_TAG, "Test complete");
    }

    private void testAppend() throws Exception {
        LogUtils.debug(LOG_TAG, "Testing append");

        File file = new File(FILE);
        file.delete();
        RaftLog raftLog = new RaftLog(file);

        raftLog.append(new LogEntry(1, 1, "m1"));
        raftLog.append(new LogEntry(2, 2, "m2"));

        Assertions.checkIfEquals(raftLog.last(), new LogEntry(2, 2, "m2"), "Last LogEntry didn't match");

        file.delete();
    }

    private void testGet() throws Exception {
        LogUtils.debug(LOG_TAG, "Testing get");

        File file = new File(FILE);
        file.delete();
        RaftLog raftLog = new RaftLog(file);

        raftLog.append(new LogEntry(1, 1, "m1"));
        raftLog.append(new LogEntry(2, 2, "m2"));
        raftLog.append(new LogEntry(3, 3, "m3"));

        Assertions.checkIfEquals(raftLog.get(1), new LogEntry(1, 1, "m1"), "LogEntry didn't match");
        Assertions.checkIfEquals(raftLog.get(2), new LogEntry(2, 2, "m2"), "LogEntry didn't match");
        Assertions.checkIfEquals(raftLog.get(3), new LogEntry(3, 3, "m3"), "LogEntry didn't match");

        file.delete();
    }

    private void testResize() throws Exception {
        LogUtils.debug(LOG_TAG, "Testing resize");

        File file = new File(FILE);
        file.delete();
        RaftLog raftLog = new RaftLog(file);

        for (int i = 0; i < 10; ++i) {
            raftLog.append(new LogEntry(i + 1, i, String.valueOf(i)));
        }

        raftLog.resize(1);

        Assertions.checkIfEquals(raftLog.last(), new LogEntry(1, 0, "0"), "LogEntry didn't match");

        file.delete();
    }
}
