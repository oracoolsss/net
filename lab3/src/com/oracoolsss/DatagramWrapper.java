package com.oracoolsss;

import java.io.*;
import java.net.DatagramPacket;

class DatagramWrapper {
    private Message message;
    private Node node;

    private int timesSent = 0;

    private ByteArrayOutputStream baos = new ByteArrayOutputStream(2048);
    private ObjectOutputStream oos;

    DatagramWrapper(Message message, Node node) throws IOException {
        oos = new ObjectOutputStream(baos);

        this.message = message;
        this.node = node;
    }

    DatagramWrapper(DatagramPacket datagramPacket) throws IOException, ClassNotFoundException {
        oos = new ObjectOutputStream(baos);

        this.node = new Node(datagramPacket.getAddress(), datagramPacket.getPort());
        this.message = deserializeObject(datagramPacket.getData());
    }

    Node getNode() {
        return node;
    }

    synchronized DatagramPacket convertToDatagramPacket() throws IOException {
        byte[] toSend = serializeObject(message);
        return new DatagramPacket(toSend, toSend.length, node.getAddress(), node.getPort());
    }

    private synchronized byte[] serializeObject(Object object) throws IOException {
        oos.writeObject(object);
        return baos.toByteArray();
    }

    private synchronized <T> T deserializeObject(byte[] rawData) throws IOException, ClassNotFoundException {
        return (T) new ObjectInputStream(new ByteArrayInputStream(rawData)).readObject();
    }

    Message getMessage() {
        return message;
    }

    public int getTimesSent() {
        return timesSent;
    }

    void increaseTimesSent() {
        timesSent++;
    }
}
