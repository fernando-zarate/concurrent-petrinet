public class Segment implements Runnable {

    private static final int MAX_ITERATIONS = 200;

    private int segmentId;

    private static int[] transitionCounters;
    private static boolean[] segmentsRunning;

    private MonitorInterface monitor;
    private int[] transitions;

    public Segment(int segmentId, int[] transitionCounters, boolean[] segmentsRunning, int[] transitions, MonitorInterface monitor) {
        this.segmentId = segmentId;
        this.monitor = monitor;
        this.transitions = transitions;
        Segment.transitionCounters = transitionCounters;
        Segment.segmentsRunning = segmentsRunning;
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
                    boolean stop = updateStatus(transition);
                    if (stop) {
                        break;
                    }
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
     * If the current segment is running normaly but there are some different segment stopped, check if the last segment has reached the maximum number of iterations,
     * and in that case, stop the current segment as well because it means that the tokens have been drained from the net.
     * @param transition The index of the fired transition.
     * @return 'true' if the segment has been stopped, 'false' otherwise.
     */
    private synchronized boolean updateStatus(int transition) {
        // Update the counter for the fired transition.
        transitionCounters[transition]++;
        // Check if the counter for the fired transition has reached the maximum number of iterations, and if it has, mark the segment as not running to stop all same segments.
        if (transitionCounters[transition] >= MAX_ITERATIONS) {
            segmentsRunning[segmentId] = false;
            return true;
        }
        // If the current segment is running normaly but the last segment has stopped, then stop the current segment as well because it means that the tokens have been drained from the net.
        if (segmentsRunning[segmentId]) {
            if (!segmentsRunning[segmentsRunning.length - 1]) {
                segmentsRunning[segmentId] = false;
                return true;
            }
        }
        return false;
    }

    public int getSegmentId() { return segmentId; }
}
