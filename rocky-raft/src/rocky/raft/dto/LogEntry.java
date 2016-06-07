package rocky.raft.dto;

import java.io.Serializable;

public class LogEntry implements Serializable {

    private int index;

    private int term;

    private String value;

    private String id;

    private boolean isConfigEntry;

    public LogEntry(int index, int term, String value, String id) {
        this(index, term, value, id, false);
    }

    public LogEntry(int index, int term, String value, String id, boolean isConfigEntry) {
        this.index = index;
        this.term = term;
        this.value = value;
        this.isConfigEntry = isConfigEntry;
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public boolean isConfigEntry() {
        return isConfigEntry;
    }

    public void setConfigEntry(boolean configEntry) {
        isConfigEntry = configEntry;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LogEntry entry = (LogEntry) o;

        if (index != entry.index) return false;
        if (term != entry.term) return false;
        if (isConfigEntry != entry.isConfigEntry) return false;
        if (value != null ? !value.equals(entry.value) : entry.value != null) return false;
        return id != null ? id.equals(entry.id) : entry.id == null;

    }

    @Override
    public int hashCode() {
        int result = index;
        result = 31 * result + term;
        result = 31 * result + (value != null ? value.hashCode() : 0);
        result = 31 * result + (id != null ? id.hashCode() : 0);
        result = 31 * result + (isConfigEntry ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return "LogEntry{" +
                "index=" + index +
                ", term=" + term +
                ", value='" + value + '\'' +
                ", id='" + id + '\'' +
                ", isConfigEntry=" + isConfigEntry +
                '}';
    }
}
