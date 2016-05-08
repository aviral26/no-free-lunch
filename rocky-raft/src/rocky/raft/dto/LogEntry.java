package rocky.raft.dto;

import java.io.Serializable;

public class LogEntry implements Serializable {

    private int index;

    private int term;

    private String value;

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getTerm() {
        return term;
    }

    public void setTerm(int term) {
        this.term = term;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "LogEntry{" +
                "index=" + index +
                ", term=" + term +
                ", value='" + value + '\'' +
                '}';
    }
}
