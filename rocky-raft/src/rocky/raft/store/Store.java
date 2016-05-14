package rocky.raft.store;

import java.io.IOException;

public interface Store {

    void put(String key, String value) throws IOException;

    String get(String key);
}
