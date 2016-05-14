package rocky.raft.log;

import rocky.raft.dto.LogEntry;

import java.io.IOException;
import java.util.List;

public interface Log {

    void append(LogEntry entry) throws IOException;

    void resize(int size) throws IOException;

    LogEntry last() throws IOException;

    LogEntry get(int index) throws IOException;

    List<LogEntry> getAll() throws IOException;

    List<LogEntry> getAll(int fromIndex) throws IOException;
}
