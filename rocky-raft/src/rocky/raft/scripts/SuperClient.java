package rocky.raft.scripts;

import com.google.gson.Gson;
import rocky.raft.client.Client;
import rocky.raft.client.RaftClient;
import rocky.raft.common.Config;
import rocky.raft.utils.LogUtils;
import rocky.raft.utils.Utils;

import java.io.FileReader;
import java.util.List;

public class SuperClient {

    private static final String LOG_TAG = "SUPER_CLIENT";

    public static void main(String[] args) throws Exception {
        SuperClient superClient = new SuperClient();
        Config config = Config.buildDefault();
        int retries = 3;

        while (retries > 0) {
            try {
                switch (args[0]) {
                    case "l":
                        if (args.length == 1) {
                            superClient.doLookup(config);
                        } else {
                            superClient.doLookup(config, Integer.parseInt(args[1]));
                        }
                        break;
                    case "p":
                        try {
                            superClient.doPost(config, args[1], args[2]);
                        } catch (ArrayIndexOutOfBoundsException e) {
                            LogUtils.debug(LOG_TAG, "Will assign a random long ID to post.");
                            superClient.doPost(config, args[1], Utils.getRandomUuid());
                        }
                        break;
                    case "c":
                        try {
                            superClient.doChangeConfig(config, args[1], args[2]);
                        } catch (ArrayIndexOutOfBoundsException e) {
                            LogUtils.debug(LOG_TAG, "Will assign a random long ID to config change message.");
                            superClient.doChangeConfig(config, args[1], Utils.getRandomUuid());
                        }
                    default:
                }
                break;
            } catch (Exception e) {
                LogUtils.error(LOG_TAG, e.getMessage());
                LogUtils.debug(LOG_TAG, "Retrying...");
                retries--;
            }
        }
    }

    private void doChangeConfig(Config config, String newConfigFile, String id) throws Exception {
        Client client;

        try {
            Config newConfig = new Gson().fromJson(new FileReader(newConfigFile), Config.class);
            LogUtils.debug(LOG_TAG, "Sending new config: " + newConfig);
            client = new RaftClient(config);
            client.configChange(newConfig, id);
        } catch (Exception e) {
            throw new Exception("Failed to do config change.", e);
        }
    }

    private void doLookup(Config config) throws Exception {
        Client client;
        try {
            client = new RaftClient(config);
            List<String> messages = client.lookup();
            LogUtils.debug(LOG_TAG, "Messages: " + messages);
        } catch (Exception e) {
            throw new Exception("Failed to do lookup", e);
        }
    }

    private void doLookup(Config config, int id) throws Exception {
        Client client;

        try {
            client = new RaftClient(config);
            List<String> messages = client.lookup(config.getServerConfig(id));
            LogUtils.debug(LOG_TAG, "Messages: " + messages);
        } catch (Exception e) {
            throw new Exception("Failed to do lookup", e);
        }
    }

    private void doPost(Config config, String message, String id) throws Exception {
        Client client;
        try {
            client = new RaftClient(config);
            client.post(message, id);
        } catch (Exception e) {
            throw new Exception("Failed to post.", e);
        }
    }
}
