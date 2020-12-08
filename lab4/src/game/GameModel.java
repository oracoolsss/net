package game;

import game.objects.*;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.Group;
import javafx.util.Pair;
import me.ippolitov.fit.snakes.SnakesProto;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedDeque;

public class GameModel  {
    private Grid grid;
    private ConcurrentLinkedDeque<SnakesProto.GamePlayer.Builder> players;
    private ConcurrentLinkedDeque<Snake> snakes;
    private ConcurrentLinkedDeque<Apple> foods;
    private Group graphics;

    SnakesProto.GameConfig config;

    private final int ROWS_COUNT;
    private final int COLS_COUNT;

    ObservableList<String> viewList;

    int gameState = 0;

    float deathFood;

    public GameModel(SnakesProto.GameConfig config, ObservableList<String> list) {
        this.config = config;

        this.COLS_COUNT = config.getWidth();
        this.ROWS_COUNT = config.getHeight();

        this.players = new ConcurrentLinkedDeque<>();
        this.snakes = new ConcurrentLinkedDeque<>();
        this.foods = new ConcurrentLinkedDeque<>();

        this.viewList = list;

        this.deathFood = config.getDeadFoodProb();
    }

    synchronized public void setStateNumber(int state) {
        gameState = state;
    }

    synchronized public int getStateNumber() {
        return gameState;
    }

    synchronized public boolean addPlayer(SnakesProto.GamePlayer player, boolean viewer) {
        SnakesProto.GamePlayer.Builder playerBuilder = player.toBuilder();
        players.add(playerBuilder);

        if (viewer)
            return true;

        Pair<Integer, Integer> p = computeSnakeCoords();
        if (p == null)
            return false;

        snakes.add(new Snake(playerBuilder, grid, p.getKey(), p.getValue(), player.getId()));
        updateApples();
        return true;
    }

    synchronized public void removePlayer(int id) {
        snakes.forEach(snake -> {
            if (snake.getPlayer() != null && snake.getPlayer().getId() == id)
                snake.setZombie();
        });

        players.removeIf(object -> object.getId() == id);
    }

    synchronized public void setRole(int id, SnakesProto.NodeRole role) {
        players.forEach(player ->{
            if (player.getId() == id)
                player.setRole(role);
        });
    }

    synchronized public ArrayList<SnakesProto.GamePlayer.Builder> getPlayers() {
        return new ArrayList<>(players);
    }


    synchronized public ArrayList<Snake> getSnakes() {
        ArrayList<Snake> snakes = new ArrayList<>(this.snakes);
        return snakes;
    }

    synchronized public ArrayList<Apple> getObjects() {
        ArrayList<Apple> objects = new ArrayList<>(this.foods);
        return objects;
    }

    public SnakesProto.GameConfig getConfig() {
        return config;
    }

    synchronized public void initGraphics(Group graphics, int WIDTH, int HEIGHT) {
        this.graphics = graphics;

        grid = new Grid(
                new Point(0, 0),
                new Point(WIDTH, HEIGHT),
                COLS_COUNT, ROWS_COUNT);
    }

    synchronized public void render() {
        if (grid == null)
            return;
        grid.draw(graphics);

        for (Snake s : snakes)
            s.draw(graphics);

        for (Apple a : foods)
            a.draw(graphics);
    }

    public static int generateId() {
        return (int)(Math.random() * 10000);
    }

    private Pair<Integer, Integer> computeSnakeCoords() {
        byte[][] matrix = new byte[COLS_COUNT][ROWS_COUNT];
        for (Snake s : snakes) {
            for (Point p : s.getCoords()) {
                matrix[p.x][p.y] = 0x01;
            }
        }


        final int MAX_ATTEMPTS = 5;
        for (int attempt = 0; attempt < MAX_ATTEMPTS; ++attempt) {
            int x = (int)(Math.random() * COLS_COUNT);
            int y = (int)(Math.random() * ROWS_COUNT);

            boolean ok = true;
            for (int i = (COLS_COUNT + x - 2) % COLS_COUNT; i < (x + 2) % COLS_COUNT; i = (i + 1) % COLS_COUNT) {
                for (int j = (ROWS_COUNT + y - 2) % ROWS_COUNT; j < (y + 2) % ROWS_COUNT; j = (j + 1) % ROWS_COUNT) {
                    if (matrix[i][j] == 0x01)
                        ok = false;
                }
            }
            if (ok)
                return new Pair<>(x, y);
        }
        return null;
    }

    private Pair<Integer, Integer> computeAppleCoords() {
        byte[][] matrix = new byte[COLS_COUNT][ROWS_COUNT];
        for (Snake s : snakes) {
            for (Point p : s.getCoords()) {
                matrix[p.x][p.y] = 0x01;
            }
        }

        final int MAX_ATTEMPTS = 5;
        for (int attempt = 0; attempt < MAX_ATTEMPTS; ++attempt) {
            int x = (int)(Math.random() * COLS_COUNT);
            int y = (int)(Math.random() * ROWS_COUNT);
            if (matrix[x][y] == 0x00)
                return new Pair<>(x, y);
        }
        return null;
    }


    synchronized public void updateView() {
        Platform.runLater(() -> {
            LinkedList<Pair<String, Integer>> players = new LinkedList<>();

            for (Snake s : getSnakes()) {
                if (s.getPlayer() != null)
                    players.add(new Pair<>(s.getPlayer().getName(),
                            s.getPlayer().getScore()));
            }

            players.sort((lhs, rhs) -> rhs.getValue() - lhs.getValue());

            LinkedList<String> newView = new LinkedList<>();
            for (Pair<String, Integer> info : players) {
                newView.add(info.getKey() + " " + info.getValue().toString());
            }
            if (!newView.equals(viewList)) {
                viewList.clear();
                viewList.addAll(newView);
            }
        });
    }


    synchronized void setFromState(SnakesProto.GameState state) {
        players.clear();

        for (SnakesProto.GamePlayer p : state.getPlayers().getPlayersList())
            players.add(p.toBuilder());

        snakes.clear();
        for (SnakesProto.GameState.Snake s : state.getSnakesList()) {
            LinkedList<Point> body = new LinkedList<>();
            for (SnakesProto.GameState.Coord c : s.getPointsList())
                body.add(new Point(c.getX(), c.getY()));

            boolean hasPlayer = false;
            for (SnakesProto.GamePlayer.Builder player : players) {
                /* Find player for snake */
                if (player.getId() == s.getPlayerId()) {
                    snakes.add(new Snake(player, grid, body, s.getHeadDirection(), s.getPlayerId()));
                    hasPlayer = true;
                    break;
                }
            }
            if (!hasPlayer) {
                /* If snake don't have player */
                Snake snake = new Snake(null, grid, body, s.getHeadDirection(), s.getPlayerId());
                snake.setZombie();
                snakes.add(snake);
            }
        }

        foods.clear();
        for (SnakesProto.GameState.Coord c : state.getFoodsList()) {
            foods.add(new Apple(grid, c.getX(), c.getY()));
        }

        gameState = state.getStateOrder();
    }


    synchronized public void updateApples() {
        int snakes = getSnakes().size();
        int max_food = (int)(snakes * config.getFoodPerPlayer()) + config.getFoodStatic();

        for (int d = getObjects().size(); d < max_food; ++d) {
            Pair<Integer, Integer> coord = computeAppleCoords();
            if (coord != null)
                foods.add(new Apple(grid, coord.getKey(), coord.getValue()));
        }

    }


    synchronized public LinkedList<SnakesProto.GamePlayer.Builder> cleanUp() {
        foods.removeIf(object -> !object.isAlive());

        LinkedList<SnakesProto.GamePlayer.Builder> deadPlayers = new LinkedList<>();

        for (Snake s : snakes) {
            if (!s.isAlive()) {
                if (s.getPlayer() != null)
                    deadPlayers.add(s.getPlayer());
                snakes.remove(s);
                for (Point p : s.getCoords()) {
                    if (Math.random() < deathFood)
                        foods.add(new Apple(grid, p.x, p.y));
                }
            }
        }
        return deadPlayers;
    }
}
