package game.game_client;

import game.BaseModelManager;
import game.GameController;
import game.GameModel;
import game.GameReader;
import game.network.DeliverySystem;
import javafx.scene.input.KeyEvent;
import me.ippolitov.fit.snakes.SnakesProto;

import static me.ippolitov.fit.snakes.SnakesProto.Direction.UP;
import static me.ippolitov.fit.snakes.SnakesProto.Direction.DOWN;
import static me.ippolitov.fit.snakes.SnakesProto.Direction.LEFT;
import static me.ippolitov.fit.snakes.SnakesProto.Direction.RIGHT;

public class ClientModelManager extends BaseModelManager {
    public static int myId;
    public static int serverId;

    GameModel model;
    GameReader reader;

    public ClientModelManager(GameModel model, SnakesProto.GameConfig config, GameController controller) {
        this.model = model;
        this.reader = new GameReader(model, config, myId, serverId, SnakesProto.NodeRole.NORMAL, controller);

        new Thread(reader).start();
    }

    public void handle(KeyEvent event) {
        switch (event.getCode()) {
            case W:
                DeliverySystem.getInstance().send(SnakesProto.GameMessage
                        .newBuilder()
                        .setSteer(SnakesProto.GameMessage.SteerMsg.newBuilder().setDirection(UP).build())
                        .setSenderId(reader.myId)
                        .setReceiverId(reader.serverId));
                break;

            case S :
                DeliverySystem.getInstance().send(SnakesProto.GameMessage
                        .newBuilder()
                        .setSteer(SnakesProto.GameMessage.SteerMsg.newBuilder().setDirection(DOWN).build())
                        .setSenderId(reader.myId)
                        .setReceiverId(reader.serverId));
                break;

            case A :
                DeliverySystem.getInstance().send(SnakesProto.GameMessage
                        .newBuilder()
                        .setSteer(SnakesProto.GameMessage.SteerMsg.newBuilder().setDirection(LEFT).build())
                        .setSenderId(reader.myId)
                        .setReceiverId(reader.serverId));
                break;

            case D:
                DeliverySystem.getInstance().send(SnakesProto.GameMessage
                        .newBuilder()
                        .setSteer(SnakesProto.GameMessage.SteerMsg.newBuilder().setDirection(RIGHT).build())
                        .setSenderId(reader.myId)
                        .setReceiverId(reader.serverId));
                break;
        }
    }

    public void quit() {
        reader.quit();
    }
}
