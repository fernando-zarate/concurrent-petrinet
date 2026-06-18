public class Segment implements Runnable {

    private MonitorInterface monitor;
    private int[] transitions;
    private Logger logger;

    public Segment(MonitorInterface monitor, int[] transitions, Logger logger) {
        this.monitor = monitor;
        this.transitions = transitions;
        this.logger = logger;
    }

    /*
     * Runs the segment, firing the assigned transitions in a loop.
     */
    @Override
    public void run() {
        while (true) {
            for (int transition : transitions) {
                boolean isFired = monitor.fireTransition(transition);
                if (isFired) {
                    logger.logTransitionFiring(transition);
                }
            }
        }
    }
}
