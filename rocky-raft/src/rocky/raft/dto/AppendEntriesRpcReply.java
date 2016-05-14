package rocky.raft.dto;

public class AppendEntriesRpcReply extends BaseRpc {

    private boolean success;

    public AppendEntriesRpcReply(int term, boolean success) {
        super(term);
        this.success = success;
    }

    public boolean isSuccess() {
        return success;
    }

    @Override
    public String toString() {
        return "AppendEntriesRpcReply{" +
                "term=" + term +
                ", success=" + success +
                '}';
    }
}
