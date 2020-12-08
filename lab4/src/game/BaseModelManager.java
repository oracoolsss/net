package game;

import javafx.scene.input.KeyEvent;

public abstract class BaseModelManager {
    abstract public void handle(KeyEvent event);
    abstract public void quit();
}
