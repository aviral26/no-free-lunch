package dsblog;

import utils.CommonUtils;
import utils.LogUtils;

/**
 * Created by aviral on 4/11/16.
 */
public class StartServers {

    private static final String LOG_TAG = "StartServers";

    public static void main(String[] args){
        Runnable runnable = () -> {
            Server server = new Server(Config.serverPort);
            server.run();
        };
        CommonUtils.startThreadWithName(runnable, "server-1");
        LogUtils.debug(LOG_TAG, "Started server!");
    }
}
