package game;

import me.ippolitov.fit.snakes.SnakesProto;

import java.net.InetAddress;
import java.net.InetSocketAddress;

public class Player {

    private String name;
    private int id;

    private InetSocketAddress address;
    private int port;

    private SnakesProto.NodeRole role;
    private int score;

    public Player(int id, String name, SnakesProto.NodeRole role, int port) {
        this.id = id;
        this.name = name;
        this.role = role;
        this.port = port;
    }

    public void updateScore() {
        ++score;
    }

    public int getScore() { return score; };

    public String getName() {
        return name;
    }

    public int getId() {
        return id;
    }

    public SnakesProto.NodeRole getRole() {
        return role;
    }

    public InetSocketAddress getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    public void setAddress(InetAddress ip, int port) {
        address = new InetSocketAddress(ip, port);
    }
}
