package rocky.raft.dto;

public class RequestVoteRpc extends BaseRpc {

    private int candidateId;
    private int lastLogIndex;
    private int lastLogTerm;

    public RequestVoteRpc(int term, int candidateId, int lastLogIndex, int lastLogTerm) {
        super(term);
        this.candidateId = candidateId;
        this.lastLogIndex = lastLogIndex;
        this.lastLogTerm = lastLogTerm;
    }

    public int getCandidateId() {
        return candidateId;
    }

    public int getLastLogIndex() {
        return lastLogIndex;
    }

    public int getLastLogTerm() {
        return lastLogTerm;
    }

    @Override
    public String toString() {
        return "RequestVoteRpc{" +
                "term=" + term +
                ", candidateId=" + candidateId +
                ", lastLogIndex=" + lastLogIndex +
                ", lastLogTerm=" + lastLogTerm +
                '}';
    }
}
