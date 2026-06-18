import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class Monitor implements MonitorInterface {

    /*
     * A reentrant lock for synchronizing access to the monitor.
     * This is used to ensure that only one thread can access the monitor at a time, and to avoid race conditions when multiple threads are trying to fire transitions or wait for transitions to fire.
     */
    private ReentrantLock monitorLock;

    /*
     * An array of conditions for each transition in the petri net, where each condition is used to manage the threads that are waiting for that transition to fire.
     * Class monitor not knows how many transitions there are, so it can be any number of transitions, and each transition can have any number of threads waiting for it.
     * When a Thread ask to fire a transition, if the transition is not enabled, the thread is added in the waitingThreads list and is set to sleep.
     * 
     * e.g.:
     * [Transition_0 = [Thread_0, Thread_1, ..., Thread_n]],
     * [Transition_1 = [Thread_2, Thread_3, ..., Thread_m]],
     * ...
     * [Transition_k = [Thread_p, Thread_q, ..., Thread_r]]
     */
    private Condition[] waitingThreads;

    private PetriNet petriNet;
    private Politic politic;

    public Monitor(PetriNet petriNet, Politic politic) {
        this.petriNet = petriNet;
        this.politic = politic;
        monitorLock = new ReentrantLock(true);
        // Initialize the waitingThreads array with the same number of elements as the number of transitions in the petri net.
        waitingThreads = new Condition[petriNet.getIncidenceMatrix()[0].length];
        for (int i = 0; i < waitingThreads.length; i++) {
            waitingThreads[i] = monitorLock.newCondition();
        }
    }

    @Override
    public boolean fireTransition(int transition) {
        // Acquire the lock to ensure exclusive access to the monitor. Others threads will be blocked until the lock is released.
        monitorLock.lock();
        try {
            boolean k = true;
            while (k) {
                k = petriNet.fireTransition(transition);
                // If the transition was fired succesfully, check if there are threads waiting for other transitions that are now enabled to fire, and fire them.
                if (k) {
                    boolean[] vs = petriNet.getSensitizedTransitions(); // 'vs' is the array of transitions that are now enabled to fire after firing the transition. e.g.: vs = [T0=1, T1=0, T2=1, ..., Tk=x]
                    boolean[] vc = getWaitingTransitions(); // 'vc' is the array of transitions that have waiting threads. e.g.: vc = [T0=1, T1=0, T2=0, ..., Tk=x]
                    boolean[] m = compareArrays(vs, vc); // 'm' is the array of transitions that are now enabled to fire and have waiting threads. e.g.: m = [T0=1, T1=0, T2=0, ..., Tk=x]
                    // Check if there are any transitions that are now enabled to fire and have waiting threads that can fire it.
                    if (m[0] || m[1] || m[2] || m[3] || m[4] || m[5] || m[6] || m[7] || m[8] || m[9]) {
                        // Wake up one of the threads waiting for the transition that is now enabled to fire, based on the politic, and set it to run.
                        int transitionToFire = politic.selectTransition(m);
                        waitingThreads[transitionToFire].signal();
                        k = false;
                    } else {
                        // If there are no transitions that are now enabled to fire and have waiting threads, exit the loop and return true, since the original transition was fired successfully.
                        k = false;
                    }
                } else {
                    // If the transition is not enabled, add the current thread to the waitingThreads list for that transition and set it to sleep until the transition is enabled and can be fired.
                    waitingThreads[transition].await();
                    // After the thread is woken up, it will try to fire the transition again, and if it is still not enabled, it will go back to sleep until it is woken up again.
                    k = true;
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            // The lock will be released in the finally block to ensure that it is released even if an exception occurs, to avoid deadlocks and allow other threads to access the monitor.
            monitorLock.unlock();
        }
        return true;
    }

    private boolean[] getWaitingTransitions() {
        boolean[] output = new boolean[waitingThreads.length];
        // For each transition in the petri net, check if there are threads waiting for that transition to fire, and if there are, add 'true' to 'output', otherwise add 'false' to 'output'.
        for (int i = 0; i < waitingThreads.length; i++) {
            output[i] = monitorLock.hasWaiters(waitingThreads[i]);
        }
        return output;
    }

    private boolean[] compareArrays(boolean[] array_a, boolean[] array_b) {
        boolean[] output = new boolean[array_a.length];
        // Make the list 'output' by comparing the 'array_a' and 'array_b'. If a transition is enabled to fire and has waiting threads, add 'true' to 'output', otherwise add 'false' to 'output'.
        for (int i = 0; i < array_a.length; i++) {
            if (array_a[i] == true && array_b[i] == true) {
                output[i] = true;
            } else {
                output[i] = false;
            }
        }
        return output;
    }
}
