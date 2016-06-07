package rocky.raft.log;

import com.google.gson.Gson;
import rocky.raft.common.Config;
import rocky.raft.common.LRUCache;
import rocky.raft.dto.LogEntry;
import rocky.raft.utils.LogUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * RaftLog is responsible for caching and converting between LogEntry index and QueueFile index
 */
public class RaftLog implements Log {

    private StackFile stackFile;

    private static final String LOG_TAG = "RaftLog-";

    private LRUCache<Integer, LogEntry> cache = new LRUCache<>(1024);

    private LogEntry last;

    private Config config;

    public RaftLog(File file) throws IOException {
        this.stackFile = new StackFile(file);
    }

    @Override
    public synchronized boolean append(LogEntry entry) throws IOException {
        CheckDuplicateEntry checkDuplicateEntry = new CheckDuplicateEntry(entry);
        stackFile.forEachReverse(checkDuplicateEntry);
        if (checkDuplicateEntry.isDuplicate()) {
            LogUtils.debug(LOG_TAG, "Duplicate entry. Not appending.");
            return false;
        }

        stackFile.push(new Gson().toJson(entry).getBytes());
        last = entry;
        cache.put(entry.getIndex(), entry);
        if (last.isConfigEntry()) setConfig(last);
        return true;
    }

    @Override
    public synchronized void resize(int size) throws IOException {
        stackFile.pop(stackFile.size() - size);
        last = cache.get(size);
        Iterator<Integer> iterator = cache.keySet().iterator();
        while (iterator.hasNext()) {
            int index = iterator.next();
            if (index > size) {
                iterator.remove();
            }
        }
        config = null;
        if (last != null && last.isConfigEntry()) setConfig(last);
    }

    @Override
    public synchronized LogEntry last() throws IOException {
        if (last == null) {
            byte[] data = stackFile.top();
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
            GetReverseVisitor visitor = new GetReverseVisitor(logEntryIndex, stackFile.size());
            stackFile.forEachReverse(visitor);
            logEntry = visitor.getLogEntry();
        }
        return logEntry;
    }

    @Override
    public synchronized List<LogEntry> getAll(int fromLogEntryIndex) throws IOException {
        List<LogEntry> logEntryList = new ArrayList<>();
        stackFile.forEachReverse(new StackFile.ElementVisitor() {
            int current = stackFile.size();

            @Override
            public boolean read(StackFile.Element element, InputStream in) throws IOException {
                if (current >= fromLogEntryIndex) {
                    LogEntry entry = getEntry(current, element, in);
                    logEntryList.add(entry);
                    current--;
                    return true;
                }
                return false;
            }
        });
        Collections.reverse(logEntryList);
        return logEntryList;
    }

    @Override
    public synchronized Config getLatestConfig() throws IOException {
        if (config == null) {
            stackFile.forEachReverse(new StackFile.ElementVisitor() {
                int current = stackFile.size();

                @Override
                public boolean read(StackFile.Element element, InputStream in) throws IOException {
                    LogEntry entry = getEntry(current, element, in);
                    if (entry.isConfigEntry()) {
                        setConfig(entry);
                        return false;
                    }
                    current--;
                    return true;
                }
            });
        }
        return config;
    }

    private LogEntry getEntry(int index, StackFile.Element element, InputStream in) throws IOException {
        LogEntry entry = cache.get(index);
        if (entry == null) {
            Gson gson = new Gson();
            byte[] data = new byte[element.length];
            in.read(data);
            entry = gson.fromJson(new String(data), LogEntry.class);
            cache.put(entry.getIndex(), entry);
        }
        return entry;
    }

    private void setConfig(LogEntry entry) {
        if (entry == null) config = null;
        config = new Gson().fromJson(entry.getValue(), Config.class);
    }

    private class GetReverseVisitor implements StackFile.ElementVisitor {

        private int index;
        private int current;
        private LogEntry logEntry;

        public GetReverseVisitor(int index, int size) {
            this.index = index;
            this.current = size;
        }

        public LogEntry getLogEntry() {
            return logEntry;
        }

        @Override
        public boolean read(StackFile.Element element, InputStream in) throws IOException {
            if (current < index) {
                return false;
            }
            if (current == index) {
                logEntry = getEntry(current, element, in);
            }
            current--;
            return true;
        }
    }

    private class CheckDuplicateEntry implements StackFile.ElementVisitor {

        private boolean duplicate;
        private int current;
        private LogEntry entry;

        CheckDuplicateEntry(LogEntry entry) {
            this.duplicate = false;
            this.current = stackFile.size();
            this.entry = entry;
        }

        public boolean isDuplicate() {
            return duplicate;
        }

        @Override
        public boolean read(StackFile.Element element, InputStream in) throws IOException {
            LogEntry logEntry = getEntry(current, element, in);
            if (logEntry.getId().equals(entry.getId())) {
                this.duplicate = true;
                return false;
            }
            current--;
            return true;
        }
    }
}
