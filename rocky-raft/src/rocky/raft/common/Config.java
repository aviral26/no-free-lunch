package rocky.raft.common;

import rocky.raft.dto.Address;
import rocky.raft.utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class Config {

    public static List<Address> SERVERS = new ArrayList<>();

    static {
        SERVERS.add(new Address("127.0.0.1", 8000, 9000));
        SERVERS.add(new Address("127.0.0.1", 8001, 9001));
        SERVERS.add(new Address("127.0.0.1", 8002, 9002));
    }

    public static long getElectionTimeout() {
        return Utils.getRandomLong(Constants.TIMEOUT_MIN, Constants.TIMEOUT_MAX + 1);
    }
}
