package rocky.raft.server;

import rocky.raft.dto.Address;

public class ServerContext {

    private int id;

    private Address address;

    private Address leaderAddress;

    private Log log;

    private Store store;

    private int commitIndex;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public Address getLeaderAddress() {
        return leaderAddress;
    }

    public void setLeaderAddress(Address leaderAddress) {
        this.leaderAddress = leaderAddress;
    }

    public Log getLog() {
        return log;
    }

    public void setLog(Log log) {
        this.log = log;
    }

    public Store getStore() {
        return store;
    }

    public void setStore(Store store) {
        this.store = store;
    }

    public int getCommitIndex() {
        return commitIndex;
    }

    public void setCommitIndex(int commitIndex) {
        this.commitIndex = commitIndex;
    }
}
