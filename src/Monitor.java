import java.util.concurrent.Semaphore;

public class Monitor implements MonitorInterface {

    /*
     * The main mutex semaphore to guarantee mutual exclusion. 
     * Initialized to 1, meaning the monitor is free and with fairness to ensure that threads will acquire in order.
     */
    private final Semaphore mutex = new Semaphore(1, true);

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
    private PolicyInterface policy;

    public Monitor(PetriNet petriNet, PolicyInterface policy) {
        this.petriNet = petriNet;
        this.policy = policy;
        int numTransitions = petriNet.getIncidenceMatrix()[0].length;
        waitingThreads = new Semaphore[numTransitions];
        waitingCount = new int[numTransitions];

        // Initialize each private queue to 0 to use it as a blocking point.
        for (int i = 0; i < numTransitions; i++) {
            waitingThreads[i] = new Semaphore(0, true);
            waitingCount[i] = 0;
        }
    }

    @Override
    public boolean fireTransition(int transition) {

        // Try to acquire the main lock to enter the monitor.
        try {
            mutex.acquire();
        } catch (InterruptedException e) {
            //e.printStackTrace();
            return false;
        }

        // We are now inside the monitor, we have the lock. We will try to fire the transition.
        boolean k = true;
        while (k) {
            int status = petriNet.fireTransition(transition);
            if (status == 0) {

                // Realize the m=vs&vc operation to check if there are any enabled transitions with waiting threads, and if there are, wake up one of them based on the policy.
                boolean[] vs = petriNet.getSensitizedTransitionsByMarking();
                boolean[] vc = getWaitingTransitions();
                boolean[] m = compareArrays(vs, vc);
                if (containsTrue(m)) {
                    int transitionToFire = policy.selectTransition(m);
                    waitingThreads[transitionToFire].release();
                    return true;

                // If there are no threads to wake up, we just exit the loop.
                } else {
                    k = false;
                }
            
            // If the transition is not enabled, it goes to sleep. Increment the waiter count for this transition and release the main 'mutex' before going to sleep.
            } else if (status == -1) {
                waitingCount[transition]++;
                mutex.release();

                // We go to sleep on our private semaphore. Waiting to be woken up by another thread.
                try {
                    waitingThreads[transition].acquire();
                    
                    // HERE WAKES UP A SLEEPING THREAD. We decrement the waiter count for this transition and set k=true to iterate again.
                    waitingCount[transition]--;
                    k = true;

                // If the thread was interrupted while waiting, we consider that the segment has reached the maximum number of iterations and we stop it.
                } catch (InterruptedException e) {
                    //e.printStackTrace();
                    waitingCount[transition]--;
                    return false;
                }
            } else if (status > 0) {
                mutex.release();
                try {
                    Thread.sleep(status);
                    mutex.acquire();
                    k = true;
                } catch (InterruptedException e) {
                    //e.printStackTrace();
                    return false;
                }
            }
        }

        // This release is ONLY executed if the thread is leaving the monitor without waking anyone else up (when k=false).
        mutex.release();
        return true;
    }

    /*
     * Returns an array of booleans indicating which transitions have waiting threads.
     */
    private boolean[] getWaitingTransitions() {
        boolean[] output = new boolean[waitingCount.length];
        for (int i = 0; i < waitingCount.length; i++) {
            output[i] = (waitingCount[i] > 0);
        }
        return output;
    }

    /*
     * Compares two arrays of booleans and returns a new array with the result of the comparison.
     */
    private boolean[] compareArrays(boolean[] array_a, boolean[] array_b) {
        boolean[] output = new boolean[array_a.length];
        for (int i = 0; i < array_a.length; i++) {
            output[i] = (array_a[i] && array_b[i]);
        }
        return output;
    }

    //public int getT4EligibleCount() { return t4EligibleCount; }

    /*
     * Checks if an array of booleans contains at least one true value.
     */
    private boolean containsTrue(boolean[] array) {
        for (boolean valor : array) {
            if (valor) return true;
        }
        return false;
    }
}
