package dsblog;

import common.Address;
import utils.CommonUtils;

import java.util.List;

/**
 * Created by aviral on 4/11/16.
 */
public class StartServers {

    private static final String LOG_TAG = "START_SERVERS";

    public static void main(String[] args) {
        List<Address> addressList = Config.getServerAddresses();
        for (int i = 0; i < addressList.size(); ++i) {
            final int id = i;
            CommonUtils.startThreadWithName(() -> {
                new Server(id);
            }, "server-" + id);
        }
    }
}
