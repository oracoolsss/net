package game.network;

import me.ippolitov.fit.snakes.SnakesProto;
import system.Timer;


import java.io.IOException;
import java.net.*;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

public class DeliverySystem implements Runnable{
    public static long SEQ_ANY = 0L;

    private final HashMap<InetSocketAddress, ConcurrentLinkedDeque<TimedMessage>> toSend;
    private final ConcurrentLinkedDeque<DatagramPacket> toSendWithoutConfirm;
    private final ConcurrentHashMap<InetSocketAddress, Seq> lastSendSeq;

    private final ConcurrentLinkedDeque<DatagramPacket> received;
    private final ConcurrentHashMap<InetSocketAddress, Seq> lastReceivedSeq;

    private final ConcurrentHashMap<Integer, InetSocketAddress> idToAddress;

    private boolean isRunning = true;

    private static volatile DeliverySystem instance = null;
    private DatagramSocket socket = null;
    public int myPort;

    private DeliverySystem() {
        toSend = new HashMap<>();
        toSendWithoutConfirm = new ConcurrentLinkedDeque<>();
        lastSendSeq = new ConcurrentHashMap<>();

        received = new ConcurrentLinkedDeque<>();
        lastReceivedSeq = new ConcurrentHashMap<>();

        idToAddress = new ConcurrentHashMap<>();
    }

    public static DeliverySystem getInstance() {
        DeliverySystem localInstance = instance;
        if (localInstance == null) {
            synchronized (DeliverySystem.class) {
                localInstance = instance;
                if (localInstance == null) {
                    instance = localInstance = new DeliverySystem();
                }
            }
        }
        return localInstance;
    }

    public void removeId(int id) {
        InetSocketAddress address = idToAddress.get(id);
        if (address == null)
            return;

        synchronized (toSend) {
            toSend.remove(address);
            lastSendSeq.remove(address);
            lastReceivedSeq.remove(address);
        }
    }

    public void addDestination(int id, InetSocketAddress address) {
        idToAddress.put(id, address);
    }

    public static SnakesProto.GameMessage toMessage(DatagramPacket p) {
        byte[] bytes = new byte[p.getLength()];
        System.arraycopy(p.getData(), p.getOffset(), bytes, 0, p.getLength());
        SnakesProto.GameMessage message = null;
        try {
            message = SnakesProto.GameMessage.parseFrom(bytes);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return message;
    }

    public LinkedList<DatagramPacket> getAllReceived() {
        int size = received.size();
        if (size > 0) {
            LinkedList<DatagramPacket> answer = new LinkedList<>();
            for (int i = 0; i < size; ++i) {
                answer.addLast(received.getFirst());
                received.removeFirst();
            }
            return answer;
        }
        return null;
    }

    public DatagramPacket getFirstReceived() {
        if (!isRunning)
            return null;

        int size = received.size();
        if (size > 0) {
            DatagramPacket answer = received.getFirst();
            received.removeFirst();
            return answer;
        }
        return null;
    }



    public void setPort(int port) {
        myPort = port;
    }


    public void sendWithoutСonfirm(SnakesProto.GameMessage.Builder packet, InetSocketAddress address) {
        synchronized (toSendWithoutConfirm) {
            SnakesProto.GameMessage message = packet.build();

            byte[] bytes = message.toByteArray();

            toSendWithoutConfirm.addLast(
                    new DatagramPacket(bytes, bytes.length, address.getAddress(), address.getPort()));
        }
    }

    public void sendWithoutСonfirm(SnakesProto.GameMessage.Builder packet) {
        synchronized (toSendWithoutConfirm) {
            SnakesProto.GameMessage message = packet.build();
            int receiverId = packet.getReceiverId();
            InetSocketAddress address = idToAddress.get(receiverId);
            if (address == null) {
                System.err.println("Couldn't find id " + receiverId);
                return;
            }

            byte[] bytes = message.toByteArray();

            toSendWithoutConfirm.addLast(
                    new DatagramPacket(bytes, bytes.length, address.getAddress(), address.getPort()));
        }
    }


    public void send(SnakesProto.GameMessage.Builder packet) {
        synchronized (toSend) {
            int receiverId = packet.getReceiverId();
            InetSocketAddress address = idToAddress.get(receiverId);
            if (address == null) {
                System.err.println("Couldn't find id " + receiverId);
                return;
            }

            if (!lastSendSeq.containsKey(address)) {
                lastSendSeq.put(address, new Seq());
                toSend.put(address, new ConcurrentLinkedDeque<>());
            }
            SnakesProto.GameMessage message = packet
                    .setMsgSeq(lastSendSeq.get(address).getSeq())
                    .build();

            toSend.get(address).addLast(new TimedMessage(message));
            lastSendSeq.get(address).update();
        }
    }

    public void send(SnakesProto.GameMessage.Builder packet, InetSocketAddress address) {
        synchronized (toSend) {
            if (!lastSendSeq.containsKey(address)) {
                lastSendSeq.put(address, new Seq());
                toSend.put(address, new ConcurrentLinkedDeque<>());
            }
            SnakesProto.GameMessage message = packet
                    .setMsgSeq(lastSendSeq.get(address).getSeq())
                    .build();

            toSend.get(address).addLast(new TimedMessage(message));
            lastSendSeq.get(address).update();
        }
    }


    @Override
    public void run() {
        System.out.println("Delivery system started!");
        try {
            while (isRunning) {
                sendMessages();
                receiveMessages();
            }
        }
        catch (IOException e) {
            //e.printStackTrace();
        }
        finally {
            System.out.println("Delivery system executed");
        }
    }

    public void changeReceiver(int old_id, int new_id) {
        synchronized (toSend) {
            InetSocketAddress old_address = idToAddress.get(old_id);
            InetSocketAddress new_address = idToAddress.get(new_id);
            if (old_address == null || new_address == null)
                return;

            toSend.put(new_address, new ConcurrentLinkedDeque<>());

            for (TimedMessage timedMessage : toSend.get(old_address)){
                if (!timedMessage.message.hasAck())
                    send(timedMessage.message.toBuilder().setReceiverId(new_id));
            }
        }
    }

    private void sendMessages() throws IOException {
        synchronized (toSend) {
            for (Map.Entry<InetSocketAddress, ConcurrentLinkedDeque<TimedMessage>> e : toSend.entrySet()) {
                ConcurrentLinkedDeque<TimedMessage> messageList = e.getValue();
                InetSocketAddress address = e.getKey();

                for (TimedMessage timedMessage : messageList) {
                    if (timedMessage.timer.accept()) {
                        byte[] bytes = timedMessage.message.toByteArray();
                        socket.send(new DatagramPacket(
                                bytes, bytes.length,
                                address.getAddress(), address.getPort()));
                    }
                }
            }
        }
        synchronized (toSendWithoutConfirm) {
            for (DatagramPacket p : toSendWithoutConfirm) {
                socket.send(p);
            }
            toSendWithoutConfirm.clear();
        }
    }

    private void receiveMessages() throws IOException {
        final int RECEIVE_TIMEOUT = 5;
        byte[] data = new byte[2048];
        DatagramPacket receivedMessage = new DatagramPacket(data, data.length);

        try {
            socket.setSoTimeout(RECEIVE_TIMEOUT);
            socket.receive(receivedMessage);
        }
        catch (SocketTimeoutException e) {
            return;
        }

        SnakesProto.GameMessage message = toMessage(receivedMessage);
        if (message == null) {
            return;
        }

        long seq = message.getMsgSeq();
        InetSocketAddress address = new InetSocketAddress(
                receivedMessage.getAddress(), receivedMessage.getPort());

        if (message.hasSenderId())
            idToAddress.put(message.getSenderId(), address);    //Now we know id and address



        /* If it was ACK */
        if (message.hasAck()) {
            synchronized (toSend) {
                if (toSend.get(address) != null) {
                    toSend.get(address).removeIf(timedMessage -> timedMessage.message.getMsgSeq() == seq);
                    received.add(receivedMessage);
                }
            }
            return;
        }

        if (message.hasJoin()) {
            System.out.println("Join!");
            lastReceivedSeq.put(address, new Seq(seq));
            received.addLast(receivedMessage);
            return;
        }

        /* Message from unknown Node */
        if (!lastReceivedSeq.containsKey(address)) {
            lastReceivedSeq.put(address, new Seq(seq));
            received.addLast(receivedMessage);
        }
        else {
            /* If it required message */
            if (lastReceivedSeq.get(address).getSeq() + 1 == seq) {
                lastReceivedSeq.get(address).update();
                received.addLast(receivedMessage);
            }
            /* If it wrong message */
            else if (lastReceivedSeq.get(address).getSeq() + 1 < seq) {
                System.out.println("DROP! REQ: " + lastReceivedSeq.get(address).getSeq() + "  GOT: " + seq);
            }
        }
    }

    public void start() throws IOException {
        isRunning = true;
        socket = new DatagramSocket(myPort);
        new Thread(instance).start();
    }

    public void quit() {
        System.out.println("Pausing...");

        isRunning = false;

        received.clear();
        lastReceivedSeq.clear();

        toSend.clear();
        toSendWithoutConfirm.clear();
        lastSendSeq.clear();

        idToAddress.clear();

        if (socket != null)
            socket.close();
    }

    public boolean isRunning() {
        return isRunning;
    }
}


class TimedMessage {
    private final long ACK_TIMEOUT = 30;

    SnakesProto.GameMessage message;
    Timer timer;

    TimedMessage(SnakesProto.GameMessage message) {
        this.timer = new Timer(ACK_TIMEOUT);
        this.message = message;
    }
}


class Seq {
    private long s = 0L;

    Seq() {}

    Seq(long val) {
        s = val;
    }

    void update() {
        ++s;
    }

    long getSeq() {
        return s;
    }
}
