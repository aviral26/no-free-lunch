package rocky.raft.dto;

public class BaseRpc extends Message.Meta {

    protected int term;

    public BaseRpc(int term) {
        this.term = term;
    }

    public int getTerm() {
        return term;
    }

    @Override
    public String toString() {
        return "BaseRpc{" +
                "term=" + term +
                '}';
    }
}
