public class Segment implements Runnable {

    private int[] transitions;
    private MonitorInterface monitor;

    public Segment(int[] transitions, MonitorInterface monitor) {
        this.transitions = transitions;
        this.monitor = monitor;
    }

    /*
     * Runs the segment, firing the assigned transitions in a loop.
     */
    @Override
    public void run() {
        while (true) {
            for (int transition : transitions) {
                boolean wasProcessed = monitor.fireTransition(transition);

                // If the transition was not processed, it means that the segment has reached the maximum number of iterations and we stop it.
                if (!wasProcessed) {
                    //System.out.printf("THREAD-%s: Transition %d not processed. Exiting segment.\n", Thread.currentThread().getName(), transition);
                    return;
                }
            }
        }
    }
}
