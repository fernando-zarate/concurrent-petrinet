import java.util.concurrent.Semaphore;

public class Monitor implements MonitorInterface {

    /*
     * The main mutex semaphore to guarantee mutual exclusion. 
     * Initialized to 1, meaning the monitor is free.
     */
    private Semaphore mutex;

    /*
     * An array of private semaphores. Each transition has its own semaphore initialized to 0.
     * Threads will block here (acquire) when their transition is not sensitized.
     */
    private Semaphore[] waitingThreads;

    /*
     * An array to manually keep track of how many threads are waiting in each private semaphore.
     */
    private int[] waitingCount;

    private PetriNet petriNet;
    private Politic politic;

    public Monitor(PetriNet petriNet, Politic politic) {
        this.petriNet = petriNet;
        this.politic = politic;
        // Initialize mutex to 1 (available) with fairness (true)
        mutex = new Semaphore(1, true); 
        int numTransitions = petriNet.getIncidenceMatrix()[0].length;
        waitingThreads = new Semaphore[numTransitions];
        waitingCount = new int[numTransitions];
        for (int i = 0; i < numTransitions; i++) {
            // Initialize each private queue to 0 (blocking)
            waitingThreads[i] = new Semaphore(0, true);
            waitingCount[i] = 0;
        }
    }

    @Override
    public boolean fireTransition(int transition) {
        // Try to acquire the main lock to enter the monitor
        try {
            mutex.acquire();
        } catch (InterruptedException e) {
            //e.printStackTrace();
            return false;
        }
        // Try to execute he main loop of the monitor.
        boolean k = true;
        while (k) {
            k = petriNet.fireTransition(transition);
            if (k) {
                boolean[] vs = petriNet.getSensitizedTransitions();
                boolean[] vc = getWaitingTransitions();
                boolean[] m = compareArrays(vs, vc);
                if (containsTrue(m)) {
                    int transitionToFire = politic.selectTransition(m);
                    // We wake up the sleeping thread by releasing ITS private semaphore. Passing the Baton: We do NOT release the main 'mutex' here. The awakened thread will inherit the lock and continue executing inside the monitor, without needing to acquire the 'mutex' again.
                    waitingThreads[transitionToFire].release();
                    // We exit the method WITHOUT releasing the main 'mutex'. The awakened thread inherits the lock automatically.
                    return true;
                } else {
                    // No one to wake up, we just exit the loop.
                    k = false;
                }
            } else {
                // Transition not enabled. We must go to sleep. Increment the waiter count for this transition.
                waitingCount[transition]++;
                // We release the main door so other threads can enter the monitor.
                mutex.release();
                try {
                    // We go to sleep on our private semaphore.
                    waitingThreads[transition].acquire();
                    // HERE WAKES UP THE THREAD. We decrement the waiter count for this transition.
                    waitingCount[transition]--;
                    // Loop again to try firing.
                    k = true;
                } catch (InterruptedException e) {
                    //e.printStackTrace();
                    waitingCount[transition]--;
                    return false;
                }
            }
        }
        // This release is ONLY executed if the thread is leaving the monitor without waking anyone else up (when k = false).
        mutex.release();
        return true;
    }

    private boolean[] getWaitingTransitions() {
        boolean[] output = new boolean[waitingCount.length];
        for (int i = 0; i < waitingCount.length; i++) {
            // If the count is greater than 0, there is at least one thread waiting
            output[i] = (waitingCount[i] > 0);
        }
        return output;
    }

    private boolean[] compareArrays(boolean[] array_a, boolean[] array_b) {
        boolean[] output = new boolean[array_a.length];
        // Make the list 'output' by comparing the 'array_a' and 'array_b'. If a transition is enabled to fire and has waiting threads, add 'true' to 'output', otherwise add 'false' to 'output'.
        for (int i = 0; i < array_a.length; i++) {
            output[i] = (array_a[i] && array_b[i]);
        }
        return output;
    }

    private boolean containsTrue(boolean[] array) {
        for (boolean valor : array) {
            if (valor) return true;
        }
        return false;
    }
}
