package dsblog;

import common.Address;
import utils.CommonUtils;

import java.util.List;

public class StartServers {

    private static final String LOG_TAG = "START_SERVERS";

    public static void main(String[] args) {
        Config.init();
        List<Address> addressList = Config.getServerAddresses();
        for (int i = 0; i < addressList.size(); ++i) {
            final int id = i;
            CommonUtils.startThreadWithName(() -> {
                new Server(id);
            }, "server-" + id);
        }
    }
}
