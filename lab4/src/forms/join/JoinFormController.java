package forms.join;

import forms.FormLoader;
import game.GameController;
import game.game_client.ClientModelManager;
import game.game_server.GameWriter;
import game.network.DeliverySystem;
import game.objects.Snake;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.util.Pair;
import me.ippolitov.fit.snakes.SnakesProto;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.util.HashMap;

public class JoinFormController {
    ConnectionsListener listener;

    @FXML
    private ListView<String> chooseGame;


    @FXML
    private Button prevButton;

    @FXML
    private Button startButton;

    @FXML
    private Label error;


    @FXML
    void initialize() {
        ObservableList<String> options =
                FXCollections.observableArrayList();

        HashMap<String, Pair<InetSocketAddress,
                SnakesProto.GameMessage.AnnouncementMsg>> optionsToAddress = new HashMap<>();


        chooseGame.setItems(options);

        error.setText("");

        startButton.setOnMouseClicked(event -> {
            if (listener == null) {
                try {
                    DeliverySystem.getInstance().start();

                    listener = new ConnectionsListener(options, optionsToAddress);
                    new Thread(listener).start();
                }
                catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
            }
        });

        chooseGame.setOnMouseClicked(click -> {
            if (click.getClickCount() == 2) {
                //Use ListView's getSelected Item
                synchronized (optionsToAddress) {
                    String elem = chooseGame.getSelectionModel()
                            .getSelectedItem();

                    if (elem == null)
                        return;

                    if (!optionsToAddress.containsKey(elem))
                        return;

                    if(tryJoin(optionsToAddress.get(elem).getKey())) {
                        listener.quit();
                        listener = null;

                        new GameController(optionsToAddress.get(elem).getValue().getConfig(),
                                SnakesProto.NodeRole.NORMAL);
                    }
                    options.clear();
                    optionsToAddress.clear();
                }
            }
        });

        prevButton.setOnAction(event -> {
            if (listener != null) {
                listener.quit();
                listener = null;
            }

            options.clear();
            optionsToAddress.clear();

            FormLoader.getInstance().prevScene();
            DeliverySystem.getInstance().quit();

            error.setText("");
        });
    }

    private boolean tryJoin(InetSocketAddress address) {
        if (address == null) {
            return false;
        }
        SnakesProto.GameMessage.JoinMsg join = SnakesProto.GameMessage.JoinMsg.newBuilder()
                .setOnlyView(false)
                .setName(GameController.name)
                .build();

        SnakesProto.GameMessage.Builder joinRequest = SnakesProto.GameMessage.newBuilder()
                .setJoin(join);

        /* Send request */
        DeliverySystem.getInstance().send(joinRequest, address);

        final long MAX_JOIN_TIMEOUT = 3000;
        long startTime = System.currentTimeMillis();

        /* Wait answer */
        while (true) {
            if (System.currentTimeMillis() - startTime > MAX_JOIN_TIMEOUT) {
                System.err.println("Couldn't connect to server!");
                return false;
            }

            DatagramPacket p = DeliverySystem.getInstance().getFirstReceived();
            if (p == null) {
                continue;
            }

            if (address.equals(new InetSocketAddress(p.getAddress(), p.getPort()))) {
                SnakesProto.GameMessage message = DeliverySystem.toMessage(p);
                if (message == null) {
                    return false;
                }

                if (message.hasError()) {
                    error.setText(message.getError().getErrorMessage());

                    DeliverySystem.getInstance().sendWithoutСonfirm(SnakesProto.GameMessage.newBuilder()
                            .setAck(SnakesProto.GameMessage.AckMsg.newBuilder().build())
                            .setMsgSeq(message.getMsgSeq()));
                    return false;
                }
                if (message.hasAck()) {
                    System.out.println("Start game!");
                    if (message.hasReceiverId())
                        System.out.println("MY id is" + message.getReceiverId());

                    Snake.myId = message.getReceiverId();
                    ClientModelManager.myId = message.getReceiverId();
                    ClientModelManager.serverId = message.getSenderId();
                    return true;
                }
            }
        }
    }
}
