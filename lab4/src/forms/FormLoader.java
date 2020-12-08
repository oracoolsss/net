package forms;

import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.HashMap;
import java.util.Stack;

public class FormLoader {
    private static volatile FormLoader instance;
    private Stage stage = null;

    private HashMap<String, Scene> scenes;
    private Stack<Scene> windows;

    private FormLoader() {
        scenes = new HashMap<>();
        windows = new Stack<>();
    }

    public static FormLoader getInstance() {
        FormLoader localInstance = instance;
        if (localInstance == null) {
            synchronized (FormLoader.class) {
                localInstance = instance;
                if (localInstance == null) {
                    instance = localInstance = new FormLoader();
                }
            }
        }
        return localInstance;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void addScene(String fxml_name, String id, int width, int height) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxml_name));
            scenes.put(id, new Scene(root, width, height));
        }
        catch (IOException e) {
            System.err.println(e.getLocalizedMessage());
        }
    }

    public void addScene(Scene scene, String id) {
        scenes.put(id, scene);
    }

    public void showScene(String id) {
        Scene scene = scenes.get(id);
        windows.push(scene);

        computePosition(scene);
        stage.setScene(scene);
        stage.show();
    }

    public void prevScene() {
        windows.pop();

        computePosition(windows.peek());
        stage.setScene(windows.peek());
        stage.show();
    }

    public Stage getStage() {
        return stage;
    }

    private void computePosition(Scene scene) {
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        stage.setX((screenBounds.getWidth() - scene.getWidth()) / 2);
        stage.setY((screenBounds.getHeight() - scene.getHeight()) / 2);
    }
}
