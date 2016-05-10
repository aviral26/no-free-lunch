package rocky.raft.dto;

import java.io.Serializable;

public class Message implements Serializable {

    public enum Type {
        GET_LEADER_ADDR,
        LEADER_ADDR,
        DO_POST,
        GET_POSTS,
        POSTS,
        APPEND_ENTRIES_RPC,
        REQUEST_VOTE_RPC,
        APPEND_ENTRIES_RPC_REPLY,
        REQUEST_VOTE_RPC_REPLY,
        ERROR
    }

    public enum Sender {
        SERVER,
        CLIENT
    }

    public enum Status {
        OK,
        FAIL
    }

    private Type messageType;

    private Sender sender;

    private Status status;

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

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
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
                ", status=" + status +
                ", message='" + message + '\'' +
                '}';
    }
}
