package common;

public class Address {

    private String ip;

    private int clientPort;

    private int serverPort;

    public Address(String ip, int clientPort, int serverPort) {
        this.ip = ip;
        this.clientPort = clientPort;
        this.serverPort = serverPort;
    }

    public String getIp() {
        return ip;
    }

    public int getClientPort() {
        return clientPort;
    }

    public int getServerPort() {
        return serverPort;
    }

    @Override
    public String toString() {
        return "Address{" +
                "ip='" + ip + '\'' +
                ", clientPort=" + clientPort +
                ", serverPort=" + serverPort +
                '}';
    }
}
