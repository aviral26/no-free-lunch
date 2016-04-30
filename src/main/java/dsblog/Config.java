package dsblog;

import com.google.gson.Gson;
import com.sun.xml.internal.bind.v2.runtime.reflect.opt.Const;
import common.Address;
import common.Constants;
import common.Properties;
import utils.LogUtils;

import java.io.File;
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
            PROPERTIES = Constants.UNIT_TESTING ? new Properties() : new Gson().fromJson(new FileReader("lucid.config"), Properties.class);
            NUMBER_OF_SERVERS = getNumberOfServers();
            LogUtils.debug(LOG_TAG, "Config initialized.");
        } catch (Exception e) {
            LogUtils.error(LOG_TAG, "Failed to init", e);
        }

        LogUtils.setLogLevel(getLogLevel());
    }

    public static List<Address> getServerAddresses() {
        if(PROPERTIES == null)
            LogUtils.error(LOG_TAG, "Properties is null.");
        return PROPERTIES.getServers();
    }

    public static int getLogLevel() {
        return PROPERTIES.getLogLevel();
    }

    public static int getNumberOfServers() {
        return getServerAddresses().size();
    }

    public static Address getServerAddressById(int id){
        return getServerAddresses().get(id);
    }
}
