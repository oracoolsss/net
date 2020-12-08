package game.objects;

public class Point {
    public int x;
    public int y;
    public Point(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public Point(Point p) {
        this.x = p.x;
        this.y = p.y;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }

        Point point = (Point)obj;
        return (x == point.x && y == point.y);
    }

    public void append(Point other) {
        x += other.x;
        y += other.y;
    }

    @Override
    public int hashCode() {
        int C = ((x + y + 1) * (x + y)) / 2 + x;
        return Integer.hashCode(C);
    }
}
