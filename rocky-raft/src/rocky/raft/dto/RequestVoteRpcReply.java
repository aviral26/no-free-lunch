package rocky.raft.dto;


public class RequestVoteRpcReply extends BaseRpc {

    private boolean voteGranted;

    public RequestVoteRpcReply(int term, boolean voteGranted) {
        super(term);
        this.voteGranted = voteGranted;
    }

    public boolean isVoteGranted() {
        return voteGranted;
    }

    @Override
    public String toString() {
        return "RequestVoteRpcReply{" +
                "term=" + term +
                ", voteGranted=" + voteGranted +
                '}';
    }
}
