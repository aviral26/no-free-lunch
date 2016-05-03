package common;

import utils.LogUtils;

import java.util.ArrayList;
import java.util.List;

public class Properties {

    private List<Address> servers;
    private int logLevel;

    /**
     * This constructor is for tests only. Modify as required.
     */
    public Properties() {
        logLevel = LogUtils.LogLevel.DEBUG.getId();
        LogUtils.setLogLevel(logLevel);
        LogUtils.debug("Properties", "TEST MODE.");
        servers = new ArrayList<>();
        servers.add(new Address("128.111.84.253", 9000, 9001));
        servers.add(new Address("128.111.84.202", 9000, 9001));
        servers.add(new Address("128.111.84.202", 9002, 9003));
    }

    public List<Address> getServers() {
        return servers;
    }

    public int getLogLevel() {
        return logLevel;
    }
}
