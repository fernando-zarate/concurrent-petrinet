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

    //                                         T0  T1  T2  T3   T4   T5  T6  T7   T8   T9
    private final long[] alphas = new long[] {  0,  0, 100, 80,  0, 400,  0, 200, 160,  0 };

    private long[] timeStamp;
    private int maxInvariants;
    private int[] transitionCounters;
    private Logger logger;

    public PetriNet(int maxInvariants, Logger logger) {
        this.maxInvariants = maxInvariants;
        this.logger = logger;
        this.transitionCounters = new int[incidenceMatrix[0].length];
        
        // Inicializa el arreglo de tiempos
        this.timeStamp = new long[incidenceMatrix[0].length];
        
        //  Setea el timeStamp inicial (ahora) para las transiciones
        //    que ya están sensibilizadas por tokens al arrancar la red.
        boolean[] initialSensitized = getSensitizedTransitionsByMarking();
        long now = System.currentTimeMillis();
        for (int i = 0; i < initialSensitized.length; i++) {
            if (initialSensitized[i]) {
                timeStamp[i] = now;
            }
        }
    }

    public boolean fireTransition(int transition, Semaphore mutex) {

        if (transitionCounters[transition] >= maxInvariants) {
            return false;
        }

        // Reevalua después de despertar
        while (true) {
            
            if (!getSensitizedTransitionsByMarking()[transition]){
                return false; // No hay tokens, le devolvemos false al Monitor
            }

            long currentTime = System.currentTimeMillis();
            long timeToWait = (timeStamp[transition] + alphas[transition]) - currentTime; // Tiempo restante para cumplir el alpha

            if (timeToWait > 0) {
                // Todavía no se cumplió el tiempo
                mutex.release();
                try {
                    // 4: sleep(timeStamp + alfa - ahora)
                    Thread.sleep(timeToWait);
                    mutex.acquire();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
                // Al despertar, el bucle while vuelve a empezar, re-chequeando 
                // tokens y tiempo por si otro hilo alteró la red
            } else {
                // El tiempo ya se cumplió, rompemos el bucle para disparar
                break;
            }
        }

        // Guardamos quién estaba sensibilizado antes del disparo
        boolean[] sensitizedBefore = getSensitizedTransitionsByMarking();

        // Aplicamos el disparo modificando el marcado
        for (int i = 0; i < marking.length; i++) {
            marking[i] += incidenceMatrix[i][transition];
        }

        // Verificamos que se cumplan los invariantes en cada transición
        verifyPlaceInvariants();

        // Guardamos quién está sensibilizado después del disparo (ya en la ventana)
        boolean[] sensitizedAfter = getSensitizedTransitionsByMarking();
        long now = System.currentTimeMillis();
        
        for (int j = 0; j < sensitizedAfter.length; j++) {
            // Si la transición está sensibilizada ahora, y no lo estaba antes
            // o si es la misma transición que acaba de disparar y sigue sensibilizada
            if (sensitizedAfter[j] && (!sensitizedBefore[j] || j == transition)) {
                timeStamp[j] = now;
            }
        }

        transitionCounters[transition]++;
        logger.logTransitionFiring(transition, marking, transitionCounters);
        return true;
    }

    private void verifyPlaceInvariants() {
        int[][] invariants = {
            { 0, 1, 2, 3, 4, 5, 6, 9 },
            { 2, 3, 4, 7 },
            { 4, 5, 6, 8 }
        };
        int[] expectedSums = { 3, 1, 1 };
        for (int k = 0; k < invariants.length; k++) {
            int sum = 0;
            for (int idx : invariants[k]) sum += marking[idx];
            if (sum != expectedSums[k]) {
                throw new IllegalStateException("Place invariant violated");
            }
        }
    }

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
    
    public int[][] getIncidenceMatrix() { return incidenceMatrix; }

    public int[] getTransitionCounters() { return transitionCounters; }
}
