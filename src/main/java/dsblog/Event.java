package dsblog;

import utils.LogUtils;

public class Event {

    private static final String LOG_TAG = "Event";
    private int timestamp;
    private String value;
    private int node;

    public Event(){
    }

    public Event(String value, int node, int timestamp){
        this.value = value;
        this.node = node;
        this.timestamp = timestamp;
    }

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
        return "node:" + node + "\ntimestamp:" + timestamp + "\nvalue:" + value;
    }

    public static Event fromString(String str){
        Event e;
        try{
            String[] map = str.split("\n");
            e = new Event();
            e.setNode(Integer.parseInt(map[0].split(":")[1]));
            e.setTimestamp(Integer.parseInt(map[1].split(":")[1]));
            e.setValue(map[2].split(":")[1]);
            return e;
        }
        catch(Exception ex){
            LogUtils.error(LOG_TAG, "Something went wrong while translating String to Event.", ex);
            return null;
        }
    }
}
