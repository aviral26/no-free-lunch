package rocky.raft.dto;

import java.io.Serializable;

public class LogEntry implements Serializable {

    private int index;

    private int term;

    private String value;

    public LogEntry(int index, int term, String value) {
        this.index = index;
        this.term = term;
        this.value = value;
    }

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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LogEntry entry = (LogEntry) o;

        if (index != entry.index) return false;
        if (term != entry.term) return false;
        return value != null ? value.equals(entry.value) : entry.value == null;

    }

    @Override
    public int hashCode() {
        int result = index;
        result = 31 * result + term;
        result = 31 * result + (value != null ? value.hashCode() : 0);
        return result;
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
