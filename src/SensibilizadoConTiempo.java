public class SensibilizadoConTiempo {

    private final long alpha;
    private long timeStamp = -1;

    public SensibilizadoConTiempo(long alpha) {
        this.alpha = alpha;
    }

    /*
     * Returns true if the transition has been sensitized and the minimum time (alpha) has elapsed.
     */
    public boolean isWindowOpen() {
        return timeStamp >= 0 && (System.currentTimeMillis() - timeStamp) >= alpha;
    }

    /*
     * Returns true if the timer has not started yet or alpha has not elapsed.
     */
    public boolean isBeforeWindow() {
        return timeStamp < 0 || (System.currentTimeMillis() - timeStamp) < alpha;
    }

    /*
     * Returns the remaining milliseconds until the window opens, or 0 if already open.
     */
    public long getSleepTime() {
        if (timeStamp < 0) return 0;
        return Math.max(0, (timeStamp + alpha) - System.currentTimeMillis());
    }

    /*
     * Marks the moment this transition became sensitized, starting the alpha countdown.
     */
    public void setNuevoTimeStamp() {
        this.timeStamp = System.currentTimeMillis();
    }

    /*
     * Returns true if the timer is currently running (transition is sensitized).
     */
    public boolean isActive() {
        return timeStamp >= 0;
    }

    /*
     * Clears the timer. Called when the transition fires or loses its tokens.
     */
    public void reset() {
        this.timeStamp = -1;
    }
}