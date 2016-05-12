package rocky.raft.log;

import rocky.raft.common.LRUCache;
import rocky.raft.dto.LogEntry;

import java.io.File;
import java.io.IOException;

public class CachedFileLog extends FileLog {

    private LRUCache<Integer, LogEntry> cache = new LRUCache<>(128);

    private LogEntry last;

    public CachedFileLog(File file) throws IOException {
        super(file);
    }

    @Override
    public synchronized void append(LogEntry entry) throws IOException {
        super.append(entry);
        last = entry;
        cache.put(entry.getIndex(), entry);
    }

    @Override
    public synchronized void resize(int size) throws IOException {
        super.resize(size);
        last = null;
        cache.clear();
    }

    @Override
    public synchronized LogEntry last() throws IOException {
        if (last == null) {
            last = super.last();
        }
        return last;
    }

    @Override
    public synchronized LogEntry get(int index) throws IOException {
        LogEntry logEntry = cache.get(index);
        if (logEntry == null) {
            logEntry = super.get(index);
        }
        return logEntry;
    }
}
