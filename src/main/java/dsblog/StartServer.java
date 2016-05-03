package dsblog;

import common.Address;
import utils.LogUtils;

public class StartServer {

    private static final String LOG_TAG = "START_SERVER";

    public static void main(String[] args) {
        Config.init();
        int id = Integer.parseInt(args[0]);
        Address server = Config.getServerAddressById(id);
        LogUtils.debug(LOG_TAG, "Launching server " + server);

        new Server(id);
    }
}
