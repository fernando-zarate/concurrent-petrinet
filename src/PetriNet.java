import java.util.concurrent.atomic.AtomicIntegerArray;

public class PetriNet {

    //                                        P0  P1  P2  P3  P4  P5  P6  P7  P8  P9
    private final int[] marking = new int[] {  3,  0,  0,  0,  0,  0,  0,  1,  1,  0 };

    //                                                      T0  T1  T2  T3  T4  T5  T6  T7  T8  T9
    private final int[][] incidenceMatrix = new int[][] { { -1,  0,  0,  0,  0,  0,  0,  0,  0,  1 },   // P0
                                                          {  1, -1,  0,  0, -1,  0, -1,  0,  0,  0 },   // P1
                                                          {  0,  1, -1,  0,  0,  0,  0,  0,  0,  0 },   // P2
                                                          {  0,  0,  1, -1,  0,  0,  0,  0,  0,  0 },   // P3
                                                          {  0,  0,  0,  0,  1, -1,  0,  0,  0,  0 },   // P4
                                                          {  0,  0,  0,  0,  0,  0,  1, -1,  0,  0 },   // P5
                                                          {  0,  0,  0,  0,  0,  0,  0,  1, -1,  0 },   // P6
                                                          {  0, -1,  0,  1, -1,  1,  0,  0,  0,  0 },   // P7
                                                          {  0,  0,  0,  0, -1,  1, -1,  0,  1,  0 },   // P8
                                                          {  0,  0,  0,  1,  0,  1,  0,  0,  1, -1 } }; // P9

    /*
     * Timed transitions with their alpha values.
     * T2: waiting for card authorization response from issuer.
     * T3: capturing funds from issuer.
     * T5: simultaneous fraud scoring and gateway authorization.
     * T7: validating destination bank account.
     * T8: executing the bank transfer in the network.
     * null = non-timed transition (can fire immediately when sensitized).
     */
    //                                                          T0     T1     T2                                       T3                                    T4     T5                                      T6     T7                                   T8                                      T9
    private final SensibilizadoConTiempo[] timedTransitions = { null,  null,  new SensibilizadoConTiempo(150), new SensibilizadoConTiempo(150), null,  new SensibilizadoConTiempo(300), null,  new SensibilizadoConTiempo(150), new SensibilizadoConTiempo(150), null };

    private final int maxInvariants;
    private final AtomicIntegerArray transitionCounters = new AtomicIntegerArray(10);
    private final Logger logger;

    public PetriNet(int maxInvariants, Logger logger) {
        this.maxInvariants = maxInvariants;
        this.logger = logger;
    }

    public boolean fireTransition(int transition) {

        // Check if the transition counter has reached the maximum number of invariants.
        if (transitionCounters.get(transition) >= maxInvariants) {
            return false;
        }

        // Check if the transition is token-sensitized.
        if (!isTokenSensitized(transition)) {
            return false;
        }

        // Check temporal window: if timed and alpha has not elapsed yet, cannot fire.
        SensibilizadoConTiempo st = timedTransitions[transition];
        if (st != null && st.isBeforeWindow()) {
            return false;
        }

        // Fire: update the marking.
        for (int i = 0; i < marking.length; i++) {
            marking[i] += incidenceMatrix[i][transition];
        }

        transitionCounters.incrementAndGet(transition);
        logger.logTransitionFiring(transition, marking, getTransitionCounters());

        // Update timed transition timestamps based on the new marking.
        updateTimestamps();

        return true;
    }

    /*
     * Returns the milliseconds the caller should sleep before retrying this transition.
     * Returns 0 if the transition is not timed or if the issue is token availability (not temporal).
     */
    public long getSleepTimeFor(int transition) {
        SensibilizadoConTiempo st = timedTransitions[transition];
        if (st == null) return 0;
        if (!isTokenSensitized(transition)) return 0;
        return st.getSleepTime();
    }

    /*
     * Returns which transitions are fully sensitized: counter ok, tokens ok, and temporal window open.
     */
    public boolean[] getSensitizedTransitions() {
        boolean[] output = new boolean[incidenceMatrix[0].length];
        for (int j = 0; j < incidenceMatrix[0].length; j++) {
            if (transitionCounters.get(j) >= maxInvariants) { output[j] = false; continue; }
            if (!isTokenSensitized(j))                  { output[j] = false; continue; }
            SensibilizadoConTiempo st = timedTransitions[j];
            if (st != null && st.isBeforeWindow())       { output[j] = false; continue; }
            output[j] = true;
        }
        return output;
    }

    /*
     * Checks if a transition is enabled by tokens alone (ignores time and counter).
     */
    private boolean isTokenSensitized(int transition) {
        for (int i = 0; i < marking.length; i++) {
            if (marking[i] < -incidenceMatrix[i][transition]) return false;
        }
        return true;
    }

    /*
     * After each firing, updates timestamps for all timed transitions based on the new marking:
     * - Newly token-sensitized transitions start their alpha countdown.
     * - De-sensitized transitions have their timer reset.
     */
    private void updateTimestamps() {
        for (int j = 0; j < incidenceMatrix[0].length; j++) {
            SensibilizadoConTiempo st = timedTransitions[j];
            if (st == null) continue;
            boolean tokenEnabled = isTokenSensitized(j);
            if (tokenEnabled && !st.isActive()) {
                st.setNuevoTimeStamp();
            } else if (!tokenEnabled && st.isActive()) {
                st.reset();
            }
        }
    }

    public int[][] getIncidenceMatrix() { return incidenceMatrix; }

    public int getTransitionCount(int t) { return transitionCounters.get(t); }

    public int[] getTransitionCounters() {
        int[] snapshot = new int[transitionCounters.length()];
        for (int i = 0; i < transitionCounters.length(); i++) snapshot[i] = transitionCounters.get(i);
        return snapshot;
    }
}