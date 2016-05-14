package rocky.raft.dto;

public class BaseRpc {

    protected int term;

    public int getTerm() {
        return term;
    }

    public void setTerm(int term) {
        this.term = term;
    }
}
