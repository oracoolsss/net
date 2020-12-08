package forms.join;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.util.Pair;
import me.ippolitov.fit.snakes.SnakesProto;

import java.io.IOException;
import java.net.*;
import java.util.HashMap;

public class ConnectionsListener implements Runnable {
    boolean isRunning = true;

    private final ObservableList<String> options;

    private HashMap<InetSocketAddress,
            SnakesProto.GameMessage.AnnouncementMsg> availableServers;

    private HashMap<InetSocketAddress, Long> timeouts;
    private final int MAX_TIMEOUT = 3000;

    private HashMap<String, Pair<InetSocketAddress, SnakesProto.GameMessage.AnnouncementMsg>> optionToAddress;

    public static final String multicast_ip = "239.192.0.4";
    public static final int multicast_port = 9192;
    public static MulticastSocket multicast;

    ConnectionsListener(ObservableList<String> options,
                        HashMap<String, Pair<InetSocketAddress,
                                SnakesProto.GameMessage.AnnouncementMsg>> optionToAddress)
            throws IOException {

        final int LISTEN_TIMEOUT = 10;

        this.options = options;
        this.availableServers = new HashMap<>();
        this.optionToAddress = optionToAddress;
        this.timeouts = new HashMap<>();

        multicast = new MulticastSocket(multicast_port);
        multicast.setSoTimeout(LISTEN_TIMEOUT);
        multicast.joinGroup(InetAddress.getByName(multicast_ip));

        Platform.runLater(options::clear);
    }


    @Override
    public void run() {
        try {
            while (isRunning) {
                byte[] data = new byte[2048];
                DatagramPacket recv = new DatagramPacket(data, data.length);

                checkConnections();

                try {
                    multicast.receive(recv);
                    byte[] bytes = new byte[recv.getLength()];

                    System.arraycopy(recv.getData(), recv.getOffset(), bytes, 0, recv.getLength());
                    SnakesProto.GameMessage message = SnakesProto.GameMessage.parseFrom(bytes);

                    if (!message.hasAnnouncement())
                        return;

                    synchronized (optionToAddress) {
                        Platform.runLater(() -> {
                            var old = availableServers.put(
                                    new InetSocketAddress(recv.getAddress(), recv.getPort()),
                                    message.getAnnouncement());

                            if (old != null && !old.equals(message.getAnnouncement())) {
                                System.out.println("Remove");
                                options.remove(announcementToOption(
                                        new InetSocketAddress(recv.getAddress(), recv.getPort()), old));
                            }

                            timeouts.put(new InetSocketAddress(recv.getAddress(),
                                    recv.getPort()), System.currentTimeMillis());

                            for (var e : availableServers.entrySet()) {
                                InetSocketAddress address = e.getKey();
                                SnakesProto.GameMessage.AnnouncementMsg announcementMsg = e.getValue();

                                String opt = announcementToOption(address, announcementMsg);

                                if (!options.contains(opt))
                                    options.add(opt);
                                optionToAddress.put(opt, new Pair<>(address, announcementMsg));
                            }
                        });
                    }
                }
                catch (SocketTimeoutException e) {
                    /* Nothing */
                }
            }
        }
        catch (IOException e) {
            System.out.println("Connection Listener EXITED");
        }
    }

    private void checkConnections() {
        long check_time = System.currentTimeMillis();
        for (var node : timeouts.entrySet()) {
            InetSocketAddress address = node.getKey();
            Long time = node.getValue();
            if (check_time - time > MAX_TIMEOUT) {
                SnakesProto.GameMessage.AnnouncementMsg announcementMsg = availableServers.get(address);
                if (announcementMsg == null)
                    continue;

                synchronized (optionToAddress) {
                    String opt = announcementToOption(address, announcementMsg);

                    Platform.runLater(() -> {
                        availableServers.remove(address);
                        options.remove(opt);
                        optionToAddress.remove(opt);
                    });
                }
            }
        }
    }

    private String announcementToOption(InetSocketAddress address,
                                        SnakesProto.GameMessage.AnnouncementMsg announcementMsg) {
        return address.getAddress().toString() + " " + address.getPort() +
                " players: " + announcementMsg.getPlayers().getPlayersList().size();
    }

    public void quit() {
        multicast.close();
        isRunning = false;
    }
}
