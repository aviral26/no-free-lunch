package rocky.raft.client;

import rocky.raft.common.Config;
import rocky.raft.dto.ServerConfig;

import java.util.List;

public interface Client {

    List<String> lookup() throws Exception;

    List<String> lookup(ServerConfig serverConfig) throws Exception;

    void post(String message, String id) throws Exception;

    void configChange(Config newConfig, String id) throws Exception;
}
