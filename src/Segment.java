public class Segment implements Runnable {

    private static final int MAX_ITERATIONS = 20000;

    private int segmentId;
    private int[] transitions;

    private static int[] transitionCounters;
    private static boolean[] segmentsRunning;

    private MonitorInterface monitor;

    public Segment(int segmentId, int[] transitions, int[] transitionCounters, boolean[] segmentsRunning, MonitorInterface monitor) {
        this.segmentId = segmentId;
        this.transitions = transitions;
        Segment.transitionCounters = transitionCounters;
        Segment.segmentsRunning = segmentsRunning;
        this.monitor = monitor;
    }

    /*
     * Runs the segment, firing the assigned transitions in a loop.
     */
    @Override
    public void run() {
        while (segmentsRunning[segmentId]) {
            for (int transition : transitions) {
                boolean isFired = monitor.fireTransition(transition);
                if (isFired) {
                    updateStatus(transition);
                } else {
                    
                    // If the thread was interrupted while waiting, we consider that the segment has reached the maximum number of iterations and we stop it.
                    segmentsRunning[segmentId] = false;
                    break;
                }
            }
        }
    }

    /*
     * Updates the counter for the fired transition and checks if the segment has reached the maximum number of iterations.
     * If the counter for the fired transition reaches the maximum number of iterations, the segment is marked as not running to stop all same segments.
     * @param transition The index of the fired transition.
     */
    private synchronized void updateStatus(int transition) {

        // Update the counter for the fired transition.
        transitionCounters[transition]++;

        // Check if the counter for the fired transition has reached the maximum number of iterations, and if it has, mark the segment as not running to stop all same segments.
        if (transitionCounters[transition] >= MAX_ITERATIONS) {
            segmentsRunning[segmentId] = false;
        }
    }

    public int getSegmentId() { return segmentId; }
}
