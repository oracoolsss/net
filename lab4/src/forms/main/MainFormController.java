package forms.main;

import forms.FormLoader;
import forms.join.ConnectionsListener;
import game.GameController;
import game.network.DeliverySystem;
import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class MainFormController extends Application {
    public static Stage stage;

    public static final int WIDTH = 700;
    public static final int HEIGHT = 450;

    @FXML
    private Button createButton;

    @FXML
    private Button joinButton;

    @FXML
    private TextField nickname;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setResizable(false);

        stage = primaryStage;
        stage.setOnCloseRequest(e -> {
            if (ConnectionsListener.multicast != null)
                ConnectionsListener.multicast.close();

            DeliverySystem.getInstance().quit();
        });

        FormLoader.getInstance().setStage(primaryStage);

        FormLoader.getInstance().addScene("main/mainform.fxml",     "mainform",   WIDTH, HEIGHT);
        FormLoader.getInstance().addScene("create/createform.fxml", "createform", WIDTH, HEIGHT);
        FormLoader.getInstance().addScene("join/joinform.fxml",     "joinform",   WIDTH, HEIGHT);

        FormLoader.getInstance().showScene("mainform");
    }


    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Wrong args count");
            return;
        }

        int port = Integer.parseInt(args[0]);
        DeliverySystem.getInstance().setPort(port);

        launch(args);
    }

    @FXML
    void initialize() {
        createButton.setOnAction(event -> {
            GameController.name = nickname.getText();
            FormLoader.getInstance().showScene("createform");
        });

        joinButton.setOnAction(event -> {
            GameController.name = nickname.getText();
            FormLoader.getInstance().showScene("joinform");
        });
    }
}
