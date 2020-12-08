package system;

public class Timer {
    private long ticks = 0;
    private long milliseconds = 0;
    private long timeout;

    private boolean initialized = false;

    private void initialize() {
        ticks = System.currentTimeMillis();
        initialized = true;
    }

    public Timer(long timeout) {
        this.timeout = timeout;
    }


    public boolean accept() {
        if (!initialized) {
            initialize();
            return true;
        }
        long dt = System.currentTimeMillis() - ticks;
        ticks = System.currentTimeMillis();
        milliseconds += dt;

        if (milliseconds < timeout)
            return false;

        milliseconds -= timeout;
        return true;
    }
}
