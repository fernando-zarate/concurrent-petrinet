import java.util.ArrayList;
import java.util.Arrays;

public class Main {

    /*
     * The maximum number of times a transition can be fired before the program ends (transition invariants).
     */
    private static final int MAX_INVARIANTS = 200;

    /*
     * Each row indicates the segments to be created.
     * The first number indicates the number of segments.
     * And the second array indicates the transitions that each segment will fire in a loop.
     */
    //                                                   qSegments    Transitions
    private static final int[][][] SEGMENTS_SETUP = { { { 3       }, { 0         } },   // Segment A (first)
                                                      { { 1       }, { 1, 2, 3   } },   // Segment B
                                                      { { 1       }, { 4, 5      } },   // Segment C
                                                      { { 1       }, { 6, 7, 8   } },   // Segment D
                                                      { { 3       }, { 9         } } }; // Segment E (last)

    public static void main(String[] args) {
        Logger logger = new Logger();
        PetriNet petriNet = new PetriNet(MAX_INVARIANTS, logger);
        //PolicyInterface policy = new PrioritizedPolicy();
        PolicyInterface policy = new RandomPolicy();
        MonitorInterface monitor = new Monitor(petriNet, policy);

        // Create the segments based on the SEGMENTS_SETUP configuration and the transitions of the petri net.
        ArrayList<Segment> segments = new ArrayList<>();
        for (int i = 0; i < SEGMENTS_SETUP.length; i++) {
            for (int j = 0; j < SEGMENTS_SETUP[i][0][0]; j++) {
                segments.add(new Segment(SEGMENTS_SETUP[i][1], monitor));
                //System.out.printf("THREAD-Main: Created segment %d for transitions %s.\n", segments.size() - 1, Arrays.toString(SEGMENTS_SETUP[i][1]));
            }
        }
        //System.out.printf("THREAD-Main: Created %d segments.\n", segments.size());

        long startTime = System.currentTimeMillis();


        // Create a thread for each segment and start all of them.
        ArrayList<Thread> threads = new ArrayList<>();
        for (Segment segment : segments) {
            Thread thread = new Thread(segment);
            threads.add(thread);
            thread.start();
            //System.out.printf("THREAD-Main: Starting thread %d/%d.\n", threads.size() - 1, segments.size() - 1);
        }
        //System.out.printf("THREAD-Main: All %d/%d threads have been started.\n", threads.size(), segments.size());

        // Wait for the last transition to complete MAX_INVARIANTS.
        int lastTransitionCounterIndex = petriNet.getTransitionCounters().length - 1;
        int lastTransitionCounter = petriNet.getTransitionCounters()[lastTransitionCounterIndex];
        while (lastTransitionCounter < MAX_INVARIANTS) {
            lastTransitionCounter = petriNet.getTransitionCounters()[lastTransitionCounterIndex];
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            //System.out.printf("THREAD-Main: Waiting for the network to be drained... (T%d: %d/%d)\n", lastTransitionCounterIndex, lastTransitionCounter, MAX_INVARIANTS);
        }
        //System.out.printf("THREAD-Main: Network drained. Interrupting threads...\n");

        // Interrupt all threads.
        for (Thread thread : threads) {
            thread.interrupt();
            //System.out.printf("THREAD-Main: Interrupting thread %s.\n", thread.getName());
        }
        //System.out.printf("THREAD-Main: All threads have been interrupted. Waiting for them to finish...\n");

        // Wait for all threads to finish.
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        //System.out.printf("THREAD-Main: All threads have finished.\n");
        
        // Print the final counters for the transitions of interest.
        int[] counters   = petriNet.getTransitionCounters();
        int creditCard   = counters[1]; // T1
        int highRisk     = counters[4]; // T4
        int bankTransfer = counters[6]; // T6

        //int t4Eligible = ((Monitor) monitor).getT4EligibleCount();

        System.out.printf("Credit/Debit:    %d%n", creditCard);
        System.out.printf("High-Risk:       %d%n", highRisk);
        System.out.printf("Bank Transfer:   %d%n", bankTransfer);
        System.out.printf("Total:           %d / %d%n", creditCard + highRisk + bankTransfer, MAX_INVARIANTS);
        //System.out.printf("T4 eligible (politica podia elegirla): %d%n", t4Eligible);
        long elapsed = System.currentTimeMillis() - startTime;
        System.out.printf("Tiempo total de ejecución: %.2f s%n", elapsed / 1000.0);
    }
}
