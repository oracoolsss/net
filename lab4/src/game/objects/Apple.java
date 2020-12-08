package game.objects;

import javafx.scene.Group;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

import java.util.LinkedList;

public class Apple implements GameObject {
    private Grid grid;
    private final Point point;
    private boolean isAlive = true;
    //private final Image image;

    public Apple(Grid grid, int i, int j) {
        //image = new Image("textures/apple.png");
        this.grid = grid;

        point = new Point(i, j);
    }

    @Override
    synchronized public void draw(Group graphics) {
        grid.drawCell(graphics, point.x, point.y, Color.RED);
    }


    @Override
    synchronized public void destroy() {
        isAlive = false;
    }

    @Override
    synchronized public boolean isAlive() {
        return isAlive;
    }

    @Override
    synchronized public LinkedList<Point> getCoords() {
        LinkedList<Point> list = new LinkedList<>();
        list.add(point);
        return list;
    }

    synchronized public Point getPosition() {
        return point;
    }
}
