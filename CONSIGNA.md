# Programación Concurrente 2026 – Trabajo Práctico Final

## Condiciones

- El trabajo es grupal, de 5 alumnos (grupo de 4 es la excepción).
- La defensa del trabajo se coordinará con el grupo respecto a modalidad presencial o videoconferencia.
- La evaluación es individual.
- Solo se corrigen los trabajos subidos al aula virtual (LEV).
- Los problemas de concurrencia deben estar correctamente resueltos y explicados.
- El trabajo debe implementarse en Java.
- Se evaluará el uso de objetos, colecciones y conceptos de programación concurrente.

---

# Red de Petri: Sistema de Procesamiento de Transacciones de Pago

## Enunciado

La red de Petri modela un sistema de procesamiento de transacciones de pago similar al núcleo de un Payment Service Provider (PSP).

El sistema:

1. Recibe transacciones desde una cola de arribo.
2. Las admite.
3. Las enruta a uno de tres flujos de procesamiento.
4. Deposita el resultado en un buffer de salida para liquidación.

### Recursos compartidos

#### P7
Representa la sesión con el Gateway de Red de Tarjetas (Visa/Mastercard).

- Solicita autorizaciones.
- Confirma capturas de fondos al issuer.

#### P8
Representa un slot disponible en el motor antifraude.

- Evalúa señales de riesgo.
- Realiza scoring de transacciones.

#### P9
Buffer de salida con transacciones procesadas listas para:

- Confirmación al cliente.
- Envío a liquidación.

### Otras plazas

#### P0
Plaza IDLE correspondiente a la cola de arribo.

#### P1
Transacción admitida:

- Payload parseado.
- Schema validado.
- Cliente autenticado.

Lista para ser ruteada.

---

# Flujos de procesamiento

## 1. Pago con tarjeta de crédito/débito

Plazas: **P2, P3**

Requiere uso exclusivo de **P7** durante todo el flujo.

Pasos:

1. Solicitar autorización al issuer.
2. Esperar respuesta.
3. Capturar fondos.

---

## 2. Pago de alto riesgo

Plaza: **P4**

Requiere simultáneamente:

- P7 (Gateway)
- P8 (Motor antifraude)

Motivo:

- El motor antifraude consulta señales en tiempo real al gateway mientras realiza el scoring.

---

## 3. Transferencia bancaria

Plazas: **P5, P6**

Requiere uso exclusivo de **P8** durante todo el flujo.

Pasos:

1. Validar cuenta destino.
2. Ejecutar transferencia en la red bancaria.

---

# Propiedades de la Red

Determinar mediante PIPE:

- Deadlock.
- Vivacidad.
- Seguridad.

Además:

- Identificar invariantes de plaza.
- Identificar invariantes de transición.
- Explicar brevemente qué representa cada uno.

---

# Implementación

Implementar un monitor de concurrencia para ejecutar la red.

## Tablas requeridas

- Tabla de estados del sistema.
- Tabla de eventos del sistema.

## Cantidad de hilos

Determinar la cantidad necesaria para obtener el mayor paralelismo posible.

### Caso 1

Si un invariante de transición posee un conflicto con otro invariante:

- Debe existir un hilo encargado de las transiciones anteriores al conflicto.
- Luego un hilo por invariante.

### Caso 2

Si existe un join entre invariantes:

- Después del join debe haber tantos hilos como tokens simultáneos en la plaza.
- Cada hilo se encarga de las transiciones restantes.

## Gráfico de responsabilidades

Realizar un gráfico donde se visualice:

- Cantidad de hilos.
- Responsabilidad de cada hilo.
- Diferenciados mediante colores.

---

# Interfaz obligatoria

```java
public interface MonitorInterface {
    boolean fireTransition(int transition);
}
```

## Consideraciones importantes

### Monitor

- `fireTransition()` debe ser el único método público.
- El monitor no puede contener referencias a transiciones específicas.
- Debe ser completamente agnóstico a la red ejecutada.
- Debe funcionar para cualquier red cambiando únicamente los datos del modelo.

---

# Semántica Temporal

Las siguientes transiciones son temporales:

- T2
- T3
- T5
- T7
- T8

Requisitos:

- Implementarlas.
- Asignar tiempos en milisegundos (a elección del grupo).

## Análisis temporal

Realizar:

### Analítico

Justificar teóricamente los resultados.

### Práctico

Ejecutar múltiples veces el proyecto y analizar:

- Resultados.
- Variaciones de tiempos.
- Conclusiones obtenidas.

---

# Políticas

Implementar y analizar por separado dos políticas.

## 1. Política aleatoria

Las transacciones se distribuyen aleatoriamente entre los tres flujos.

## 2. Política priorizada

Priorizar el flujo de:

**Pago de alto riesgo**

(es decir, el que utiliza simultáneamente P7 y P8).

---

# Requerimientos

## 1

Modelar el proyecto mediante objetos Java utilizando un monitor de concurrencia.

### a)

Si se utilizan librerías externas, incluirlas en el proyecto.

### b)

El proyecto debe ejecutarse en cualquier sistema operativo e IDE sin configuraciones adicionales.

---

## 2

Debe existir una clase:

```java
Main
```

que inicie el programa.

---

## 3

Al finalizar:

- No deben quedar hilos activos.

---

## 4

Implementar un objeto:

```text
Política
```

que cumpla los objetivos establecidos.

---

## 5

Realizar el diagrama de clases.

---

## 6

Realizar un diagrama de secuencia mostrando:

- Ejecución de un disparo.
- Funcionamiento del monitor.
- Uso de la política.

---

## 7

Determinar y justificar la cantidad de hilos necesarios.

---

## 8

Realizar múltiples ejecuciones con:

**200 invariantes completados por ejecución**

Demostrar:

### a)

Cumplimiento de las políticas mediante la distribución de carga.

### b)

Cantidad de cada tipo de invariante y justificar resultados.

---

## 9

Registrar resultados mediante archivo de log.

---

## 10

Realizar análisis temporal.

### a)

La ejecución total debe durar entre:

**20 y 40 segundos**

---

## 11

Mostrar e interpretar:

- Invariantes de plaza.
- Invariantes de transición.

---

## 12

Verificar invariantes de plaza luego de cada disparo.

---

## 13

Verificar invariantes de transición mediante análisis de logs.

Condiciones:

- Utilizar expresiones regulares.
- Analizar el log al finalizar la ejecución.

Herramientas sugeridas:

- regex.com
- debuggex.com

---

# Entregables

## a)

Imagen del diagrama de clases.

## b)

Imagen del diagrama de secuencia.

## c)

Código fuente Java completo.

## d)

Informe obligatorio con:

- Explicación de la solución.
- Explicación del código.
- Criterios adoptados.
- Resultados obtenidos.

Todos los integrantes deben subir el trabajo al LEV.

---

# Fecha de entrega

**10 de junio de 2026**

---

# Referencia bibliográfica

Artículo:

"Algoritmos para determinar cantidad y responsabilidad de hilos en sistemas embebidos modelados con Redes de Petri S3_PR1"

https://www.researchgate.net/publication/358104149_Algoritmos_para_determinar_cantidad_y_responsabilidad_de_hilos_en_sistemas_embebidos_modelados_con_Redes_de_Petri_S_3_PR1
