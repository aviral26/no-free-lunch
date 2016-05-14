package rocky.raft.store;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import rocky.raft.utils.Utils;

import java.io.*;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class FileStore implements Store {

    private static final String LOG_TAG = "FILE_STORE";

    private File file;

    private Map<String, String> map = new HashMap<>();

    public FileStore(File file) throws IOException {
        if (!file.exists()) {
            initialize(file);
        }
        this.file = file;
        readMap();
    }

    private static void initialize(File file) throws IOException {
        // Use a temp file so we don't leave a partially-initialized file.
        File tempFile = new File(file.getPath() + ".tmp");
        try (RandomAccessFile raf = new RandomAccessFile(tempFile, "rw")) {
            raf.writeBytes("{}");
        }

        // A rename is atomic.
        if (!tempFile.renameTo(file)) {
            throw new IOException("Rename failed!");
        }
    }

    private void readMap() throws IOException {
        Reader reader = null;
        Type type = new TypeToken<Map<String, String>>() {
        }.getType();
        try {
            reader = new FileReader(file);
            Map<String, String> map = new Gson().fromJson(reader, type);
            this.map.putAll(map);
        } finally {
            Utils.closeQuietly(reader);
        }
    }

    private void writeMap() throws IOException {
        Writer writer = null;
        try {
            writer = new FileWriter(file);
            writer.write(new Gson().toJson(map));
        } finally {
            Utils.closeQuietly(writer);
        }
    }

    @Override
    public synchronized void put(String key, String value) throws IOException {
        Map<String, String> clone = new HashMap<>(map);
        clone.put(key, value);
        writeMap();
        map = clone;
    }

    @Override
    public synchronized String get(String key) {
        return map.get(key);
    }
}
