package rocky.raft.server;

import rocky.raft.common.Config;
import rocky.raft.dto.Address;
import rocky.raft.log.CachedFileLog;
import rocky.raft.log.Log;
import rocky.raft.store.FileStore;
import rocky.raft.store.Store;
import rocky.raft.utils.LogUtils;

import java.io.File;
import java.io.IOException;

public class ServerContext {

    private String LOG_TAG = "SERVER_CONTEXT-";

    private static final String CURRENT_TERM_KEY = "CURRENT_TERM";

    private static final String VOTED_FOR_KEY = "VOTED_FOR";

    private String LOG_FILE = "raft-log-";

    private String STORE_FILE = "raft-store-";

    private int id;

    private Address address;

    private Address leaderAddress;

    private Store<String, Integer> store;

    // Persistent
    private int currentTerm;

    // Persistent
    private int votedFor;

    // Persistent
    private Log log;

    private int commitIndex;

    public ServerContext(int id) throws IOException {
        LOG_TAG += id;
        this.id = id;
        this.address = Config.SERVERS.get(id);
        this.leaderAddress = null;
        this.store = new FileStore<>(new File(STORE_FILE + id));
        this.log = new CachedFileLog(new File(LOG_FILE + id));
        this.commitIndex = 0;
        initPersistentVars();
    }

    private void initPersistentVars() throws IOException {
        Integer currentTerm = store.get(CURRENT_TERM_KEY);
        if (currentTerm == null) {
            currentTerm = 0;
        }
        this.currentTerm = currentTerm;

        Integer votedFor = store.get(VOTED_FOR_KEY);
        if (votedFor == null) {
            votedFor = -1;
        }
        this.votedFor = votedFor;
    }

    public int getId() {
        return id;
    }

    public Address getAddress() {
        return address;
    }

    public Address getLeaderAddress() {
        return leaderAddress;
    }

    public void setLeaderAddress(Address leaderAddress) {
        this.leaderAddress = leaderAddress;
    }

    public int getCurrentTerm() {
        return currentTerm;
    }

    public void setCurrentTerm(int currentTerm) {
        try {
            store.put(CURRENT_TERM_KEY, currentTerm);
            this.currentTerm = currentTerm;
        } catch (IOException e) {
            LogUtils.error(LOG_TAG, "Failed to persist currentTerm", e);
        }
    }

    public int getVotedFor() {
        return votedFor;
    }

    public void setVotedFor(int votedFor) {
        try {
            store.put(VOTED_FOR_KEY, votedFor);
            this.votedFor = votedFor;
        } catch (IOException e) {
            LogUtils.error(LOG_TAG, "Failed to persist votedFor", e);
        }
    }

    public Log getLog() {
        return log;
    }

    public int getCommitIndex() {
        return commitIndex;
    }

    public void setCommitIndex(int commitIndex) {
        this.commitIndex = commitIndex;
    }

    @Override
    public String toString() {
        return "ServerContext{" +
                "commitIndex=" + commitIndex +
                ", votedFor=" + votedFor +
                ", currentTerm=" + currentTerm +
                ", leaderAddress=" + leaderAddress +
                ", address=" + address +
                ", id=" + id +
                '}';
    }
}
