package game.objects;

import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class Grid {
    final int COLS_COUNT;
    final int ROWS_COUNT;

    private float stepX;
    private float stepY;

    private Point left_upper;
    private Point right_lower;

    public Grid(Point left_upper, Point right_lower, int cols, int rows) {
        COLS_COUNT = cols;
        ROWS_COUNT = rows;

        this.left_upper = left_upper;
        this.right_lower = right_lower;

        stepX = ((float)(right_lower.x - left_upper.x)) / COLS_COUNT;
        stepY = ((float)(right_lower.y - left_upper.y)) / ROWS_COUNT;
    }

    public void draw(Group graphicsContext) {
        Platform.runLater(() -> {
            graphicsContext.getChildren().clear();
            Rectangle rect = new Rectangle(left_upper.x, left_upper.y,
                    stepX * COLS_COUNT, stepY * ROWS_COUNT);

            rect.setFill(Color.GREY);
            rect.setStroke(Color.BLACK);

            graphicsContext.getChildren().add(rect);
        });
    }

    public void drawCell(Group graphicsContext, int i, int j, Color color) {
        Platform.runLater(() -> {
            float x = stepX * i + left_upper.x;
            float y = stepY * j + left_upper.y;
            var rect = new Rectangle(x, y, stepX, stepY);
            rect.setFill(color);
            rect.setStroke(color);
            graphicsContext.getChildren().add(rect);
        });
    }
}

