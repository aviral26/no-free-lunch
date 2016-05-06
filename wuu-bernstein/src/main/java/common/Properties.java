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
        servers.add(new Address("127.0.0.1", 9000, 9001));
        servers.add(new Address("127.0.0.1", 9002, 9003));
        servers.add(new Address("127.0.0.1", 9004, 9005));
    }

    public List<Address> getServers() {
        return servers;
    }

    public int getLogLevel() {
        return logLevel;
    }
}
