package rocky.raft.log;

import rocky.raft.common.Config;
import rocky.raft.dto.LogEntry;

import java.io.IOException;
import java.util.List;

public interface Log {

    boolean append(LogEntry entry) throws IOException;

    void resize(int size) throws IOException;

    LogEntry last() throws IOException;

    LogEntry get(int index) throws IOException;

    List<LogEntry> getAll(int fromIndex) throws IOException;

    Config getLatestConfig() throws IOException;
}
