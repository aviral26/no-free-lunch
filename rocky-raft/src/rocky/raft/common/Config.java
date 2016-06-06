package rocky.raft.common;

import com.google.gson.Gson;
import rocky.raft.dto.Address;
import rocky.raft.dto.ServerConfig;
import rocky.raft.log.Log;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Config implements Serializable {

    private static final String LOG_TAG = "CONFIG";

    private static final String DEFAULT_CONFIG_FILE = "default.config";

    private List<ServerConfig> oldServerConfigs;

    private List<ServerConfig> serverConfigs;

    private boolean isJointConfig;

    public Config(List<ServerConfig> oldServerConfigs, List<ServerConfig> serverConfigs, boolean isJointConfig) {
        this.oldServerConfigs = oldServerConfigs;
        this.serverConfigs = serverConfigs;
        this.isJointConfig = isJointConfig;
        initConfigs(this);
    }

    public static Config getFromLog(Log log) throws IOException {
        Config config = log.getLatestConfig();
        config.initConfigs(config);
        return config;
    }

    public static Config buildDefault() throws FileNotFoundException {
        Config config = new Gson().fromJson(new FileReader(DEFAULT_CONFIG_FILE), Config.class);
        config.initConfigs(config);
        return config;
    }

    private void initConfigs(Config config) {
        if (config.oldServerConfigs == null) config.oldServerConfigs = new ArrayList<>();
        if (config.serverConfigs == null) config.serverConfigs = new ArrayList<>();
    }

    public boolean isMajority(List<ServerConfig> configs) {
        if (isJointConfig) {
            int oldCount = 0;
            int newCount = 0;
            for (ServerConfig config : configs) {
                if (oldServerConfigs.contains(config)) oldCount++;
                if (serverConfigs.contains(config)) newCount++;
            }
            return (oldCount > oldServerConfigs.size() / 2) && (newCount > serverConfigs.size() / 2);
        }

        return configs.size() > serverConfigs.size() / 2;
    }

    public List<ServerConfig> getServerConfigs() {
        return serverConfigs;
    }

    public boolean isJointConfig() {
        return isJointConfig;
    }

    public List<ServerConfig> getAll() {
        Set<ServerConfig> all = new HashSet<>();
        all.addAll(oldServerConfigs);
        all.addAll(serverConfigs);
        return new ArrayList<>(all);
    }

    public ServerConfig getServerConfig(int id) {
        for (ServerConfig serverConfig : getAll()) {
            if (serverConfig.getId() == id) {
                return serverConfig;
            }
        }
        return null;
    }

    public Address getServer(int id) {
        for (ServerConfig serverConfig : getAll()) {
            if (serverConfig.getId() == id) {
                return serverConfig.getAddress();
            }
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Config config = (Config) o;

        if (isJointConfig != config.isJointConfig) return false;
        if (oldServerConfigs != null ? !oldServerConfigs.equals(config.oldServerConfigs) : config.oldServerConfigs != null)
            return false;
        return serverConfigs != null ? serverConfigs.equals(config.serverConfigs) : config.serverConfigs == null;

    }

    @Override
    public int hashCode() {
        int result = oldServerConfigs != null ? oldServerConfigs.hashCode() : 0;
        result = 31 * result + (serverConfigs != null ? serverConfigs.hashCode() : 0);
        result = 31 * result + (isJointConfig ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Config{" +
                "oldServerConfigs=" + oldServerConfigs +
                ", serverConfigs=" + serverConfigs +
                ", isJointConfig=" + isJointConfig +
                '}';
    }
}
