package game;

import game.network.DeliverySystem;
import game.objects.Snake;
import me.ippolitov.fit.snakes.SnakesProto;
import system.Timer;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;

public class GameReader implements Runnable {
    private final int JOIN_ERR = -1;
    private final int ID_ANY = -2;

    private final int MAX_MESSAGE_TIMEOUT;

    private GameModel model;
    private boolean isRunning = true;

    private ConcurrentHashMap<Integer, Long> nodesCheckTime;

    public int myId = ID_ANY;
    public int serverId = ID_ANY;

    private SnakesProto.NodeRole myRole;

    private Timer pingTimer;
    private Timer fps_counter;

    private GameController controller;

    public GameReader(GameModel model, SnakesProto.GameConfig config, int myId, int serverId,
                      SnakesProto.NodeRole role, GameController controller) {
        this(model, config);

        this.myId = myId;
        this.serverId = serverId;

        this.myRole = role;
        this.controller = controller;
    }

    private GameReader(GameModel model, SnakesProto.GameConfig config) {
        this.model = model;
        this.nodesCheckTime = new ConcurrentHashMap<>();

        this.pingTimer = new Timer(config.getPingDelayMs());
        this.fps_counter = new Timer((int)(1000.0 / 60));

        this.MAX_MESSAGE_TIMEOUT = config.getNodeTimeoutMs();
    }

    public int getMyId() {
        return myId;
    }

    @Override
    public void run() {
        while (isRunning) {
            if (pingTimer.accept()) {
                sendPing();
                checkNodes();
            }


            LinkedList<DatagramPacket> queue = DeliverySystem.getInstance().getAllReceived();
            if (queue == null) {
                continue;
            }

            for (DatagramPacket p : queue) {
                SnakesProto.GameMessage message = DeliverySystem.toMessage(p);
                if (message == null)
                    continue;

                /* Update node timeout */
                if (message.hasSenderId()) {
                    nodesCheckTime.put(message.getSenderId(), System.currentTimeMillis());
                }

                /* Send Ack */
                if (!message.hasAck() && !message.hasJoin()) {
                    SnakesProto.GameMessage.Builder ack = SnakesProto.GameMessage.newBuilder()
                            .setAck(SnakesProto.GameMessage.AckMsg.newBuilder().build())
                            .setMsgSeq(message.getMsgSeq())
                            .setSenderId(myId);

                    if (message.hasSenderId())
                        ack.setReceiverId(message.getSenderId());

                    DeliverySystem.getInstance().sendWithoutСonfirm(
                            ack, new InetSocketAddress(p.getAddress(), p.getPort()));
                }

                /* SERVER: Join game message - for server */
                if (message.hasJoin()) {
                    int id = handleJoin(message, p.getAddress(), p.getPort());
                    if (id != JOIN_ERR) {
                        nodesCheckTime.put(id, System.currentTimeMillis() + MAX_MESSAGE_TIMEOUT);
                    }
                }

                /* SERVER: Client want rotate snake's head */
                if (message.hasSteer()) {
                    rotateSnake(message.getSenderId(), message.getSteer().getDirection());
                }

                /* CLIENT: Game state message */
                if (message.hasState()) {
                    model.setFromState(message.getState().getState());
                    if (fps_counter.accept()) {
                        model.updateView();
                        model.render();
                    }

                }

                /* CLIENT : Error */
                if (message.hasError()) {
                    System.err.println(message.getError().getErrorMessage());
                }

                /* CLIENT : RoleChange */
                if (message.hasRoleChange()) {
                    System.out.println("Role change");
                    if (myId == message.getReceiverId()) {
                        handleRoleChange(message);
                    }
                }
            }
        }
        System.out.println("READER OUIT");
    }


    private void handleRoleChange(SnakesProto.GameMessage message) {
        if (!message.hasRoleChange())
            return;

        SnakesProto.GameMessage.RoleChangeMsg roleChange = message.getRoleChange();

        if (roleChange.hasReceiverRole()) {
            myRole = roleChange.getReceiverRole();
            System.out.println("My role : " + myRole);

            /* If this node become master */
            if (myRole == SnakesProto.NodeRole.MASTER) {
                changeMaster(message.getSenderId());
            }

            if (myRole == SnakesProto.NodeRole.VIEWER) {
                System.out.println("I died =( ");
            }
        }

        if (roleChange.hasSenderRole()) {
            SnakesProto.NodeRole senderRole = roleChange.getSenderRole();
            if (senderRole == SnakesProto.NodeRole.MASTER) {
                serverId = message.getSenderId();
            }
        }
    }


    private void changeMaster(int prev_master) {
        /* Clear all messages */
        System.out.println("Master dead");
        DeliverySystem.getInstance().removeId(prev_master);

        /* Set up this node */
        model.removePlayer(prev_master);
        model.setRole(myId, SnakesProto.NodeRole.MASTER);

        serverId = myId;
        myRole = SnakesProto.NodeRole.MASTER;

        /* Add ids of other players */
        SnakesProto.GameMessage.RoleChangeMsg roleChange = SnakesProto.GameMessage.RoleChangeMsg
                .newBuilder()
                .setSenderRole(SnakesProto.NodeRole.MASTER)
                .build();

        model.getPlayers().forEach(player -> {
            if (player.getId() != myId) {
                try {
                    System.out.println("Adding" + player.getId());
                    DeliverySystem.getInstance().addDestination(
                            player.getId(), new InetSocketAddress(
                                    InetAddress.getByName(player.getIpAddress()),
                                    player.getPort()));

                    DeliverySystem.getInstance().send(SnakesProto.GameMessage.newBuilder()
                            .setRoleChange(roleChange)
                            .setReceiverId(player.getId())
                            .setSenderId(myId));
                }
                catch (UnknownHostException e) {
                    e.printStackTrace();
                }
            }
        });
        controller.changeToServer(this);
    }


    private void sendPing() {
        if (myRole != SnakesProto.NodeRole.MASTER) {
            DeliverySystem.getInstance().send(SnakesProto.GameMessage.newBuilder()
                    .setPing(SnakesProto.GameMessage.PingMsg.newBuilder().build())
                    .setSenderId(myId)
                    .setReceiverId(serverId));
        }
    }

    private SnakesProto.NodeRole checkMyRole() {
        for (SnakesProto.GamePlayer.Builder p : model.getPlayers()) {
            if (p.getId() == myId)
                return p.getRole();
        }
        return null;
    }

    /* Find deputy id in player list */
    private int findDeputy() {
        for (SnakesProto.GamePlayer.Builder p : model.getPlayers()) {
            if (p.getRole() == SnakesProto.NodeRole.DEPUTY)
                return p.getId();
        }
        return ID_ANY;
    }


    private int findMaster() {
        for (SnakesProto.GamePlayer.Builder p : model.getPlayers()) {
            if (p.getRole() == SnakesProto.NodeRole.MASTER)
                return p.getId();
        }
        return ID_ANY;
    }


    /* Checks that node died and doing some work */
    private void checkNodes() {
        myRole = checkMyRole();

        long checkTime = System.currentTimeMillis();
        for (HashMap.Entry<Integer, Long> node : nodesCheckTime.entrySet()) {
            if (checkTime - node.getValue() > MAX_MESSAGE_TIMEOUT) {
                int playerId = node.getKey();

                /* For server */
                if (myRole == SnakesProto.NodeRole.MASTER) {
                    System.out.println("Player " + playerId  + "removed");
                    model.removePlayer(playerId);

                    /* Drop unconfirmed messages for this node */
                    DeliverySystem.getInstance().removeId(playerId);
                }

                /* For clients */
                if (myRole == SnakesProto.NodeRole.DEPUTY) {
                    if (serverId == playerId) {
                        changeMaster(playerId);
                    }
                }
                if (myRole == SnakesProto.NodeRole.NORMAL) {
                    if (playerId == findMaster()) {
                        int oldMaster = serverId;
                        serverId = findDeputy();
                        model.getPlayers().forEach(player -> {
                            try {
                                if (player.getId() == serverId)
                                    DeliverySystem.getInstance().addDestination(
                                            player.getId(), new InetSocketAddress(
                                                    InetAddress.getByName(player.getIpAddress()), player.getPort()));
                            }
                            catch (UnknownHostException e) {}
                        });
                        DeliverySystem.getInstance().changeReceiver(oldMaster, serverId);
                    }
                }
                nodesCheckTime.remove(playerId);
            }
        }
    }


    private int handleJoin(SnakesProto.GameMessage join, InetAddress ip, int port) {
            SnakesProto.NodeRole role;
            int id = GameModel.generateId();
            System.out.println("Set player id : " + id);

            boolean viewer = join.getJoin().hasOnlyView() && join.getJoin().getOnlyView();

            role = (viewer) ? SnakesProto.NodeRole.VIEWER : SnakesProto.NodeRole.NORMAL;


            if (!model.addPlayer(SnakesProto.GamePlayer.newBuilder()
                    .setId(id)
                    .setName(join.getJoin().getName())
                    .setIpAddress(ip.toString().substring(1))
                    .setPort(port)
                    .setScore(0)
                    .setRole(role)
                    .build(), viewer)) {
                        sendError(ip, port, "Server full !!!");
                        return JOIN_ERR;
            }

            DeliverySystem.getInstance().addDestination(id, new InetSocketAddress(ip, port));

            SnakesProto.GameMessage.AckMsg ack = SnakesProto.GameMessage.AckMsg.newBuilder().build();
            SnakesProto.GameMessage.Builder builder = SnakesProto.GameMessage
                    .newBuilder()
                    .setAck(ack)
                    .setSenderId(myId)
                    .setReceiverId(id)
                    .setMsgSeq(join.getMsgSeq());

            DeliverySystem.getInstance().sendWithoutСonfirm(builder, new InetSocketAddress(ip, port));
            return id;
    }


    private void sendError(InetAddress ip, int port, String cause) {
        SnakesProto.GameMessage.ErrorMsg err = SnakesProto.GameMessage.ErrorMsg.newBuilder()
                .setErrorMessage(cause)
                .build();

        SnakesProto.GameMessage.Builder message = SnakesProto.GameMessage.newBuilder()
                .setError(err)
                .setSenderId(myId);

        DeliverySystem.getInstance().send(message, new InetSocketAddress(ip, port));
    }


    private void rotateSnake(int id, SnakesProto.Direction dir) {
        for (Snake s : model.getSnakes()) {
            if (s.getPlayer() != null) {
                if (s.getPlayer().getId() == id) {
                    s.rotate(dir);
                }
            }
        }
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void quit() {
        isRunning = false;
    }
}
