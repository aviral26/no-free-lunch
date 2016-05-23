package rocky.raft.log;

import com.google.gson.Gson;
import rocky.raft.common.LRUCache;
import rocky.raft.dto.LogEntry;
import rocky.raft.utils.LogUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * RaftLog is responsible for caching and converting between LogEntry index and QueueFile index
 */
public class RaftLog implements Log {

    private QueueFile queueFile;

    private LRUCache<Integer, LogEntry> cache = new LRUCache<>(1024);

    private LogEntry last;

    private LogEntry config;

    private int FIRST_LOG_ENTRY_INDEX = 1; // TODO This should be 1 or 0?

    public RaftLog(File file) throws IOException {
        this.queueFile = new QueueFile(file);
    }

    private boolean verifyNotDuplicate(LogEntry entry) throws IOException {
        for(LogEntry logEntry : getAll(FIRST_LOG_ENTRY_INDEX)){
            if(logEntry.getId() == entry.getId())
                return false;
        }
        return true;
    }

    @Override
    public synchronized boolean append(LogEntry entry) throws IOException {

        if(verifyNotDuplicate(entry)){
            queueFile.add(new Gson().toJson(entry).getBytes());
            last = entry;
            cache.put(entry.getIndex(), entry);
            if (last.isConfigEntry()) config = last;
            return true;
        }

        return false;
    }

    @Override
    public synchronized void resize(int size) throws IOException {
        queueFile.resize(size);
        last = cache.get(size);
        Iterator<Integer> iterator = cache.keySet().iterator();
        while (iterator.hasNext()) {
            int index = iterator.next();
            if (index > size) {
                iterator.remove();
            }
        }
        config = null;
        if (last != null && last.isConfigEntry()) config = last;
    }

    @Override
    public synchronized LogEntry last() throws IOException {
        if (last == null) {
            byte[] data = queueFile.last();
            if (data != null) {
                last = new Gson().fromJson(new String(data), LogEntry.class);
                cache.put(last.getIndex(), last);
            }
        }
        return last;
    }

    @Override
    public synchronized LogEntry get(int logEntryIndex) throws IOException {
        LogEntry logEntry = cache.get(logEntryIndex);
        if (logEntry == null) {
            byte[] data = queueFile.get(logEntryIndex - 1);
            if (data != null) {
                logEntry = new Gson().fromJson(new String(data), LogEntry.class);
                cache.put(logEntryIndex, logEntry);
            }
        }
        return logEntry;
    }

    @Override
    public synchronized List<LogEntry> getAll(int fromLogEntryIndex) throws IOException {
        Gson gson = new Gson();
        List<LogEntry> logEntryList = new ArrayList<>();

        queueFile.forEach(new QueueFile.ElementVisitor() {
            int current = -1;

            @Override
            public boolean read(InputStream in, int length) throws IOException {
                ++current;
                if (current >= fromLogEntryIndex - 1) {
                    LogEntry entry = cache.get(current + 1);
                    if (entry == null) {
                        byte[] data = new byte[length];
                        in.read(data, 0, length);
                        entry = gson.fromJson(new String(data), LogEntry.class);
                    }
                    logEntryList.add(entry);
                }
                return true;
            }
        });
        return logEntryList;
    }

    @Override
    public synchronized LogEntry getLatestConfig() throws IOException {
        // TODO make this more efficient.
        if (config == null) {
            for (LogEntry logEntry : getAll(1)) {
                if (logEntry.isConfigEntry()) {
                    config = logEntry;
                }
            }
        }
        return config;
    }
}
