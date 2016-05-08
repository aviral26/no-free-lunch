package rocky.raft.server;

public interface Store {

    void put(String key, int value);

    void get(String key, int value);
}
