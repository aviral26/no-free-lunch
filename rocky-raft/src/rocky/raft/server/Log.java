package rocky.raft.server;

import rocky.raft.dto.LogEntry;

import java.util.List;

public interface Log {

    void append(LogEntry entry);

    void append(LogEntry entry, int index, int term);

    LogEntry peek();

    List<LogEntry> lookup();
}
