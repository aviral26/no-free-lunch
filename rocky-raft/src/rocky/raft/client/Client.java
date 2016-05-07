package rocky.raft.client;

import java.util.List;

public interface Client {

    void connect();

    void disconnect();

    List<String> lookup();

    void post(String message);
}
