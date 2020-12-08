package game.objects;

import game.network.DeliverySystem;
import javafx.scene.Group;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import me.ippolitov.fit.snakes.SnakesProto;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;

import static me.ippolitov.fit.snakes.SnakesProto.Direction.UP;
import static me.ippolitov.fit.snakes.SnakesProto.Direction.DOWN;
import static me.ippolitov.fit.snakes.SnakesProto.Direction.LEFT;
import static me.ippolitov.fit.snakes.SnakesProto.Direction.RIGHT;


public class Snake implements GameObject {
    private SnakesProto.GamePlayer.Builder player;
    private Grid grid;
    private LinkedList<Point> body;
    private HashSet<Point> container;

    private SnakesProto.GameState.Snake.SnakeState state =
            SnakesProto.GameState.Snake.SnakeState.ALIVE;

    private Point reserve;
    private SnakesProto.Direction current_direction;
    private SnakesProto.Direction next_direction;
    private boolean growUp = false;

    private boolean isAlive = true;

    public static final int ID_NONE = -1;
    public static int myId = ID_NONE;

    int id;

    public Snake(SnakesProto.GamePlayer.Builder player, Grid grid, int i, int j, int id) {
        this.grid = grid;
        this.player = player;
        this.id = id;


        body = new LinkedList<>();
        container = new HashSet<>();

        body.add(new Point(i, j));
        container.add(new Point(i, j));


        int dir = (int)(Math.random() * 4);
        switch (dir) {
            case 0 : current_direction = UP;
                     break;
            case 1 : current_direction = DOWN;
                     break;
            case 2 : current_direction = LEFT;
                     break;
            case 3 : current_direction = RIGHT;
                     break;
        }
        next_direction = current_direction;
        growUp();
    }


    public Snake(SnakesProto.GamePlayer.Builder player, Grid grid,
                 LinkedList<Point> coords, SnakesProto.Direction dir, int id) {
        this.grid = grid;


        this.body = getFullCoords(coords);

        this.container = new HashSet<>(body);

        this.player = player;
        this.id = id;

        current_direction = dir;
        next_direction = dir;
    }

    synchronized public void setZombie() {
        player = null;
        state = SnakesProto.GameState.Snake.SnakeState.ZOMBIE;
    }

    synchronized public SnakesProto.GameState.Snake.SnakeState getState() {
        return state;
    }

    synchronized public SnakesProto.Direction getDirection() {
        return current_direction;
    }

    synchronized public int getScore() {
        return body.size() - 1;
    }

    synchronized public int getId() {
        return id;
    }

    @Override
    synchronized public void draw(Group graphics) {
        Color color = (player != null && player.getId() == myId) ? Color.GREEN : Color.WHEAT;
        for (Point p : body) {
            grid.drawCell(graphics, p.x, p.y, color);
        }
    }

    synchronized public boolean update() {
        container.remove(body.getLast());

        reserve = new Point(body.getLast());
        Point head = new Point(body.getFirst());

        if (growUp) {
            growUp = false;

            body.add(new Point(reserve));
            container.add(new Point(reserve));
        }

        boolean result = true;

        if (checkDirection()) {
            current_direction = next_direction;
        }
        else {
            next_direction = current_direction;
            result = false;
        }


        switch (current_direction) {
            case LEFT:  head.x = (grid.COLS_COUNT + head.x - 1) % grid.COLS_COUNT;
                break;
            case RIGHT: head.x = (grid.COLS_COUNT + head.x + 1) % grid.COLS_COUNT;
                break;
            case UP:    head.y = (grid.ROWS_COUNT + head.y - 1) % grid.ROWS_COUNT;
                break;
            case DOWN:  head.y = (grid.ROWS_COUNT + head.y + 1) % grid.ROWS_COUNT;
                break;
        }
        body.addFirst(head);
        body.removeLast();

        //Check self-collision
        if (!container.add(new Point(head)))
            destroy();

        return result;
    }

    private boolean checkDirection() {
        switch (next_direction) {
            case UP: return (current_direction != DOWN);
            case DOWN: return (current_direction != UP);
            case LEFT: return (current_direction != RIGHT);
            case RIGHT: return (current_direction != LEFT);
        }
        return false;
    }

    synchronized public void handle(KeyEvent event) {
        switch (event.getCode()) {
            case W:
                moveUp();
                break;
            case S :
                moveDown();
                break;
            case A :
                moveLeft();
                break;
            case D:
                moveRight();
                break;
        }
    }

    synchronized public void rotate(SnakesProto.Direction dir) {
        switch (dir) {
            case UP:
                moveUp();
                break;
            case DOWN:
                moveDown();
                break;
            case LEFT:
                moveLeft();
                break;
            case RIGHT:
                moveRight();
                break;
        }
    }

    synchronized public void growUp() {
        growUp = true;
    }


    private void moveUp() {
        next_direction = UP;
    }

    private void moveDown() {
        next_direction = DOWN;
    }

    private void moveLeft() {
        next_direction = LEFT;
    }

    private void moveRight() {
        next_direction = RIGHT;
    }


    synchronized public void checkCollide(Apple a) {
        if (body.getFirst().equals(a.getPosition())) {
            a.destroy();
            this.growUp();
            if (player != null) {
                this.player.setScore(player.getScore() + 1);
            }
        }
    }

    synchronized public SnakesProto.GamePlayer.Builder getPlayer() {
        return player;
    }

    synchronized public void checkCollide(Snake other) {
        if (other == this)
            return;

        if (other.container.contains(body.getFirst())) {
            destroy();
            if (player != null && player.getRole() != SnakesProto.NodeRole.MASTER)
                player.setRole(SnakesProto.NodeRole.VIEWER);
        }

        if (container.contains(other.body.getFirst())) {
            other.destroy();
            if (other.player != null && other.player.getRole() != SnakesProto.NodeRole.MASTER)
                other.player.setRole(SnakesProto.NodeRole.VIEWER);
        }
    }

    @Override
    synchronized public void destroy() {
        isAlive = false;

        if (player == null)
            return;
        DeliverySystem.getInstance().send(SnakesProto.GameMessage
                .newBuilder()
                .setRoleChange(
                        SnakesProto.GameMessage.RoleChangeMsg.newBuilder()
                                .setReceiverRole(SnakesProto.NodeRole.VIEWER)
                                .setSenderRole(SnakesProto.NodeRole.MASTER).build())
                .setReceiverId(player.getId())
                .setSenderId(myId));
    }

    @Override
    synchronized public boolean isAlive() {
        return isAlive;
    }

    @Override
    synchronized public LinkedList<Point> getCoords() {
        return body;
    }

    synchronized public LinkedList<Point> getCoordsOptimized() {
        ArrayList<Point> bodyArray = new ArrayList<>(body);
        LinkedList<Point> coords = new LinkedList<>();
        coords.add(bodyArray.get(0));

        Point p = new Point(0, 0);

        for (int i = 1; i < bodyArray.size(); ++i) {
            int newX = (grid.COLS_COUNT + bodyArray.get(i).x - bodyArray.get(i - 1).x) % grid.COLS_COUNT;
            int newY = (grid.ROWS_COUNT + bodyArray.get(i).y - bodyArray.get(i - 1).y) % grid.ROWS_COUNT;

            if (newX == grid.COLS_COUNT - 1)
                newX = -1;

            if (newY == grid.ROWS_COUNT - 1)
                newY = -1;

            p.append(new Point(newX, newY));
            if (i == bodyArray.size() - 1) {
                coords.add(p);
                break;
            }

            int dx = bodyArray.get(i + 1).x - bodyArray.get(i - 1).x;
            int dy = bodyArray.get(i + 1).y - bodyArray.get(i - 1).y;

            if ((Math.abs(dx) == 1 || Math.abs(dx) == grid.COLS_COUNT - 1)
                   && (Math.abs(dy) == 1 || Math.abs(dy) == grid.ROWS_COUNT - 1)) {
                coords.addLast(p);
                p = new Point(0, 0);
            }
        }
        return coords;
    }

    private int getOnFieldX(int i) {
        return (i + grid.COLS_COUNT) % grid.COLS_COUNT;
    }

    private int getOnFieldY(int j) {
        return (j + grid.ROWS_COUNT) % grid.ROWS_COUNT;
    }

    private LinkedList<Point> getFullCoords(LinkedList<Point> skeleton) {
        LinkedList<Point> body = new LinkedList<>();
        Point prevDot = skeleton.getFirst();
        skeleton.removeFirst();

        body.addFirst(prevDot);
        for (var dot : skeleton) {
            int x = dot.x;
            int y = dot.y;
            if (x != 0 && y != 0) {
                continue;
            }
            if (x != 0) {
                if (x > 0) {
                    for(int i = prevDot.x + 1; i <= prevDot.x + x; ++i)
                        body.addLast(new Point(getOnFieldX(i),
                                               getOnFieldY(prevDot.y)));
                }
                else {
                    for(int i = prevDot.x - 1; i >= prevDot.x + x; --i)
                        body.addLast(new Point(getOnFieldX(i),
                                               getOnFieldY(prevDot.y)));
                }
            }
            else if (y != 0) {
                if (y > 0) {
                    for(int i = prevDot.y + 1; i <= prevDot.y + y; ++i)
                        body.addLast(new Point(getOnFieldX(prevDot.x),
                                               getOnFieldY(i)));
                }
                else {
                    for(int i = prevDot.y - 1; i >= prevDot.y + y; --i)
                        body.addLast(new Point(getOnFieldX(prevDot.x),
                                               getOnFieldY(i)));
                }
            }
            prevDot = body.getLast();
        }
        return body;
    }
}
