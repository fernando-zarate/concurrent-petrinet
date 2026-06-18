public class PetriNet {

    /*
     * Represent the current marking of the petri net, represented as an array of integers, where each element represents the number of tokens in a place.
     */
    //                                  P0  P1  P2  P3  P4  P5  P6  P7  P8  P9
    private int[] marking = new int[] {  3,  0,  0,  0,  0,  0,  0,  1,  1,  0 };

    /*
     * Represent the incidence matrix of the petri net, represented as a 2D array of integers.
     */
    //                                                T0  T1  T2  T3  T4  T5  T6  T7  T8  T9
    private int[][] incidenceMatrix = new int[][] { { -1,  0,  0,  0,  0,  0,  0,  0,  0,  1 },   // P0
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

    public PetriNet(Logger logger) {
        this.logger = logger;
    }

    public boolean fireTransition(int transition) {
        // Check if the transition is enabled, which means that for each place, the number of tokens in the place is greater than or equal to the number of tokens required by the transition to fire.
        for (int i = 0; i < marking.length; i++) {
            if (marking[i] < -incidenceMatrix[i][transition]) {
                return false;
            }
        }
        // If the transition is enabled, update the marking of the petri net by adding the corresponding column of the incidence matrix to the current marking.
        for (int i = 0; i < marking.length; i++) {
            marking[i] += incidenceMatrix[i][transition];
        }
        // Log the firing of the transition and the new marking of the petri net.
        logger.logTransitionFiring(transition, marking);
        return true;
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
