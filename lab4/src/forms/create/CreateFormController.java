package forms.create;

import forms.FormLoader;
import game.GameController;
import game.network.DeliverySystem;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

import javafx.fxml.FXML;
import javafx.util.Pair;
import me.ippolitov.fit.snakes.SnakesProto;

import java.io.IOException;
import java.util.HashMap;


public class CreateFormController {

    HashMap<String, Pair<Integer, Integer>> sizes;

    @FXML
    private ComboBox<String> fieldSize;

    @FXML
    private TextField foodCount;

    @FXML
    private Button nextButton;

    @FXML
    private Button prevButton;

    @FXML
    void initialize() {
        sizes = new HashMap<>();
        sizes.put("50x50", new Pair<>(50, 50));
        sizes.put("75x75", new Pair<>(75, 75));
        sizes.put("100x100", new Pair<>(100, 100));

        ObservableList<String> options =
                FXCollections.observableArrayList();
        for (HashMap.Entry entry : sizes.entrySet())
            options.add((String)entry.getKey());

        fieldSize.setItems(options);


        prevButton.setOnAction(event -> {
            FormLoader.getInstance().prevScene();
        });

        nextButton.setOnAction(event -> {
            Pair<Integer, Integer> size = sizes.get(fieldSize.getValue());
            int ifoodCount;
            try {
                ifoodCount = Integer.parseInt(foodCount.getText());

                SnakesProto.GameConfig config =
                        SnakesProto.GameConfig.newBuilder()
                                .setWidth(size.getKey())
                                .setHeight(size.getValue())
                                .setPingDelayMs(50)
                                .setStateDelayMs(25)
                                .setDeadFoodProb(0.4f)
                                .setFoodPerPlayer(5.0f)
                                .setFoodStatic(ifoodCount)
                                .setNodeTimeoutMs(200)
                                .build();

                DeliverySystem.getInstance().start();
                new GameController(config, SnakesProto.NodeRole.MASTER);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        });


    }
}
