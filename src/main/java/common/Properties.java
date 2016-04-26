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
    public Properties(){
        logLevel = LogUtils.LogLevel.DEBUG.getId();
        LogUtils.setLogLevel(logLevel);
        LogUtils.debug("Properties", "TEST MODE.");
        servers = new ArrayList<>();
        for(int i = 0; i < 8; i += 2)
            servers.add(new Address("127.0.0.1", 9000 + i, 9000 + i + 1));
    }

    public List<Address> getServers() {
        return servers;
    }

    public int getLogLevel() {
        return logLevel;
    }
}
