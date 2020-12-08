package game;

import forms.FormLoader;
import game.game_client.ClientModelManager;
import game.game_server.ServerModelManager;
import game.network.DeliverySystem;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Box;
import javafx.stage.Screen;
import javafx.stage.Stage;
import me.ippolitov.fit.snakes.SnakesProto;


public class GameController implements EventHandler<KeyEvent> {
    private GameModel gameModel;
    private BaseModelManager manager;

    private Button exit_button;

    public static String name;

    private Stage primaryStage;
    private Group root;
    private Scene scene;
    private Box keyboardNode;

    ListView<String> playerList;

    SnakesProto.GameConfig config;

    public GameController(SnakesProto.GameConfig config,
                          SnakesProto.NodeRole role) {
        this.config = config;

        final int COLS = config.getWidth();
        final int ROWS = config.getHeight();

        ObservableList<String> options =
                FXCollections.observableArrayList();
        gameModel = new GameModel(config, options);

        final Stage stage = initGame(COLS, ROWS);
        playerList.setItems(options);

        stage.setOnCloseRequest(e -> {
            manager.quit();
            DeliverySystem.getInstance().quit();
        });

        /* Set up exit by button */
        exit_button.setOnAction(e -> {
            manager.quit();

            /* Pause delivery system */
            DeliverySystem.getInstance().quit();
            FormLoader.getInstance().prevScene();
        });

        if (role == SnakesProto.NodeRole.MASTER) {
            manager = new ServerModelManager(gameModel, config, this);
        }
        else {
            manager = new ClientModelManager(gameModel, config, this);
        }
    }

    public void changeToServer(GameReader reader) {
        manager = new ServerModelManager(
                gameModel, config, reader);
    }


    @Override
    public void handle(KeyEvent event) {
        manager.handle(event);
    }


    private Stage initGame(int COLS, int ROWS) {
        /* Compute window size */
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();

        final double MAX_WINDOW_WIDTH = screenBounds.getWidth() * 0.6;
        final double MAX_WINDOW_HEIGHT = screenBounds.getHeight() * 0.6;

        final double INFO_BAR_WIDTH = MAX_WINDOW_WIDTH * 0.15;
        final double BUTTON_BAR_HEIGHT = MAX_WINDOW_HEIGHT * 0.0625;

        final double RATIO;

        double r1 = (MAX_WINDOW_WIDTH  - INFO_BAR_WIDTH)
                / (MAX_WINDOW_HEIGHT - BUTTON_BAR_HEIGHT);

        double r2 = (double)(COLS) / ROWS;

        if (r1 < r2)
            RATIO = (MAX_WINDOW_WIDTH - INFO_BAR_WIDTH) / COLS;
        else
            RATIO = (MAX_WINDOW_HEIGHT - BUTTON_BAR_HEIGHT) / ROWS;

        final double WIDTH = COLS * RATIO + INFO_BAR_WIDTH;
        final double HEIGHT = ROWS * RATIO + BUTTON_BAR_HEIGHT;
        final double GRID_WIDTH = RATIO * COLS;
        final double GRID_HEIGHT = RATIO * ROWS;

        /* Set up stage */
        primaryStage = FormLoader.getInstance().getStage();
        root = new Group();
        scene = new Scene(root, WIDTH, HEIGHT, Color.BLACK);

        Group graphics = new Group();
        root.getChildren().add(graphics);

        keyboardNode = new Box();
        keyboardNode.setFocusTraversable(true);
        keyboardNode.requestFocus();
        keyboardNode.setOnKeyPressed(this);
        root.getChildren().add(keyboardNode);

        /* Button */
        final double SPACE = 2;
        exit_button = new Button("EXIT");

        final double BUTTON_HEIGHT = HEIGHT - GRID_HEIGHT - 2 * SPACE;
        final double BUTTON_WIDTH = BUTTON_HEIGHT * 3;
        exit_button.setMinWidth(BUTTON_WIDTH);
        exit_button.setMinHeight(BUTTON_HEIGHT);

        exit_button.setLayoutX(SPACE);
        exit_button.setLayoutY(RATIO * ROWS + SPACE);
        root.getChildren().add(exit_button);

        playerList = new ListView<>();
        playerList.setLayoutX(GRID_WIDTH + SPACE);
        playerList.setLayoutY(SPACE);
        playerList.setMinWidth(WIDTH - GRID_WIDTH - 2 * SPACE);
        playerList.setMaxWidth(WIDTH - GRID_WIDTH - 2 * SPACE);
        playerList.setMinHeight(GRID_HEIGHT - 2 * SPACE);
        playerList.setMaxHeight(GRID_HEIGHT - 2 * SPACE);
        root.getChildren().add(playerList);

        gameModel.initGraphics(graphics, (int)(GRID_WIDTH), (int)(GRID_HEIGHT));

        FormLoader.getInstance().addScene(scene, "game");
        FormLoader.getInstance().showScene("game");
        return primaryStage;
    }
}
