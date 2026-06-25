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
    private PolicyInterface policy;
    private boolean waitingThreadsReleased;

    public Monitor(PetriNet petriNet, PolicyInterface policy) {
        this.petriNet = petriNet;
        this.policy = policy;
        this.waitingThreadsReleased = false;

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
        boolean mutexAcquired = false;
        boolean registeredAsWaiting = false;
        try {
            mutex.acquire();
            mutexAcquired = true;

            while (true) {
                if (petriNet.isFinished()) {
                    finishExecution();
                    return false;
                }

                boolean fired = petriNet.fireTransition(transition);
                if (fired) {
                    if (petriNet.isFinished()) {
                        finishExecution();
                        return false;
                    }

                    wakeNextSensitizedWaitingThread();
                    return true;
                }

                waitingCount[transition]++;
                registeredAsWaiting = true;
                wakeNextSensitizedWaitingThread();
                mutex.release();
                mutexAcquired = false;

                waitingThreads[transition].acquire();
                registeredAsWaiting = false;

                mutex.acquire();
                mutexAcquired = true;
            }
        } catch (InterruptedException e) {
            if (registeredAsWaiting) {
                removeInterruptedWaiter(transition);
            }
            return false;
        } finally {
            if (mutexAcquired) {
                mutex.release();
            }
        }
    }

    private void finishExecution() {
        if (!waitingThreadsReleased) {
            System.out.printf("THREAD-MONITOR: Completed invariants: %d\n", petriNet.getCompletedInvariants());
        }
        releaseAllWaitingThreads();
    }

    private void releaseAllWaitingThreads() {
        if (waitingThreadsReleased) {
            return;
        }

        waitingThreadsReleased = true;
        for (int i = 0; i < waitingThreads.length; i++) {
            if (waitingCount[i] > 0) {
                int threadsToRelease = waitingCount[i];
                waitingCount[i] = 0;
                waitingThreads[i].release(threadsToRelease);
            }
        }
    }

    private void removeInterruptedWaiter(int transition) {
        boolean acquired = false;
        try {
            mutex.acquire();
            acquired = true;
            if (waitingCount[transition] > 0) {
                waitingCount[transition]--;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            if (acquired) {
                mutex.release();
            }
        }
    }

    private void wakeNextSensitizedWaitingThread() {
        boolean[] vs = petriNet.getSensitizedTransitions();
        boolean[] vc = getWaitingTransitions();
        boolean[] m = compareArrays(vs, vc);
        if (containsTrue(m)) {
            int transitionToFire = policy.selectTransition(m);
            waitingCount[transitionToFire]--;
            waitingThreads[transitionToFire].release();
        }
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
