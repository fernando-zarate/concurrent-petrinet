public class PetriNet {

    /*
     * Represent the current marking of the petri net, represented as an array of integers, where each element represents the number of tokens in a place.
     */
    //                                               P0  P1  P2  P3  P4  P5  P6  P7  P8  P9
    private static final int[] marking = new int[] {  3,  0,  0,  0,  0,  0,  0,  1,  1,  0 };

    /*
     * Represent the incidence matrix of the petri net, represented as a 2D array of integers.
     */
    //                                                             T0  T1  T2  T3  T4  T5  T6  T7  T8  T9
    private static final int[][] incidenceMatrix = new int[][] { { -1,  0,  0,  0,  0,  0,  0,  0,  0,  1 },   // P0
                                                                 {  1, -1,  0,  0, -1,  0, -1,  0,  0,  0 },   // P1
                                                                 {  0,  1, -1,  0,  0,  0,  0,  0,  0,  0 },   // P2
                                                                 {  0,  0,  1, -1,  0,  0,  0,  0,  0,  0 },   // P3
                                                                 {  0,  0,  0,  0,  1, -1,  0,  0,  0,  0 },   // P4
                                                                 {  0,  0,  0,  0,  0,  0,  1, -1,  0,  0 },   // P5
                                                                 {  0,  0,  0,  0,  0,  0,  0,  1, -1,  0 },   // P6
                                                                 {  0, -1,  0,  1, -1,  1,  0,  0,  0,  0 },   // P7
                                                                 {  0,  0,  0,  0, -1,  1, -1,  0,  1,  0 },   // P8
                                                                 {  0,  0,  0,  1,  0,  1,  0,  0,  1, -1 } }; // P9;

    private Logger logger;
    private long[] sensitizationTime;

    // Timed transitions configuration: T2, T3, T5, T7, T8
    private static final boolean[] isTimed = new boolean[] {
        false, // T0
        false, // T1
        true,  // T2
        true,  // T3
        false, // T4
        true,  // T5
        false, // T6
        true,  // T7
        true,  // T8
        false  // T9
    };

    // Delays in milliseconds (Adjusted so the total run time is between 20s and 40s)
    private static final long[] tMin = new long[] {
        0,   // T0
        0,   // T1
        150, // T2
        150, // T3
        0,   // T4
        300, // T5
        0,   // T6
        150, // T7
        150, // T8
        0    // T9
    };

    public PetriNet(Logger logger) {
        this.logger = logger;
        int numTransitions = incidenceMatrix[0].length;
        this.sensitizationTime = new long[numTransitions];
        
        // Initialize sensitization times for transitions enabled at startup
        long now = System.currentTimeMillis();
        boolean[] sensitized = getSensitizedTransitions();
        for (int i = 0; i < sensitized.length; i++) {
            if (sensitized[i]) {
                sensitizationTime[i] = now;
            } else {
                sensitizationTime[i] = -1;
            }
        }
    }

    public boolean fireTransition(int transition) {
        // Check if the transition is enabled
        for (int i = 0; i < marking.length; i++) {
            if (marking[i] < -incidenceMatrix[i][transition]) {
                return false;
            }
        }

        // Save sensitization status of all transitions before the fire
        boolean[] previouslySensitized = getSensitizedTransitions();

        // If the transition is enabled, update the marking of the petri net by adding the corresponding column of the incidence matrix to the current marking.
        for (int i = 0; i < marking.length; i++) {
            marking[i] += incidenceMatrix[i][transition];
        }

        // Update sensitization times for the new state
        long now = System.currentTimeMillis();
        boolean[] currentlySensitized = getSensitizedTransitions();
        for (int i = 0; i < currentlySensitized.length; i++) {
            if (!currentlySensitized[i]) {
                sensitizationTime[i] = -1;
            } else {
                // If it became sensitized now, or if it was already sensitized but was the fired transition
                if (!previouslySensitized[i] || i == transition) {
                    sensitizationTime[i] = now;
                }
            }
        }

        // Log the firing of the transition and the new marking of the petri net.
        logger.logTransitionFiring(transition, marking);
        return true;
    }

    public boolean isSensitizedByTokens(int transition) {
        for (int i = 0; i < marking.length; i++) {
            if (marking[i] < -incidenceMatrix[i][transition]) {
                return false;
            }
        }
        return true;
    }

    public boolean isTransitionTimed(int transition) {
        return isTimed[transition];
    }

    public long getTransitionTMin(int transition) {
        return tMin[transition];
    }

    public long getSensitizationTime(int transition) {
        return sensitizationTime[transition];
    }

    /*
     * Returns an array of booleans indicating which transitions are enabled to fire.
     * The place of each transition in the array corresponds to the index of the transition in the incidence matrix.
     */
    public boolean[] getSensitizedTransitions() {
        boolean[] output = new boolean[incidenceMatrix[0].length];

        // For each transition in the petri net, check if it is enabled to fire, and if it is, add 'true' to 'output', otherwise add 'false' to 'output'.
        for (int j = 0; j < incidenceMatrix[0].length; j++) {
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

    public int[] getMarking() { return marking; }

    public int[][] getIncidenceMatrix() { return incidenceMatrix; }
}
