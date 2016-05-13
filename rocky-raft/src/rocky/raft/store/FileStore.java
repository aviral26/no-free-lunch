package rocky.raft.store;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import rocky.raft.utils.Utils;

import java.io.*;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class FileStore<K, V> implements Store<K, V> {

    private static final String LOG_TAG = "FILE_STORE";

    private File file;

    private Map<K, V> map = new HashMap<>();

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
        Type type = new TypeToken<Map<K, V>>() {
        }.getType();
        try {
            reader = new FileReader(file);
            Map<K, V> map = new Gson().fromJson(reader, type);
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
    public synchronized void put(K key, V value) throws IOException {
        Map<K, V> clone = new HashMap<>(map);
        clone.put(key, value);
        writeMap();
        map = clone;
    }

    @Override
    public synchronized V get(K key) {
        return map.get(key);
    }
}
