package rocky.raft.store;

import java.io.IOException;

public interface Store<K, V> {

    void put(K key, V value) throws IOException;

    V get(K key);
}
