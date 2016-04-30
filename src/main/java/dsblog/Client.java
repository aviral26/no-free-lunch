package dsblog;

import common.Address;
import common.Constants;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class Client {

    public Client() {
        Config.init();
    }

    public static void main(String[] args) {
        Client client = new Client();
        switch (args[0]) {
            case "p":
                try {
                    client.doPost(Integer.parseInt(args[1]), args[2]);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case "l":
                try {
                    String[] response = client.doLookup(Integer.parseInt(args[1]));
                    System.out.println("Response:");
                    for (String line : response) {
                        System.out.println(line);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case "s":
                try {
                    client.doSync(Integer.parseInt(args[1]), Integer.parseInt(args[2]));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            default:
                System.out.println("Invalid command");
                break;
        }
    }

    public void doPost(int node, String message) throws Exception {
        Address address = Config.getServerAddressById(node);
        System.out.println("Posting message to " + address);
        Socket socket = new Socket(address.getIp(), address.getClientPort());

        ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
        objectOutputStream.writeObject(new Message(Message.Type.POST, message, Message.Sender.CLIENT, -1));

        ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
        String reply = ((Message) objectInputStream.readObject()).getMessage();
        System.out.println("Response: " + reply);
    }

    public String[] doLookup(int node) throws Exception {
        Address address = Config.getServerAddressById(node);
        Socket socket = new Socket(address.getIp(), address.getClientPort());

        ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
        System.out.println("Looking up from " + address);
        objectOutputStream.writeObject(new Message(Message.Type.LOOKUP, "Blah", Message.Sender.CLIENT, -1));

        ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
        String response = ((Message) objectInputStream.readObject()).getMessage();

        return response.split(Constants.OBJECT_DELIMITER);
    }

    public void doSync(int node, int other) throws Exception {
        Address address = Config.getServerAddressById(node);
        Address otherAddress = Config.getServerAddressById(other);
        Socket socket = new Socket(address.getIp(), address.getClientPort());
        System.out.println("Syncing " + address + " with " + otherAddress);

        ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
        objectOutputStream.writeObject(new Message(Message.Type.SYNC, String.valueOf(other), Message.Sender.CLIENT, -1));

        ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
        String response = ((Message) objectInputStream.readObject()).getMessage();
        System.out.println("Response: " + response);
    }
}
