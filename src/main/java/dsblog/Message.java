package dsblog;

import java.io.Serializable;

public class Message implements Serializable {

    private static final String LOG_TAG = "MESSAGE";

    private Type type;

    private String message;

    private int node;

    public enum Type {
        POST, LOOKUP, SYNC
    }

    public Message(Type type) {
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public int getNode() {
        return node;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setNode(int node) {
        this.node = node;
    }

    @Override
    public String toString() {
        return "Message{" +
                "type=" + type +
                ", message='" + message + '\'' +
                ", node=" + node +
                '}';
    }
}
