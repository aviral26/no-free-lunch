package rocky.raft.client;

import rocky.raft.common.Config;
import rocky.raft.dto.ServerConfig;

import java.util.List;

public interface Client {

    List<String> lookup() throws Exception;

    List<String> lookup(ServerConfig serverConfig) throws Exception;

    void post(String message) throws Exception;

    void post(String message, long id) throws Exception;

    void configChange(Config newConfig) throws Exception;

    void configChange(Config newConfig, long id) throws Exception;
}
