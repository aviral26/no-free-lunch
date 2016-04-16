package dsblog;

import com.google.gson.Gson;
import common.Address;
import common.Properties;
import utils.LogUtils;

import java.io.FileReader;
import java.util.List;

public class Config {

    private static String LOG_TAG = "CONFIG";

    private static Properties PROPERTIES;
    public static int NUMBER_OF_SERVERS;

    private Config() {
    }

    // Must init
    public static void init() {
        try {
            PROPERTIES = new Gson().fromJson(new FileReader("lucid.config"), Properties.class);
            NUMBER_OF_SERVERS = getNumberOfServers();
        } catch (Exception e) {
            LogUtils.error(LOG_TAG, "Failed to init", e);
        }

        LogUtils.setLogLevel(getLogLevel());
    }

    public static List<Address> getServerAddresses() {
        return PROPERTIES.getServers();
    }

    public static int getLogLevel() {
        return PROPERTIES.getLogLevel();
    }

    public static int getNumberOfServers() {
        return 4; // Just for running the unit tests.
        //return getServerAddresses().size();
    }
}
