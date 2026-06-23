# Explicación Detallada del Monitor y Transiciones Temporales

Este documento explica de forma clara y didáctica el funcionamiento del **Monitor de Concurrencia** implementado en el proyecto, detallando el patrón de sincronización utilizado, el flujo de ejecución de los hilos, y cómo deben incorporarse las **Transiciones Temporales** según los requisitos teóricos de la materia.

---

## 1. Funcionamiento General del Monitor

El monitor actúa como un intermediario o controlador de tráfico. Su objetivo principal es garantizar que:
1. El estado de la Red de Petri (marcado de plazas y tokens) se modifique bajo **exclusión mutua** (un solo hilo a la vez).
2. Los hilos se bloqueen de manera eficiente cuando la transición que quieren disparar no está habilitada (evitando espera activa).
3. Se despierte al hilo adecuado en cuanto su transición pase a estar habilitada, según una política de prioridad o selección.

### El Patrón "Pasaje de Testigo" (Pass the Baton)
El monitor de este proyecto utiliza el patrón **Pasaje de Testigo** (también conocido como *Señalización Privada*). 

En los monitores tradicionales de Java (usando `synchronized`, `wait()` y `notify()`), cuando un hilo bloqueado es despertado mediante `notify()`, este compite en igualdad de condiciones con cualquier hilo nuevo que intente entrar al monitor desde el exterior. Esto puede causar:
- **Inanición (Starvation)**: Hilos bloqueados que nunca logran entrar porque hilos nuevos les ganan el mutex.
- **Condiciones de carrera**: Para cuando el hilo despertado vuelve a adquirir el mutex, el estado de la red puede haber cambiado y su transición ya no estar habilitada.

El **Pasaje de Testigo** soluciona esto estructurando el control con tres elementos:
1.  **`mutex` (Semáforo binario inicializado en 1)**: Controla el acceso desde el exterior al monitor.
2.  **`waitingThreads` (Arreglo de Semáforos inicializados en 0)**: Un semáforo privado para cada transición. Si un hilo no puede disparar su transición, incrementa el contador de espera y se duerme en su semáforo privado liberando el `mutex` principal.
3.  **Heredar el Lock (El Testigo)**: Cuando un hilo que está ejecutando dentro del monitor finaliza un disparo, verifica si hay alguna transición habilitada que tenga hilos durmiendo en su semáforo privado. Si la hay:
    - Despierta a ese hilo haciendo `release()` de su semáforo privado.
    - **No libera el `mutex` principal**.
    - El hilo despertado se despierta **dentro** del monitor y **hereda la exclusión mutua**. Es decir, toma el "testigo" y continúa su ejecución sabiendo que ningún hilo externo pudo meterse en el medio.

---

## 2. Análisis del Código: `fireTransition(int transition)`

A continuación se detalla qué hace paso a paso el método principal del monitor:

```java
@Override
public boolean fireTransition(int transition) {
    // 1. INTENTO DE ENTRADA AL MONITOR
    try {
        mutex.acquire();
    } catch (InterruptedException e) {
        return false;
    }

    boolean k = true;
    while (k) {
        // 2. INTENTO DE DISPARO EN LA RED DE PETRI
        k = petriNet.fireTransition(transition);
        
        if (k) { // CASO A: El disparo fue exitoso
            
            // 3. SELECCIÓN DE HILOS A DESPERTAR (Pasaje de Testigo)
            boolean[] vs = petriNet.getSensitizedTransitions(); // Transiciones habilitadas por tokens
            boolean[] vc = getWaitingTransitions();             // Transiciones con hilos esperando
            boolean[] m = compareArrays(vs, vc);                // Intersección (m = vs AND vc)
            
            if (containsTrue(m)) {
                // Hay al menos una transición habilitada con hilos esperando.
                int transitionToFire = policy.selectTransition(m); // La política decide cuál
                
                // Despierta al hilo durmiendo en esa transición
                waitingThreads[transitionToFire].release(); 
                
                // IMPORTANTE: Retornamos true sin hacer mutex.release(). 
                // El hilo despertado hereda el mutex (toma el testigo).
                return true; 
            } else {
                // No hay ningún hilo esperando cuya transición esté habilitada.
                k = false; // Salimos del bucle
            }
            
        } else { // CASO B: El disparo falló (La transición no está habilitada)
            
            // 4. SUSPENSIÓN DEL HILO (Irse a dormir)
            waitingCount[transition]++; // Marcamos que este hilo está esperando
            mutex.release();             // Liberamos el mutex de entrada para que otros puedan operar

            try {
                waitingThreads[transition].acquire(); // Nos bloqueamos en nuestro semáforo privado
                
                // <<< AQUÍ DESPIERTA EL HILO CUANDO LE PASAN EL TESTIGO >>>
                // Nota: Al despertar, el hilo ya posee el mutex (lo heredó del que lo despertó).
                
                waitingCount[transition]--; // Ya no estamos esperando
                k = true;                   // Volvemos a intentar el disparo (bucle while)
                
            } catch (InterruptedException e) {
                waitingCount[transition]--;
                return false;
            }
        }
    }

    // 5. SALIDA DIRECTA SIN PASAR EL TESTIGO
    // Solo llegamos aquí si disparamos con éxito pero no había nadie a quien despertar (k = false).
    mutex.release(); // Liberamos el mutex para que entren nuevos hilos desde afuera.
    return true;
}
```

---

## 3. Transiciones Temporales (Timed Petri Nets)

En una Red de Petri Temporalizada, cada transición temporal $T_i$ tiene asociado un intervalo de tiempo $[t_{min}, t_{max}]$ (o simplemente una demora mínima $t_{min}$).

### Conceptos Clave
1.  **Sensibilización de la Transición**: Momento en que la transición recibe los tokens necesarios en sus plazas de entrada para estar habilitada.
2.  **Marca de Tiempo (Sensitization Timestamp)**: En el instante en que una transición se sensibiliza, la Red de Petri debe registrar el tiempo del sistema actual (`System.currentTimeMillis()`).
3.  **Estados Temporales**:
    - **Antes del Período ($t < t_{min}$)**: La transición está habilitada por tokens, pero aún no transcurrió el tiempo mínimo requerido. **No se puede disparar**. El hilo debe esperar.
    - **Dentro de la Ventana ($t_{min} \le t \le t_{max}$)**: La transición está habilitada por tokens y por tiempo. **Es disparable**.
    - **Después del Período ($t > t_{max}$)**: Excedió el tiempo límite (si existe un máximo establecido). Puede representar un timeout o pérdida de validez del disparo.

---

### ¿Cómo se implementa la Lógica Temporal en el Monitor?

Actualmente, el código de la Red de Petri y el Monitor no maneja tiempos. Para cumplir con la semántica temporal requerida en la consigna (para las transiciones $T_2, T_3, T_5, T_7, T_8$), se debe modificar el comportamiento del monitor. 

El flujo cuando un hilo intenta disparar una transición temporal es el siguiente:

1.  **Verificar si está habilitada por tokens**.
2.  Si está habilitada por tokens, calcular cuánto tiempo ha transcurrido desde que se sensibilizó:
    $$\text{tiempo\_transcurrido} = \text{System.currentTimeMillis()} - \text{timestamp\_sensibilizacion}$$
3.  **Evaluación Temporal**:
    -   **Si $\text{tiempo\_transcurrido} < t_{min}$ (Es muy temprano)**:
        El hilo **no puede disparar** y no debe quedarse ocupando el monitor. Debe:
        1. Calcular el tiempo restante de espera: $\text{tiempo\_restante} = t_{min} - \text{tiempo\_transcurrido}$.
        2. Liberar el `mutex` del monitor.
        3. Dormirse fuera del monitor con `Thread.sleep(tiempo_restante)`.
        4. Al despertar, intentar volver a adquirir el `mutex` y reevaluar la transición.
    -   **Si $\text{tiempo\_transcurrido} \ge t_{min}$ (Está a tiempo)**:
        El disparo procede de inmediato. Se ejecuta el disparo físico, se actualizan los timestamps de sensibilización de las transiciones afectadas y se realiza el pasaje de testigo.

### Modificaciones necesarias en el código (Pseudocódigo / Ejemplo)

#### A. En `PetriNet.java`
Se debe llevar un registro de cuándo se sensibilizó cada transición:
```java
private long[] sensitizationTime = new long[numTransitions];

// Al inicializar la red, registrar el tiempo de las transiciones que arrancan sensibilizadas:
public void initSensitizationTimes() {
    long now = System.currentTimeMillis();
    boolean[] sensitized = getSensitizedTransitions();
    for (int i = 0; i < sensitized.length; i++) {
        if (sensitized[i]) {
            sensitizationTime[i] = now;
        } else {
            sensitizationTime[i] = -1; // No sensibilizada
        }
    }
}

// Al disparar con éxito una transición:
// Se debe actualizar el sensitizationTime para las transiciones que se sensibilizaron NUEVAMENTE
// o que se mantuvieron sensibilizadas tras el disparo.
```

#### B. En `Monitor.java`
Dentro de `fireTransition(int transition)`, antes de disparar físicamente, se debe evaluar si es temporal y si está en la ventana:
```java
// Dentro del bucle while (k) del fireTransition:
if (petriNet.isSensitizedByTokens(transition)) {
    
    if (isTemporal(transition)) {
        long now = System.currentTimeMillis();
        long sensTime = petriNet.getSensitizationTime(transition);
        long elapsed = now - sensTime;
        long tMin = getTMin(transition);
        
        if (elapsed < tMin) {
            // El hilo llegó antes del tiempo mínimo. Debe dormir la diferencia fuera del monitor.
            long sleepTime = tMin - elapsed;
            
            // Liberamos el mutex para que otros hilos operen
            mutex.release();
            
            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                // Manejo de interrupción
            }
            
            // Volvemos a competir por entrar al monitor
            mutex.acquire();
            k = true; // Forzamos a iterar el bucle para volver a verificar
            continue; 
        }
    }
    
    // Si llegó a esta parte, está habilitada por tokens y por tiempo.
    // Dispara la transición normalmente...
}
```

---

## 4. Preguntas Frecuentes para una Defensa Oral

Si los docentes te preguntan sobre el funcionamiento del monitor en el examen, aquí tienes las respuestas clave:

*   **P: ¿Qué es el "Pasaje de Testigo" (Pass the Baton)?**
    *   **R**: Es un patrón de sincronización donde un hilo que está dentro del monitor y realiza un cambio de estado exitoso, despierta directamente a un hilo bloqueado y le cede el control del semáforo de exclusión mutua (`mutex`) sin liberarlo al exterior. El hilo despertado continúa su ejecución inmediatamente sin competir con nuevos hilos entrantes.
*   **P: ¿Por qué usamos semáforos privados en lugar de `wait()` y `notify()` estándar de Java?**
    *   **R**: Con `notify()` no se puede elegir específicamente qué hilo despertar (despierta a uno aleatorio en la cola del objeto). Con semáforos privados (`waitingThreads[i]`), podemos despertar con precisión quirúrgica al hilo que está esperando la transición exacta que se acaba de habilitar y asegurar que herede el mutex de forma inmediata.
*   **P: ¿Qué ocurre si un hilo llega "antes de tiempo" a una transición temporal?**
    *   **R**: El monitor detecta que no ha transcurrido el tiempo mínimo de sensibilización ($t < t_{min}$). Para no bloquear el monitor completo, el hilo libera el mutex principal, se duerme utilizando `Thread.sleep()` por el tiempo restante y, una vez transcurrido ese tiempo, vuelve a adquirir el mutex para intentar el disparo.
*   **P: ¿Cómo garantiza el monitor que es agnóstico a la Red de Petri?**
    *   **R**: El monitor no contiene condiciones específicas de plazas o nombres de transiciones en su lógica. Toda la lógica de "cuáles transiciones están habilitadas" (`getSensitizedTransitions`) y "cuál es el marcado" se delega al objeto `PetriNet`. El monitor solo recibe arreglos booleanos y un índice numérico de transición, lo que permite cambiar la Red de Petri completa sin modificar una sola línea de código del Monitor.
