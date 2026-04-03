package project.controller;

import project.model.EventoLog;

/**
 * Demostración interactiva de CONDICIONES DE CARRERA.
 * 
 * Esta clase ejecuta dos escenarios para demostrar visualmente qué ocurre
 * cuando dos hilos modifican un recurso compartido:
 * 
 * ESCENARIO 1 (SIN PROTECCIÓN):
 * - Dos hilos incrementan un contador compartido 1000 veces cada uno
 * - Sin exclusión mutua, se producen condiciones de carrera
 * - El resultado final será < 2000 (incorrecto)
 * 
 * ESCENARIO 2 (CON SEMÁFORO):
 * - El mismo ejercicio pero protegido por un semáforo binario
 * - El resultado final será exactamente 2000 (correcto)
 * 
 * La diferencia entre ambos resultados demuestra de forma práctica
 * por qué la exclusión mutua es necesaria en programación concurrente.
 */
public class SimuladorCondicionesCarrera {

    private final RegistroEventos registro;

    /** Contador compartido — recurso que ambos hilos intentan modificar */
    private int contadorCompartido;

    public SimuladorCondicionesCarrera(RegistroEventos registro) {
        this.registro = registro;
    }

    /**
     * Ejecuta ambas demostraciones (sin protección y con protección).
     * Se ejecuta en un hilo separado para no bloquear la UI.
     */
    public void ejecutarDemo() {
        new Thread(() -> {
            try {
                registro.registrar(EventoLog.TipoEvento.RACE_CONDITION, "Demo",
                        "════════════════════════════════════════════════════════");
                registro.registrar(EventoLog.TipoEvento.RACE_CONDITION, "Demo",
                        "🏁 INICIO: Demostración de Condiciones de Carrera");
                registro.registrar(EventoLog.TipoEvento.RACE_CONDITION, "Demo",
                        "════════════════════════════════════════════════════════");

                demoSinProteccion();
                Thread.sleep(1500);
                demoConProteccion();

                registro.registrar(EventoLog.TipoEvento.RACE_CONDITION, "Demo",
                        "════════════════════════════════════════════════════════");
                registro.registrar(EventoLog.TipoEvento.RACE_CONDITION, "Demo",
                        "🏁 FIN: Demostración completada. Compare los resultados.");
                registro.registrar(EventoLog.TipoEvento.RACE_CONDITION, "Demo",
                        "════════════════════════════════════════════════════════");

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Demo-RaceCondition").start();
    }

    /**
     * ESCENARIO 1: Dos hilos modifican el contador SIN exclusión mutua.
     * El resultado esperado es 2000, pero será menor debido a la condición de carrera.
     * 
     * La race condition ocurre porque la operación contadorCompartido++ NO es atómica:
     * 1. Leer valor actual
     * 2. Incrementar
     * 3. Escribir valor nuevo
     * Dos hilos pueden leer el mismo valor antes de que el otro escriba.
     */
    private void demoSinProteccion() throws InterruptedException {
        contadorCompartido = 0;

        registro.registrar(EventoLog.TipoEvento.RACE_CONDITION, "Demo",
                "--- ESCENARIO 1: SIN PROTECCIÓN (sin semáforo) ---");
        registro.registrar(EventoLog.TipoEvento.RACE_CONDITION, "Demo",
                "Dos hilos incrementarán un contador 1000 veces cada uno.");
        registro.registrar(EventoLog.TipoEvento.RACE_CONDITION, "Demo",
                "Resultado esperado: 2000");

        Thread hilo1 = new Thread(() -> {
            for (int i = 0; i < 1000; i++) {
                contadorCompartido++; // ¡NO ATÓMICO! → Condición de carrera
            }
        }, "Hilo-A-SinLock");

        Thread hilo2 = new Thread(() -> {
            for (int i = 0; i < 1000; i++) {
                contadorCompartido++; // ¡NO ATÓMICO! → Condición de carrera
            }
        }, "Hilo-B-SinLock");

        hilo1.start();
        hilo2.start();
        hilo1.join();
        hilo2.join();

        registro.registrar(EventoLog.TipoEvento.RACE_CONDITION, "Demo",
                "⚠ Resultado REAL sin protección: " + contadorCompartido +
                        " (debería ser 2000)");
        registro.registrar(EventoLog.TipoEvento.RACE_CONDITION, "Demo",
                "⚠ Diferencia: " + (2000 - contadorCompartido) + " operaciones perdidas por race condition.");
    }

    /**
     * ESCENARIO 2: Los mismos hilos pero protegidos con un semáforo binario.
     * El resultado siempre será exactamente 2000.
     */
    private void demoConProteccion() throws InterruptedException {
        contadorCompartido = 0;
        final java.util.concurrent.Semaphore mutex = new java.util.concurrent.Semaphore(1);

        registro.registrar(EventoLog.TipoEvento.RACE_CONDITION, "Demo",
                "--- ESCENARIO 2: CON PROTECCIÓN (semáforo binario) ---");
        registro.registrar(EventoLog.TipoEvento.RACE_CONDITION, "Demo",
                "Misma operación pero protegida con Semaphore(1) como mutex.");

        Thread hilo1 = new Thread(() -> {
            for (int i = 0; i < 1000; i++) {
                try {
                    mutex.acquire(); // Adquirir exclusión mutua
                    contadorCompartido++;
                    mutex.release(); // Liberar exclusión mutua
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "Hilo-A-ConLock");

        Thread hilo2 = new Thread(() -> {
            for (int i = 0; i < 1000; i++) {
                try {
                    mutex.acquire();
                    contadorCompartido++;
                    mutex.release();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "Hilo-B-ConLock");

        hilo1.start();
        hilo2.start();
        hilo1.join();
        hilo2.join();

        registro.registrar(EventoLog.TipoEvento.RACE_CONDITION, "Demo",
                "✔ Resultado CON protección: " + contadorCompartido + " (correcto: 2000)");
        registro.registrar(EventoLog.TipoEvento.RACE_CONDITION, "Demo",
                "✔ El semáforo binario garantizó la exclusión mutua.");
    }
}
