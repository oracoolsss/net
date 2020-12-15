package com.oracoolsss;

import java.nio.channels.SocketChannel;

public class ChannelAndPort {
    private SocketChannel channel;
    private int port;

    public ChannelAndPort(SocketChannel channel, int port){
        this.channel = channel;
        this.port = port;
    }

    public SocketChannel getChannel() {
        return channel;
    }

    public void setChannel(SocketChannel channel) {
        this.channel = channel;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}