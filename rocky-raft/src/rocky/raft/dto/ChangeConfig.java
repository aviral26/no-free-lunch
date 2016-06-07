package rocky.raft.dto;

import rocky.raft.common.Config;

public class ChangeConfig extends Message.Meta {

    private String id;

    private Config config;

    public ChangeConfig(String id, Config config) {
        this.id = id;
        this.config = config;
    }

    public String getId() {
        return id;
    }

    public Config getConfig() {
        return config;
    }

    public void setConfig(Config config) {
        this.config = config;
    }

    @Override
    public String toString() {
        return "ChangeConfig{" +
                "id='" + id + '\'' +
                ", config=" + config +
                '}';
    }
}
