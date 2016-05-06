package dsblog;

import java.io.Serializable;

public class Message implements Serializable {

    private static final String LOG_TAG = "MESSAGE";

    private Type type;
    /**
     * If type = Type.POST and sender = client, then message = content of post.
     *
     * If type = Type.SYNC and sender = client, then message = index into the list of servers to sync with.
     *
     * If type = Type.SYNC and sender = remote server with whom we are syncing, message = remote_TT +
     * OBJECT_DELIMITER + event + LIST_DELIMITER + event ..... + LIST_DELIMITER + event + LIST_DELIMITER
     */
    private String message;
    private Sender sender;
    private int node;


    public Message(Type type) {
        this.type = type;
    }

    public Message(Type type, String message, Sender sender, int id){
        this.type = type;
        this.message = message;
        this.sender = sender;
        this.node = id;
    }
    public Sender getSender() {
        return sender;
    }

    public void setSender(Sender sender) {
        this.sender = sender;
    }

    public enum Sender {
        SERVER, CLIENT
    }

    public enum Type {
        POST, LOOKUP, SYNC
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
