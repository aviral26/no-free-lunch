package rocky.raft.dto;

import java.io.Serializable;

public class Message implements Serializable {

    public enum Type {
        GET_LEADER_ADDR,
        GET_LEADER_ADDR_REPLY,
        DO_POST,
        DO_POST_REPLY,
        GET_POSTS,
        GET_POSTS_REPLY,
        APPEND_ENTRIES_RPC,
        APPEND_ENTRIES_RPC_REPLY,
        REQUEST_VOTE_RPC,
        REQUEST_VOTE_RPC_REPLY
    }

    public enum Status {
        OK,
        ERROR
    }

    private Type type;

    private Status status;

    private Meta meta;

    private Message() {

    }

    private Message(Type type, Status status, Meta meta) {
        this.type = type;
        this.status = status;
        this.meta = meta;
    }

    public Type getType() {
        return type;
    }

    public Status getStatus() {
        return status;
    }

    public Meta getMeta() {
        return meta;
    }

    @Override
    public String toString() {
        return "Message{" +
                "type=" + type +
                ", status=" + status +
                ", meta=" + meta +
                '}';
    }

    public static class Meta implements Serializable {

    }

    public static class Builder {
        private Message.Type type;
        private Message.Status status;
        private Meta meta;

        public Builder setType(Message.Type type) {
            this.type = type;
            return this;
        }

        public Builder setStatus(Message.Status status) {
            this.status = status;
            return this;
        }

        public Builder setMeta(Meta meta) {
            this.meta = meta;
            return this;
        }

        public Message build() {
            return new Message(type, status, meta);
        }
    }
}
