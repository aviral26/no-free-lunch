package rocky.raft.dto;


public class RequestVoteRpcReply extends BaseRpc {

    private boolean voteGranted;

    public boolean isVoteGranted() {
        return voteGranted;
    }

    public void setVoteGranted(boolean voteGranted) {
        this.voteGranted = voteGranted;
    }
}
