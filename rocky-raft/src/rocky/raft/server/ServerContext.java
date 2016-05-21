package rocky.raft.server;

import rocky.raft.common.Config;
import rocky.raft.dto.Address;
import rocky.raft.dto.LogEntry;
import rocky.raft.dto.ServerConfig;
import rocky.raft.log.Log;
import rocky.raft.log.RaftLog;
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

    private ServerConfig leaderConfig;

    private Store store;

    // Persistent
    private Log log;

    private int commitIndex;

    public ServerContext(int id) throws IOException {
        LOG_TAG += id;
        this.id = id;
        this.leaderConfig = null;
        this.store = new FileStore(new File(STORE_FILE + id));
        this.log = new RaftLog(new File(LOG_FILE + id));
        this.commitIndex = 0;
    }

    public int getId() {
        return id;
    }

    public Address getAddress() {
        return getServerConfig().getAddress();
    }

    public ServerConfig getServerConfig() {
        return getConfig().getServerConfig(id);
    }

    public ServerConfig getLeaderConfig() {
        return leaderConfig;
    }

    public void setLeaderConfig(ServerConfig leaderConfig) {
        this.leaderConfig = leaderConfig;
    }

    public int getCurrentTerm() {
        return Integer.parseInt(store.getOrDefault(CURRENT_TERM_KEY, "0"));
    }

    public void setCurrentTerm(int currentTerm) {
        try {
            store.put(CURRENT_TERM_KEY, String.valueOf(currentTerm));
        } catch (IOException e) {
            LogUtils.error(LOG_TAG, "Failed to persist currentTerm", e);
        }
    }

    public int getVotedFor() {
        return Integer.parseInt(store.getOrDefault(VOTED_FOR_KEY, "-1"));
    }

    public void setVotedFor(int votedFor) {
        try {
            store.put(VOTED_FOR_KEY, String.valueOf(votedFor));
        } catch (IOException e) {
            LogUtils.error(LOG_TAG, "Failed to persist votedFor", e);
        }
    }

    public Log getLog() {
        return log;
    }

    public Config getConfig() {
        Config config = null;
        try {
            config = Config.buildFromLog(log);
        } catch (Exception e) {
            try {
                config = Config.buildDefault();
            } catch (Exception ex) {
                LogUtils.error(LOG_TAG, "Failed to build config", ex);
            }
        }
        return config;
    }

    public int getCommitIndex() {
        return commitIndex;
    }

    public void setCommitIndex(int commitIndex) {
        this.commitIndex = commitIndex;
    }

    public int getLastIndex() throws IOException {
        LogEntry last = getLog().last();
        return last == null ? 0 : last.getIndex();
    }

    public int getLastTerm() throws IOException {
        LogEntry last = getLog().last();
        return last == null ? 0 : last.getTerm();
    }

    @Override
    public String toString() {
        return "ServerContext{" +
                "commitIndex=" + commitIndex +
                ", log=" + log +
                ", config=" + getConfig() +
                ", store=" + store +
                ", leaderConfig=" + leaderConfig +
                ", id=" + id +
                '}';
    }
}
