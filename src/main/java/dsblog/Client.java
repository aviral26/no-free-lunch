package dsblog;

import common.Address;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class Client {

    public static void main(String[] args) {
        
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

    public String doLookup(int node) throws Exception {
        Address address = Config.getServerAddressById(node);
        Socket socket = new Socket(address.getIp(), address.getClientPort());

        ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
        System.out.println("Looking up from " + address);
        objectOutputStream.writeObject(new Message(Message.Type.LOOKUP, "Blah", Message.Sender.CLIENT, -1));

        ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
        return ((Message) objectInputStream.readObject()).getMessage();
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
