package com.oracoolsss;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Main {
    private final static String MESSAGE = "Test";
    private static final int TIME_GAP = 7000;
    private static final int CLEANER_SLEEP_TIME = 2000;
    private static final int SENDER_SLEEP_TIME = 1000;
    private final static String DEFAULT_IP = "228.69.4.20";
    private final static int PORT = 1337;
    private final static int BUFFER_SIZE = 1024;
    private static int connectionsCount = 0;

    public static void main(String[] args) {
        String ip_addr;
        ip_addr = args.length >= 2 ? args[1] : DEFAULT_IP;

        try {
            Map<String, Long> connections = new HashMap<String, Long>();
            Map<String, Long> todo = new HashMap<String, Long>();
            MulticastSocket server, socket;
            InetAddress group = InetAddress.getByName(ip_addr);
            server = new MulticastSocket(PORT);
            server.joinGroup(group);
            socket = new MulticastSocket();
            socket.joinGroup(group);
            byte[] buf = new byte[BUFFER_SIZE];
            long lastSendTime = 0L;

            while (true) {
                Date date = new Date();
                Long currentTime = date.getTime();

                if(currentTime - lastSendTime >= SENDER_SLEEP_TIME) {
                    DatagramPacket packet = new DatagramPacket(MESSAGE.getBytes(), MESSAGE.length(), group, PORT);
                    try {
                        socket.send(packet);
                        Thread.sleep(SENDER_SLEEP_TIME);
                    } catch (IOException | InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                DatagramPacket recv = new DatagramPacket(buf, buf.length);
                try {
                    server.receive(recv);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                todo.put(recv.getSocketAddress().toString(), (new Date()).getTime());

                connections.putAll(todo);
                Map<String, Long> tmp = connections;
                for (Map.Entry<String, Long> val : tmp.entrySet()) {
                    if (val.getValue() + TIME_GAP < currentTime) {
                        connections.remove(val.getKey());
                    }
                }
                if(connections.size() != connectionsCount) {
                    System.out.println("\n" + "~~~~~~~~~~~~~~~~~~");
                    connectionsCount = connections.size();
                    for (Map.Entry<String, Long> val : tmp.entrySet()) {
                        System.out.println(val.getKey());
                    }
                }

                todo.clear();
                try {
                    Thread.sleep(CLEANER_SLEEP_TIME);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}