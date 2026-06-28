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
    private final long[] alphas = new long[] {  0,  0, 100, 80,  0, 400,  0, 200, 140,  0 };

    private long[] timeStamp;
    private int maxInvariants;
    private int[] transitionCounters;
    private Logger logger;

    public PetriNet(int maxInvariants, Logger logger) {
        this.maxInvariants = maxInvariants;
        this.logger = logger;
        this.transitionCounters = new int[incidenceMatrix[0].length];
        
        // 1. Inicializar el arreglo de timeStamps
        this.timeStamp = new long[incidenceMatrix[0].length];
        
        // 2. Setear el timeStamp inicial (ahora) para las transiciones
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

        // Bucle para poder re-evaluar de forma segura después de despertar del sleep
        while (true) {
            
            // 1.1: estaSensibilizado()
            if (!getSensitizedTransitionsByMarking()[transition]){
                return false; // No hay tokens, le devolvemos false al Monitor
            }

            // 1.1.1: testVentanaTiempo()
            long currentTime = System.currentTimeMillis();
            long timeToWait = (timeStamp[transition] + alphas[transition]) - currentTime;

            if (timeToWait > 0) {
                // [antes == true] - Todavía no se cumplió el tiempo Alfa
                mutex.release();
                try{
                    // 4: sleep(timeStamp + alfa - ahora)
                    Thread.sleep(timeToWait);
                    mutex.acquire();
                } catch (InterruptedException e) {
                    boolean acquired = false;
                    while (!acquired) {
                        try { mutex.acquire(); acquired = true; }
                        catch (InterruptedException ignored) {}
                    }
                    Thread.currentThread().interrupt();
                    return false;
                }
                // Al despertar, el bucle while vuelve a empezar, re-chequeando 
                // tokens y tiempo por si otro hilo alteró la red.
            } else {
                // [ventana == true] - El tiempo Alfa ya se cumplió, rompemos el bucle para disparar
                break;
            }
        }

        // 6: calculoDeVectorEstado()
        // Guardamos una foto de quién estaba sensibilizado ANTES del disparo
        boolean[] sensitizedBefore = getSensitizedTransitionsByMarking();

        // Aplicamos el disparo modificando el marcado
        for (int i = 0; i < marking.length; i++) {
            marking[i] += incidenceMatrix[i][transition];
        }

        verifyPlaceInvariants();

        // 6.6: actualiceSensibilizadoT()
        // Sacamos una foto de quién está sensibilizado DESPUÉS del disparo
        boolean[] sensitizedAfter = getSensitizedTransitionsByMarking();
        long now = System.currentTimeMillis();
        
        // 6.6.1: setNuevoTimeStamp()
        for (int j = 0; j < sensitizedAfter.length; j++) {
            // Si la transición está sensibilizada ahora, y NO lo estaba antes...
            // O si es la misma transición que acaba de disparar y sigue sensibilizada (reinicia su propio ciclo)
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
