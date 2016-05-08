package rocky.raft.client;

import rocky.raft.dto.Address;

import java.util.List;

public interface Client {

    List<String> lookup() throws Exception;

    List<String> lookup(Address address) throws Exception;

    void post(String message) throws Exception;
}
