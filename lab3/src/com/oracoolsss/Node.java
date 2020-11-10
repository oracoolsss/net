package com.oracoolsss;

import java.io.Serializable;
import java.net.InetAddress;

public class Node implements Serializable {
    private InetAddress address;
    private Integer port;

    Node(InetAddress address, Integer port) {
        this.address = address;
        this.port = port;
    }

    InetAddress getAddress() {
        return address;
    }

    Integer getPort() {
        return port;
    }

    public void setAddress(InetAddress address) {
        this.address = address;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Node && ((Node)obj).address.equals(this.address) && ((Node)obj).port.equals(this.port);
    }
}