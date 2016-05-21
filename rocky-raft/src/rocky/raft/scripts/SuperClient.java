package rocky.raft.scripts;

import com.google.gson.Gson;
import rocky.raft.client.Client;
import rocky.raft.client.RaftClient;
import rocky.raft.common.Config;
import rocky.raft.utils.LogUtils;

import java.io.FileReader;
import java.util.List;

public class SuperClient {

    private static final String LOG_TAG = "SUPER_CLIENT";

    public static void main(String[] args) throws Exception {
        SuperClient superClient = new SuperClient();
        Config config = Config.buildDefault();

        switch (args[0]) {
            case "l":
                if (args.length == 1) {
                    superClient.doLookup(config);
                } else {
                    superClient.doLookup(config, Integer.parseInt(args[1]));
                }
                break;
            case "p":
                superClient.doPost(config, args[1]);
                break;
            case "c":
                superClient.doChangeConfig(config, args[1]);
            default:
        }
    }

    private void doChangeConfig(Config config, String newConfigFile) {
        Client client;

        try {
            Config newConfig = new Gson().fromJson(new FileReader(newConfigFile), Config.class);
            LogUtils.debug(LOG_TAG, "Sending new config: " + newConfig);
            client = new RaftClient(config);
            client.configChange(newConfig);
        } catch (Exception e) {
            LogUtils.error(LOG_TAG, "Failed to do config change.", e);
        }
    }

    private void doLookup(Config config) {
        Client client;

        try {
            client = new RaftClient(config);
            List<String> messages = client.lookup();
            LogUtils.debug(LOG_TAG, "Messages: " + messages);
        } catch (Exception e) {
            LogUtils.error(LOG_TAG, "Failed to do lookup", e);
        }
    }

    private void doLookup(Config config, int id) {
        Client client;

        try {
            client = new RaftClient(config);
            List<String> messages = client.lookup(config.getServerConfig(id));
            LogUtils.debug(LOG_TAG, "Messages: " + messages);
        } catch (Exception e) {
            LogUtils.error(LOG_TAG, "Failed to do lookup", e);
        }
    }

    private void doPost(Config config, String message) {
        Client client;

        try {
            client = new RaftClient(config);
            client.post(message);
        } catch (Exception e) {
            LogUtils.error(LOG_TAG, "Failed to do post", e);
        }
    }
}
