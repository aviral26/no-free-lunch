package rocky.raft.dto;

import java.util.List;

public class AppendEntriesRpc extends BaseRpc {

    private int leaderId;
    private int prevLogIndex;
    private int prevLogTerm;
    private List<LogEntry> entries;
    private int leaderCommit;

    private AppendEntriesRpc(int term, int leaderId, int prevLogIndex, int prevLogTerm, List<LogEntry> entries, int leaderCommit) {
        this.term = term;
        this.leaderId = leaderId;
        this.prevLogIndex = prevLogIndex;
        this.prevLogTerm = prevLogTerm;
        this.entries = entries;
        this.leaderCommit = leaderCommit;
    }

    public int getLeaderId() {
        return leaderId;
    }

    public int getPrevLogIndex() {
        return prevLogIndex;
    }

    public int getPrevLogTerm() {
        return prevLogTerm;
    }

    public List<LogEntry> getEntries() {
        return entries;
    }

    public int getLeaderCommit() {
        return leaderCommit;
    }

    public static class Builder {
        private int term;
        private int leaderId;
        private int prevLogIndex;
        private int prevLogTerm;
        private List<LogEntry> entries;
        private int leaderCommit;

        public Builder setTerm(int term) {
            this.term = term;
            return this;
        }

        public Builder setLeaderId(int leaderId) {
            this.leaderId = leaderId;
            return this;
        }

        public Builder setPrevLogIndex(int prevLogIndex) {
            this.prevLogIndex = prevLogIndex;
            return this;
        }

        public Builder setPrevLogTerm(int prevLogTerm) {
            this.prevLogTerm = prevLogTerm;
            return this;
        }

        public Builder setEntries(List<LogEntry> entries) {
            this.entries = entries;
            return this;
        }

        public Builder setLeaderCommit(int leaderCommit) {
            this.leaderCommit = leaderCommit;
            return this;
        }

        public AppendEntriesRpc build() {
            return new AppendEntriesRpc(term, leaderId, prevLogIndex, prevLogTerm, entries, leaderCommit);
        }
    }
}
