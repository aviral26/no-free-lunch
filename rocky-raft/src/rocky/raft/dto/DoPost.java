package rocky.raft.dto;

public class DoPost extends Message.Meta {

    private String post;

    public DoPost(String post) {
        this.post = post;
    }

    public String getPost() {
        return post;
    }

    @Override
    public String toString() {
        return "DoPost{" +
                "post='" + post + '\'' +
                '}';
    }
}
