package dsblog;

public class Event {

    private int timestamp;
    private String value;
    private int node;

    public int getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(int timestamp) {
        this.timestamp = timestamp;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public int getNode() {
        return node;
    }

    public void setNode(int node) {
        this.node = node;
    }

    public String toString(){
        return "node: " + node + "\ntimestamp: " + timestamp + "\nvalue: " + value;
    }
}
