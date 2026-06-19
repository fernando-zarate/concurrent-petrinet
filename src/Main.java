import java.util.ArrayList;

public class Main {

    /*
     * Each row indicates the segments to be created.
     * The first number indicates the number of segments.
     * And the second array indicates the transitions that each segment will fire in a loop.
     */
    //                                             qSegments    Transitions
    private static int[][][] SEGMENTS_SETUP = { { { 2       }, { 0         } },
                                                { { 1       }, { 1, 2, 3   } },
                                                { { 1       }, { 4, 5      } },
                                                { { 1       }, { 6, 7, 8   } },
                                                { { 2       }, { 9         } } };

    public static void main(String[] args) {
        Logger logger = new Logger();
        PetriNet petriNet = new PetriNet(logger);
        Politic politic = new Politic();
        Monitor monitor = new Monitor(petriNet, politic);
        // Create the segments based on the SEGMENTS_SETUP configuration and the transitions of the petri net.
        ArrayList<Segment> segments = new ArrayList<>();
        int[] transitionCounters = new int[petriNet.getIncidenceMatrix()[0].length];
        boolean[] segmentsRunning = new boolean[SEGMENTS_SETUP.length];
        for (int i = 0; i < SEGMENTS_SETUP.length; i++) {
            segmentsRunning[i] = true;
            for (int j = 0; j < SEGMENTS_SETUP[i][0][0]; j++) {
                segments.add(new Segment(i, transitionCounters, segmentsRunning, SEGMENTS_SETUP[i][1], monitor));
                System.out.printf("THREAD-MAIN: Created segment %d for transitions %s.\n", segments.size() - 1, java.util.Arrays.toString(SEGMENTS_SETUP[i][1]));
            }
        }
        System.out.printf("THREAD-MAIN: Created %d segments.\n", segments.size());
        // Create a thread for each segment and start all of them.
        ArrayList<Thread> threads = new ArrayList<>();
        for (Segment segment : segments) {
            Thread thread = new Thread(segment);
            threads.add(thread);
            thread.start();
            System.out.printf("THREAD-MAIN: Started thread %d for segment %d.\n", threads.size() - 1, segment.getSegmentId());
        }
        System.out.printf("THREAD-MAIN: All %d threads have been started.\n", threads.size());
        // Wait for the first and last thread to finish.
        while (segmentsRunning[segmentsRunning.length - 1] == true) {
            try {
                Thread.sleep(50); // Dormimos 50ms para no consumir CPU en vano
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.printf("THREAD-MAIN: Waiting for the network to be drained...\n");
        }
        System.out.printf("THREAD-MAIN: Network drained. Interrupting sleeping threads...\n");
        // Interrupt all others threads just in case they are still running.
        for (Thread thread : threads) {
            thread.interrupt();
        }
        // Wait for all threads to finish.
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.printf("THREAD-MAIN: All threads have finished.\n");
    }
}
