package rocky.raft.scripts;

import rocky.raft.client.Client;
import rocky.raft.client.RaftClient;
import rocky.raft.common.Config;
import rocky.raft.dto.Address;
import rocky.raft.utils.LogUtils;
import rocky.raft.utils.Utils;

import java.util.List;

public class SuperClient {

    private static final String LOG_TAG = "SUPER_CLIENT";

    public static void main(String[] args) {
        SuperClient superClient = new SuperClient();

        switch (args[0]) {
            case "l":
                if (args.length == 1) {
                    superClient.doLookup();
                } else {
                    superClient.doLookup(Config.SERVERS.get(Integer.valueOf(args[1])));
                }
                break;
            case "p":
                superClient.doPost(args[1]);
                break;
            default:
        }
    }

    private void doLookup() {
        Client client = null;

        try {
            client = new RaftClient(Config.SERVERS);
            List<String> messages = client.lookup();
            LogUtils.debug(LOG_TAG, "Messages:");
            Utils.dumpList(messages);
        } catch (Exception e) {
            LogUtils.error(LOG_TAG, "Failed to do lookup", e);
        }
    }

    private void doLookup(Address address) {
        Client client = null;

        try {
            client = new RaftClient(Config.SERVERS);
            List<String> messages = client.lookup(address);
            LogUtils.debug(LOG_TAG, "Messages:");
            Utils.dumpList(messages);
        } catch (Exception e) {
            LogUtils.error(LOG_TAG, "Failed to do lookup", e);
        }
    }

    private void doPost(String message) {
        Client client = null;

        try {
            client = new RaftClient(Config.SERVERS);
            client.post(message);
        } catch (Exception e) {
            LogUtils.error(LOG_TAG, "Failed to do lookup", e);
        }
    }
}
