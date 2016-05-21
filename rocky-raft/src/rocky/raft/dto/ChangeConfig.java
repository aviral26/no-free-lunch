package rocky.raft.dto;

import rocky.raft.common.Config;

public class ChangeConfig extends Message.Meta {

    private Config config;

    public ChangeConfig(Config config) {
        this.config = config;
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
                "config=" + config +
                '}';
    }
}
