package rocky.raft.dto;

import java.util.List;

public class GetPostsReply extends Message.Meta {

    private List<String> posts;

    public GetPostsReply(List<String> posts) {
        this.posts = posts;
    }

    public List<String> getPosts() {
        return posts;
    }

    @Override
    public String toString() {
        return "GetPostsReply{" +
                "posts=" + posts +
                '}';
    }
}
