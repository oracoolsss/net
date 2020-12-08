package game.game_server;

import game.BaseModelManager;
import game.GameController;
import game.GameModel;
import game.GameReader;
import game.network.DeliverySystem;
import game.objects.Snake;
import javafx.scene.input.KeyEvent;
import me.ippolitov.fit.snakes.SnakesProto;


public class ServerModelManager extends BaseModelManager {
    private GameLoop loop;
    private GameWriter writer;
    private GameReader reader;
    GameModel model;

    public ServerModelManager(GameModel model, SnakesProto.GameConfig config, GameController controller) {
        int id = GameModel.generateId();
        model.addPlayer(SnakesProto.GamePlayer.newBuilder()
                    .setId(id)
                    .setName(GameController.name)
                    .setIpAddress("")
                    .setScore(0)
                    .setPort(DeliverySystem.getInstance().myPort)
                    .setRole(SnakesProto.NodeRole.MASTER)
                    .build(), false);

        System.err.println("SERVER ID " + id);

        writer = new GameWriter(model, id);
        reader = new GameReader(model, config, id, id, SnakesProto.NodeRole.MASTER, controller);
        loop = new GameLoop(model, config, writer);

        Snake.myId = id;

        new Thread(loop).start();
        new Thread(writer).start();
        new Thread(reader).start();
    }

    public ServerModelManager(GameModel model, SnakesProto.GameConfig config, GameReader reader) {
        this.reader = reader;
        int id = reader.getMyId();

        writer = new GameWriter(model, id);
        loop = new GameLoop(model, config, writer);

        Snake.myId = id;

        new Thread(loop).start();
        new Thread(writer).start();
    }


    public void handle(KeyEvent event) {
        loop.handle(event);
    }

    public void quit() {
        System.out.println("Model quit");
        loop.quit();
        writer.quit();
        reader.quit();
    }
}
