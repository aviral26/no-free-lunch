package rocky.raft.dto;

import java.io.Serializable;

public class Message implements Serializable {

    public enum Type {
        GET_LEADER_ADDR,
        LEADER_ADDR
    }

    public enum Sender {
        SERVER,
        CLIENT
    }

    private Type messageType;

    private Sender sender;

    private String message;

    public Message(Sender sender, Type messageType) {
        this.sender = sender;
        this.messageType = messageType;
    }

    public Type getMessageType() {
        return messageType;
    }

    public void setMessageType(Type messageType) {
        this.messageType = messageType;
    }

    public Sender getSender() {
        return sender;
    }

    public void setSender(Sender sender) {
        this.sender = sender;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "Message{" +
                "messageType=" + messageType +
                ", sender=" + sender +
                ", message='" + message + '\'' +
                '}';
    }
}
