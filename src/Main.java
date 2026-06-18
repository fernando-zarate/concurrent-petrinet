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
        PetriNet petriNet = new PetriNet();
        Politic politic = new Politic();
        Monitor monitor = new Monitor(petriNet, politic);
        // Create the segments based on the SEGMENTS_SETUP configuration.
        ArrayList<Segment> segments = new ArrayList<>();
        for (int i = 0; i < SEGMENTS_SETUP.length; i++) {
            for (int j = 0; j < SEGMENTS_SETUP[i][0][0]; j++) {
                segments.add(new Segment(monitor, SEGMENTS_SETUP[i][1], logger));
            }
        }
        // Create a thread for each segment and start all of them.
        ArrayList<Thread> threads = new ArrayList<>();
        for (Segment segment : segments) {
            Thread thread = new Thread(segment);
            threads.add(thread);
            thread.start();
        }
        System.out.printf("All threads have been started.\n");
        // Wait for all threads to finish.
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.printf("All threads have finished.\n");
    }
}
