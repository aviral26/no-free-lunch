package rocky.raft.log;

import com.google.gson.Gson;
import rocky.raft.common.LRUCache;
import rocky.raft.dto.LogEntry;

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

    public RaftLog(File file) throws IOException {
        queueFile = new QueueFile(file);
    }

    @Override
    public synchronized void append(LogEntry entry) throws IOException {
        queueFile.add(new Gson().toJson(entry).getBytes());
        last = entry;
        cache.put(entry.getIndex(), entry);
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
    public synchronized LogEntry get(int index) throws IOException {
        LogEntry logEntry = cache.get(index);
        if (logEntry == null) {
            byte[] data = queueFile.get(index - 1);
            if (data != null) {
                logEntry = new Gson().fromJson(new String(data), LogEntry.class);
                cache.put(index, logEntry);
            }
        }
        return logEntry;
    }

    @Override
    public List<LogEntry> getAll(int fromIndex) throws IOException {
        Gson gson = new Gson();
        List<LogEntry> logEntryList = new ArrayList<>();

        queueFile.forEach(new QueueFile.ElementVisitor() {
            int current = -1;

            @Override
            public boolean read(InputStream in, int length) throws IOException {
                ++current;
                if (current >= fromIndex - 1) {
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
}
