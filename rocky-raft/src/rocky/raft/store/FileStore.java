package rocky.raft.store;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import rocky.raft.utils.NetworkUtils;

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
        map = readMap();
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

    private Map<String, String> readMap() throws IOException {
        Reader reader = null;
        Type type = new TypeToken<Map<String, String>>() {
        }.getType();
        try {
            reader = new FileReader(file);
            return new Gson().fromJson(reader, type);
        } finally {
            NetworkUtils.closeQuietly(reader);
        }
    }

    private void writeMap(Map<String, String> map) throws IOException {
        Writer writer = null;
        try {
            writer = new FileWriter(file);
            writer.write(new Gson().toJson(map));
        } finally {
            NetworkUtils.closeQuietly(writer);
        }
    }

    @Override
    public synchronized void put(String key, String value) throws IOException {
        Map<String, String> clone = new HashMap<>(map);
        clone.put(key, value);
        writeMap(clone);
        map = clone;
    }

    @Override
    public synchronized String get(String key) {
        return map.get(key);
    }

    @Override
    public synchronized String getOrDefault(String key, String defaultValue) {
        String value = get(key);
        if (value == null) {
            value = defaultValue;
        }
        return value;
    }

    @Override
    public String toString() {
        return "FileStore{" +
                "map=" + map +
                ", file=" + file +
                '}';
    }
}
