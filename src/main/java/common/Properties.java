package common;

import java.util.List;

public class Properties {

    private List<Address> servers;

    private int logLevel;
    private int numberOfServers;

    public List<Address> getServers() {
        return servers;
    }

    public int getLogLevel() {
        return logLevel;
    }
}
