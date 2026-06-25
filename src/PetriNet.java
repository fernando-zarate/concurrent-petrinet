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
                                                          {  0,  0,  0,  1,  0,  1,  0,  0,  1, -1 } }; // P9;

    private int maxInvariants;
    private int[] transitionCounters = new int[incidenceMatrix[0].length];
    private Logger logger;

    public PetriNet(int maxInvariants, Logger logger) {
        this.maxInvariants = maxInvariants;
        this.logger = logger;
    }

    public boolean fireTransition(int transition) {

        // Check if the transition can be fired based on his own transition counter.
        if (transitionCounters[transition] >= maxInvariants) {
            return false;
        }

        // Check if the transition is enabled by tokens.
        for (int i = 0; i < marking.length; i++) {
            if (marking[i] < -incidenceMatrix[i][transition]) {
                return false;
            }
        }

        // If the transition is enabled, update the marking of the petri net by adding the corresponding column of the incidence matrix to the current marking.
        for (int i = 0; i < marking.length; i++) {
            marking[i] += incidenceMatrix[i][transition];
        }

        // Log the firing of the transition.
        transitionCounters[transition]++;
        logger.logTransitionFiring(transition, marking, transitionCounters);
        return true;
    }

    /*
     * Returns an array of booleans indicating which transitions are enabled to fire based on the current marking and the transition counters.
     * The place of each transition in the array corresponds to the index of the transition in the incidence matrix.
     */
    public boolean[] getSensitizedTransitions() {
        boolean[] output = new boolean[incidenceMatrix[0].length];
        for (int j = 0; j < incidenceMatrix[0].length; j++) {
            if (transitionCounters[j] >= maxInvariants) {
                output[j] = false;
                continue;
            }
            boolean isSensitized = true;
            for (int i = 0; i < marking.length; i++) {
                if (marking[i] < -incidenceMatrix[i][j]) {
                    isSensitized = false;
                    break;
                }
            }
            output[j] = isSensitized;
        }
        return output;
    }

    public int[][] getIncidenceMatrix() { return incidenceMatrix; }

    public int[] getTransitionCounters() { return transitionCounters; }
}
