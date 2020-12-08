package game.game_server;

import game.GameModel;
import game.objects.*;
import javafx.scene.input.KeyEvent;
import me.ippolitov.fit.snakes.SnakesProto;
import system.Timer;
import java.util.ArrayList;
import java.util.LinkedList;

public class GameLoop implements Runnable {
    GameWriter writer;
    GameModel model;
    Timer timer;

    Timer fpsCounter;

    boolean running = true;

    GameLoop(GameModel model, SnakesProto.GameConfig config, GameWriter writer) {
        this.writer = writer;
        this.model = model;
        this.timer = new Timer(config.getStateDelayMs());

        this.fpsCounter = new Timer((int)(1000.0 / 60));
    }


    void handle(KeyEvent event) {
        for (Snake s : model.getSnakes()) {
            if (s.getPlayer() != null && s.getPlayer().getId() == writer.myId)
                s.handle(event);
        }
    }


    private void update() {
        cleanUp();

        ArrayList<Snake> snakes = model.getSnakes();
        ArrayList<Apple> apples = model.getObjects();

        for (Snake snake : snakes) {
            if (!snake.update()) {
                if (snake.getPlayer() != null && snake.getPlayer().getRole() != SnakesProto.NodeRole.MASTER) {
                    writer.sendError(SnakesProto.GameMessage.ErrorMsg.newBuilder()
                            .setErrorMessage("Wrong direction!")
                            .build(), snake.getPlayer().getId());
                }
            }
        }

        for (Snake snake : snakes) {
            for (Apple apple : apples)
                snake.checkCollide(apple);
        }

        for (int i = 0; i < snakes.size() - 1; ++i) {
            for (int j = i + 1; j < snakes.size(); ++j) {
                snakes.get(i).checkCollide(snakes.get(j));
            }
        }
        writer.sendState(getState());
    }

    private void render() {
        model.render();
        model.updateView();
    }

    @Override
    public void run() {
        while (running) {
            if (timer.accept()) {
                update();

                /* Fps locker */
                if (fpsCounter.accept())
                    render();

                writer.sendState(getState());
            }
        }
        System.out.println("LOOP QUIT");
    }

    void quit() {
        running = false;
    }

    private void cleanUp() {
        LinkedList<SnakesProto.GamePlayer.Builder> deadPlayers = model.cleanUp();
        deadPlayers.forEach(player -> writer.sendDeathMessage(player.getId()));
        model.updateApples();
    }


    private ArrayList<SnakesProto.GameState.Coord> getBody(Snake snake) {
        ArrayList<SnakesProto.GameState.Coord> body = new ArrayList<>();
        for (Point p : snake.getCoordsOptimized()) {
            body.add(SnakesProto.GameState.Coord.newBuilder()
                    .setX(p.x)
                    .setY(p.y)
                    .build());
        }
        return body;
    }


    private ArrayList<SnakesProto.GameState.Snake> getSnakes() {
        ArrayList<SnakesProto.GameState.Snake> snakes = new ArrayList<>();

        for (Snake s : model.getSnakes()) {
            snakes.add(SnakesProto.GameState.Snake.newBuilder()
                    .setPlayerId(s.getId())
                    .addAllPoints(getBody(s))
                    .setHeadDirection(s.getDirection())
                    .setState(SnakesProto.GameState.Snake.SnakeState.ALIVE)
                    .build());
        }
        return snakes;
    }


    public SnakesProto.GameState getState() {
        /* Foods */
        ArrayList<SnakesProto.GameState.Coord> foods = new ArrayList<>();
        for (Apple a : model.getObjects()) {
            Point p = a.getPosition();
            foods.add(SnakesProto.GameState.Coord.newBuilder()
                    .setX(p.x)
                    .setY(p.y)
                    .build());
        }

        /* Players */
        LinkedList<SnakesProto.GamePlayer> playerList = new LinkedList<>();
        for ( SnakesProto.GamePlayer.Builder p : model.getPlayers())
            playerList.add(p.build());

        SnakesProto.GamePlayers players = SnakesProto.GamePlayers.newBuilder()
                .addAllPlayers(playerList)
                .build();

        int stateNumber = model.getStateNumber();
        SnakesProto.GameState state = SnakesProto.GameState.newBuilder()
                .addAllFoods(foods)
                .addAllSnakes(getSnakes())
                .setPlayers(players)
                .setConfig(model.getConfig())
                .setStateOrder(stateNumber)
                .build();

        model.setStateNumber(++stateNumber);
        return state;
    }
}
