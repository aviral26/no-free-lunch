package rocky.raft.dto;

public class AppendEntriesRpcReply extends BaseRpc {

    private boolean success;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }
}
