import java.util.concurrent.Semaphore;

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
    private long[] timeStamp;
    private long[] alphas;

    public PetriNet(int maxInvariants, Logger logger, long[] alphas) {
        this.maxInvariants = maxInvariants;
        this.logger = logger;
        this.alphas = alphas;
    }

    public boolean fireTransition(int transition, Semaphore mutex) {

        // Check if the transition can be fired based on his own transition counter.
        if (transitionCounters[transition] >= maxInvariants) {
            return false;
        }

        // Check if the transition is enabled by tokens.
        if(getSensitizedTransitionsByMarking()[transition] == false){
            return false;
        }

        // Check if the transition is enabled by time.
        if (getSensitizedTransitionsByTime(transition)[transition] == false) {
            mutex.release(); // Release the mutex before returning false to avoid deadlock.
            //Si llego aca es porque esta sensibilizada por tokens pero no por tiempo
            try{
                Thread.sleep(timeStamp[transition] + alphas[transition] - System.currentTimeMillis());
                mutex.acquire();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // If the transition is enabled, update the marking of the petri net by adding the corresponding column of the incidence matrix to the current marking.
        for (int i = 0; i < marking.length; i++) {
            marking[i] += incidenceMatrix[i][transition];
        }

        setTimeStamp(System.currentTimeMillis(), transition);

        // Log the firing of the transition.
        transitionCounters[transition]++;
        logger.logTransitionFiring(transition, marking, transitionCounters);
        return true;
    }

    /*
     * Returns an array of booleans indicating which transitions are enabled to fire based on the current marking and the transition counters.
     * The place of each transition in the array corresponds to the index of the transition in the incidence matrix.
     */

    public boolean[] getSensitizedTransitionsByMarking(){
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


    public boolean[] getSensitizedTransitionsByTime(int transition){

        boolean[] output = new boolean[incidenceMatrix[0].length];

        for (int j = 0; j < incidenceMatrix[0].length; j++) {
            boolean isSensitized = true;
            for (int i = 0; i < incidenceMatrix[0].length; i++) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - getTimeStamp()[i] < alphas[i]) {
                    isSensitized = false;
                }
            }
            output[j] = isSensitized;
        }
        return output;
    }


    public void setTimeStamp(long timeStamp, int transition) {
        this.timeStamp[transition] = timeStamp;
    }
    public long[] getTimeStamp() {
        return timeStamp;
    }

    public int[][] getIncidenceMatrix() { return incidenceMatrix; }

    public int[] getTransitionCounters() { return transitionCounters; }
}