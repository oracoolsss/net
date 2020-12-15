package com.oracoolsss;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class InitUtils {

    public static DatagramChannel createDatagramSocket(Selector selector, InetSocketAddress bind, int ops) throws IOException {
        DatagramChannel channel = DatagramChannel.open();
        channel.configureBlocking(false);
        channel.connect(bind);
        channel.register(selector, ops);
        return channel;
    }

    public static SocketChannel createSocket(Selector selector, int ops) throws IOException {
        SocketChannel channel = SocketChannel.open();
        channel.configureBlocking(false);
        channel.register(selector, ops);
        return channel;
    }

    public static ServerSocketChannel createServerSocket(Selector selector, InetSocketAddress bind, int ops) throws IOException {
        ServerSocketChannel channel = ServerSocketChannel.open();
        channel.configureBlocking(false);
        channel.socket().bind(bind);
        channel.register(selector, ops);
        return channel;
    }

    public static SocketChannel createSocket(Selector selector, InetSocketAddress bind, int ops) throws IOException {
        SocketChannel channel = SocketChannel.open();
        channel.configureBlocking(false);
        channel.socket().bind(bind);
        channel.register(selector, ops);
        return channel;
    }

    public static SocketChannel createSocket(ServerSocketChannel server, Selector selector, int ops) throws IOException {
        SocketChannel channel = server.accept();
        channel.configureBlocking(false);
        channel.register(selector, ops);
        return channel;
    }

}