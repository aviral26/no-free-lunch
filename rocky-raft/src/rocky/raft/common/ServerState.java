package rocky.raft.common;

public enum ServerState {
    INACTIVE,
    FOLLOWER,
    CANDIDATE,
    LEADER
}
