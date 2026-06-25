public class Segment implements Runnable {

    private int segmentId;
    private int[] transitions;

    private MonitorInterface monitor;

    public Segment(int segmentId, int[] transitions, MonitorInterface monitor) {
        this.segmentId = segmentId;
        this.transitions = transitions;
        this.monitor = monitor;
    }

    /*
     * Runs the segment, firing the assigned transitions in a loop.
     */
    @Override
    public void run() {
        boolean running = true;
        while (running) {
            for (int transition : transitions) {
                boolean shouldContinue = monitor.fireTransition(transition);
                if (!shouldContinue) {
                    running = false;
                    break;
                }
            }
        }
    }

    public int getSegmentId() { return segmentId; }
}
