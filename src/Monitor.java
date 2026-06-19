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

        // Initialize mutex to 1 with fairness to ensure that threads will acquire in order.
        mutex = new Semaphore(1, true); 

        int numTransitions = petriNet.getIncidenceMatrix()[0].length;
        waitingThreads = new Semaphore[numTransitions];
        waitingCount = new int[numTransitions];
        for (int i = 0; i < numTransitions; i++) {

            // Initialize each private queue to 0 to use it as a blocking point.
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

        // We are now inside the monitor, we have the lock. We will try to fire the transition.
        boolean k = true;
        while (k) {
            k = petriNet.fireTransition(transition);
            if (k) {

                // Realize the m=vs&vc operation to check if there are any enabled transitions with waiting threads, and if there are, wake up one of them based on the politic.
                boolean[] vs = petriNet.getSensitizedTransitions();
                boolean[] vc = getWaitingTransitions();
                boolean[] m = compareArrays(vs, vc);
                if (containsTrue(m)) {
                    int transitionToFire = politic.selectTransition(m);

                    // Wake up the sleeping thread by releasing its private semaphore. We do not release the main 'mutex' here. The awakened thread will inherit the lock and continue executing inside the monitor.
                    waitingThreads[transitionToFire].release();

                    // We exit the method WITHOUT releasing the main 'mutex'. The awakened thread inherits the lock automatically.
                    return true;

                // If no one to wake up, we just exit the loop.
                } else {
                    k = false;
                }
            
            // If the transition is not enabled, it goes to sleep. Increment the waiter count for this transition and release the main 'mutex' before going to sleep.
            } else {
                waitingCount[transition]++;
                mutex.release();

                try {
                    // We go to sleep on our private semaphore.
                    waitingThreads[transition].acquire();

                    // << HERE WAKES UP A SLEEPING THREAD >>

                    // Then, we decrement the waiter count for this transition and set k=true to iterate again.
                    waitingCount[transition]--;
                    k = true;

                // If the thread was interrupted while waiting, we consider that the segment has reached the maximum number of iterations and we stop it.
                } catch (InterruptedException e) {
                    //e.printStackTrace();
                    waitingCount[transition]--;
                    return false;
                }
            }
        }

        // This release is ONLY executed if the thread is leaving the monitor without waking anyone else up (when k=false).
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
