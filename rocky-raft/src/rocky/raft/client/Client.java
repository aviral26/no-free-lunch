package rocky.raft.client;

import rocky.raft.dto.Message;

import java.util.List;

public interface Client {

    void connect();

    void disconnect();

    List<Message> lookup();

    void post(Message message);
}
