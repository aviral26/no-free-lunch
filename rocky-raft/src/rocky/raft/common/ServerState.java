package rocky.raft.common;

public enum ServerState {
    INACTIVE,
    PASSIVE,
    FOLLOWER,
    CANDIDATE,
    LEADER
}
