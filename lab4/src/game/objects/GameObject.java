package game.objects;

import javafx.scene.Group;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyEvent;

import java.util.LinkedList;


public interface GameObject {
    void draw(Group graphicsContext);

    LinkedList<Point> getCoords();

    void destroy();
    boolean isAlive();
}
