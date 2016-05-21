package rocky.raft.dto;

import java.io.Serializable;

public class Address implements Serializable {

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Address address = (Address) o;

        if (clientPort != address.clientPort) return false;
        if (serverPort != address.serverPort) return false;
        return ip != null ? ip.equals(address.ip) : address.ip == null;

    }

    @Override
    public int hashCode() {
        int result = ip != null ? ip.hashCode() : 0;
        result = 31 * result + clientPort;
        result = 31 * result + serverPort;
        return result;
    }
}
