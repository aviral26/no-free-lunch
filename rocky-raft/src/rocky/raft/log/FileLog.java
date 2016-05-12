package rocky.raft.log;

import com.google.gson.Gson;
import rocky.raft.dto.LogEntry;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FileLog implements Log {

    private QueueFile queueFile;

    public FileLog(File file) throws IOException {
        queueFile = new QueueFile(file);
    }

    @Override
    public void append(LogEntry entry) throws IOException {
        queueFile.add(new Gson().toJson(entry).getBytes());
    }

    @Override
    public void resize(int size) throws IOException {
        queueFile.resize(size);
    }

    @Override
    public LogEntry last() throws IOException {
        byte[] data = queueFile.last();
        if (data != null) {
            return new Gson().fromJson(new String(data), LogEntry.class);
        }
        return null;
    }

    @Override
    public LogEntry get(int index) throws IOException {
        byte[] data = queueFile.get(index);
        if (data != null) {
            return new Gson().fromJson(new String(data), LogEntry.class);
        }
        return null;
    }

    @Override
    public List<LogEntry> getAll() throws IOException {
        Gson gson = new Gson();
        List<LogEntry> logEntryList = new ArrayList<>();

        queueFile.forEach((in, length) -> {
            byte[] data = new byte[length];
            in.read(data, 0, length);
            logEntryList.add(gson.fromJson(new String(data), LogEntry.class));
            return true;
        });
        return logEntryList;
    }
}
