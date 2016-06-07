package rocky.raft.dto;

public class DoPost extends Message.Meta {

    private String id;

    private String post;

    public DoPost(String id, String post) {
        this.id = id;
        this.post = post;
    }

    public String getId() {
        return id;
    }

    public String getPost() {
        return post;
    }

    @Override
    public String toString() {
        return "DoPost{" +
                "id='" + id + '\'' +
                ", post='" + post + '\'' +
                '}';
    }
}
