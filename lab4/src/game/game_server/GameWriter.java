package game.game_server;

import game.GameModel;
import game.network.DeliverySystem;
import me.ippolitov.fit.snakes.SnakesProto;
import system.Timer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.LinkedList;

public class GameWriter implements Runnable {
    private Timer multicastTimer;
    private GameModel model;

    int myId;
    final int ID_ANY = -1;

    boolean isRunning = true;

    public static final String multicast_ip = "239.192.0.4";
    public static final int multicast_port = 9192;

    GameWriter(GameModel model, int id) {
        final int timeout = 1000;
        multicastTimer = new Timer(timeout);

        this.model = model;
        this.myId = id;
    }

    @Override
    public void run() {
        try {
            while (isRunning) {
                if (multicastTimer.accept()) {
                    sendAnnouncement();
                }
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            System.out.println("QUIT WRITER");
        }
    }


    private void sendAnnouncement() throws IOException {
        LinkedList<SnakesProto.GamePlayer> playerList = new LinkedList<>();
        for ( SnakesProto.GamePlayer.Builder p : model.getPlayers())
            playerList.add(p.build());

        SnakesProto.GamePlayers players = SnakesProto.GamePlayers
                .newBuilder()
                .addAllPlayers(playerList)
                .build();

        SnakesProto.GameMessage.AnnouncementMsg announcementMsg
                = SnakesProto.GameMessage.AnnouncementMsg
                .newBuilder()
                .setPlayers(players)
                .setConfig(model.getConfig())
                .build();

        SnakesProto.GameMessage.Builder message = SnakesProto.GameMessage.newBuilder()
                .setAnnouncement(announcementMsg)
                .setMsgSeq(DeliverySystem.SEQ_ANY);    //don't confirm multicast

        DeliverySystem.getInstance().sendWithoutСonfirm(message,
                new InetSocketAddress(InetAddress.getByName(
                        multicast_ip), multicast_port));
    }


    void sendState(SnakesProto.GameState state) {
        /* Find and set deputy */
        setDeputy();

        for (SnakesProto.GamePlayer.Builder p : model.getPlayers()) {
            if (p.getRole() != SnakesProto.NodeRole.MASTER) {
                SnakesProto.GameMessage.Builder message = SnakesProto.GameMessage.newBuilder()
                        .setState(SnakesProto.GameMessage.StateMsg.newBuilder().setState(state).build())
                        .setSenderId(myId)
                        .setReceiverId(p.getId());

                DeliverySystem.getInstance().send(message);
            }
        }
    }

    void sendError(SnakesProto.GameMessage.ErrorMsg errorMsg, int player_id) {
        DeliverySystem.getInstance().send(
                SnakesProto.GameMessage.newBuilder()
                        .setError(errorMsg)
                        .setSenderId(myId)
                        .setReceiverId(player_id)
        );
    }

    /* Find deputy id in player list */
    private int findDeputy() {
        for (SnakesProto.GamePlayer.Builder p : model.getPlayers()) {
            if (p.getRole() == SnakesProto.NodeRole.DEPUTY)
                return p.getId();
        }
        return ID_ANY;
    }

    /* Set deputy - only for MASTER */
    void setDeputy() {
        if (findDeputy() != ID_ANY)
            return;

        for (SnakesProto.GamePlayer.Builder p : model.getPlayers()) {
            if (p.getRole() != SnakesProto.NodeRole.VIEWER &&
                    p.getRole() != SnakesProto.NodeRole.MASTER) {
                int deputyId = p.getId();

                System.out.println("Deputy is : " + deputyId);

                model.setRole(deputyId, SnakesProto.NodeRole.DEPUTY);

                SnakesProto.GameMessage.RoleChangeMsg change = SnakesProto.GameMessage.RoleChangeMsg
                        .newBuilder()
                        .setSenderRole(SnakesProto.NodeRole.MASTER)
                        .setReceiverRole(SnakesProto.NodeRole.DEPUTY)
                        .build();

                SnakesProto.GameMessage.Builder message = SnakesProto.GameMessage.newBuilder()
                        .setRoleChange(change)
                        .setSenderId(myId)
                        .setReceiverId(deputyId);

                DeliverySystem.getInstance().send(message);
            }
        }
    }

    void sendDeathMessage(int id) {
        if (id == myId)
            return;

        SnakesProto.GameMessage.RoleChangeMsg changeMsg = SnakesProto.GameMessage.RoleChangeMsg
                .newBuilder()
                .setSenderRole(SnakesProto.NodeRole.MASTER)
                .setReceiverRole(SnakesProto.NodeRole.VIEWER)
                .build();

        DeliverySystem.getInstance().send(SnakesProto.GameMessage
                .newBuilder()
                .setRoleChange(changeMsg)
                .setSenderId(myId)
                .setReceiverId(id));
    }


    void quit() {
        isRunning = false;
    }
}
