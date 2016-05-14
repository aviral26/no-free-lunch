package rocky.raft.dto;

public class GetLeaderAddrReply extends Message.Meta {

    private Address leaderAddress;

    public GetLeaderAddrReply(Address leaderAddress) {
        this.leaderAddress = leaderAddress;
    }

    public Address getLeaderAddress() {
        return leaderAddress;
    }

    @Override
    public String toString() {
        return "GetLeaderAddrReply{" +
                "leaderAddress=" + leaderAddress +
                '}';
    }
}
