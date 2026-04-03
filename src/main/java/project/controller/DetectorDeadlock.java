package project.controller;

import project.model.EventoLog;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Demostración interactiva de DEADLOCKS y estrategias de prevención.
 * 
 * ESCENARIO 1 (DEADLOCK):
 * - Avión-X adquiere Recurso-A y luego intenta Recurso-B
 * - Avión-Y adquiere Recurso-B y luego intenta Recurso-A
 * - Ambos quedan bloqueados esperando al otro → DEADLOCK (espera circular)
 * - Se usa tryAcquire con timeout para detectar y romper el deadlock
 * 
 * ESCENARIO 2 (PREVENCIÓN):
 * - Ambos aviones adquieren los recursos en el MISMO ORDEN (A → B)
 * - Al eliminar el orden circular, el deadlock es imposible
 * - Esta es la estrategia de "orden global de asignación" documentada en el proyecto
 * 
 * Las 4 condiciones necesarias para deadlock (Coffman):
 * 1. Exclusión mutua (los recursos no se comparten)
 * 2. Hold-and-wait (retener y esperar)
 * 3. No preemption (no se pueden quitar recursos)
 * 4. Espera circular → ESTA es la que rompemos con orden global
 */
public class DetectorDeadlock {

    private final RegistroEventos registro;

    public DetectorDeadlock(RegistroEventos registro) {
        this.registro = registro;
    }

    /**
     * Ejecuta ambas demostraciones en un hilo separado.
     */
    public void ejecutarDemo() {
        new Thread(() -> {
            try {
                registro.registrar(EventoLog.TipoEvento.DEADLOCK, "Demo",
                        "════════════════════════════════════════════════════════");
                registro.registrar(EventoLog.TipoEvento.DEADLOCK, "Demo",
                        "🔒 INICIO: Demostración de Deadlocks");
                registro.registrar(EventoLog.TipoEvento.DEADLOCK, "Demo",
                        "════════════════════════════════════════════════════════");

                demoDeadlock();
                Thread.sleep(2000);
                demoPrevencion();

                registro.registrar(EventoLog.TipoEvento.DEADLOCK, "Demo",
                        "════════════════════════════════════════════════════════");
                registro.registrar(EventoLog.TipoEvento.DEADLOCK, "Demo",
                        "🔒 FIN: Demostración completada.");
                registro.registrar(EventoLog.TipoEvento.DEADLOCK, "Demo",
                        "════════════════════════════════════════════════════════");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Demo-Deadlock").start();
    }

    /**
     * ESCENARIO 1: Provoca un deadlock deliberado.
     * Dos hilos adquieren recursos en ORDEN INVERSO → espera circular.
     * Se detecta con tryAcquire(timeout) para poder continuar.
     */
    private void demoDeadlock() throws InterruptedException {
        Semaphore recursoA = new Semaphore(1);
        Semaphore recursoB = new Semaphore(1);

        registro.registrar(EventoLog.TipoEvento.DEADLOCK, "Demo",
                "--- ESCENARIO 1: PROVOCANDO DEADLOCK ---");
        registro.registrar(EventoLog.TipoEvento.DEADLOCK, "Demo",
                "Avión-X tomará Recurso-A → luego Recurso-B");
        registro.registrar(EventoLog.TipoEvento.DEADLOCK, "Demo",
                "Avión-Y tomará Recurso-B → luego Recurso-A (orden INVERSO)");

        Thread avionX = new Thread(() -> {
            try {
                recursoA.acquire();
                registro.registrar(EventoLog.TipoEvento.DEADLOCK, "Recurso-A",
                        "Avión-X adquirió Recurso-A");
                Thread.sleep(500); // Da tiempo para que Y adquiera B

                registro.registrar(EventoLog.TipoEvento.DEADLOCK, "Recurso-B",
                        "Avión-X intenta adquirir Recurso-B... (esperando)");

                // tryAcquire con timeout para detectar deadlock
                boolean adquirido = recursoB.tryAcquire(3, TimeUnit.SECONDS);
                if (!adquirido) {
                    registro.registrar(EventoLog.TipoEvento.DEADLOCK, "Recurso-B",
                            "⚠ ¡DEADLOCK DETECTADO! Avión-X no pudo adquirir Recurso-B (timeout 3s)");
                    recursoA.release(); // Liberar para continuar
                } else {
                    recursoB.release();
                    recursoA.release();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Avión-X-Deadlock");

        Thread avionY = new Thread(() -> {
            try {
                recursoB.acquire();
                registro.registrar(EventoLog.TipoEvento.DEADLOCK, "Recurso-B",
                        "Avión-Y adquirió Recurso-B");
                Thread.sleep(500); // Da tiempo para que X adquiera A

                registro.registrar(EventoLog.TipoEvento.DEADLOCK, "Recurso-A",
                        "Avión-Y intenta adquirir Recurso-A... (esperando)");

                boolean adquirido = recursoA.tryAcquire(3, TimeUnit.SECONDS);
                if (!adquirido) {
                    registro.registrar(EventoLog.TipoEvento.DEADLOCK, "Recurso-A",
                            "⚠ ¡DEADLOCK DETECTADO! Avión-Y no pudo adquirir Recurso-A (timeout 3s)");
                    recursoB.release();
                } else {
                    recursoA.release();
                    recursoB.release();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Avión-Y-Deadlock");

        avionX.start();
        avionY.start();
        avionX.join();
        avionY.join();

        registro.registrar(EventoLog.TipoEvento.DEADLOCK, "Demo",
                "El deadlock fue detectado mediante timeout. En un sistema real, " +
                        "esto causaría bloqueo indefinido.");
    }

    /**
     * ESCENARIO 2: Prevención de deadlock con ORDEN GLOBAL.
     * Ambos hilos adquieren los recursos en el MISMO ORDEN (A → B).
     * Esto hace imposible la espera circular.
     */
    private void demoPrevencion() throws InterruptedException {
        Semaphore recursoA = new Semaphore(1);
        Semaphore recursoB = new Semaphore(1);

        registro.registrar(EventoLog.TipoEvento.DEADLOCK, "Demo",
                "--- ESCENARIO 2: PREVENCIÓN CON ORDEN GLOBAL ---");
        registro.registrar(EventoLog.TipoEvento.DEADLOCK, "Demo",
                "Ambos aviones adquirirán primero Recurso-A, luego Recurso-B (MISMO ORDEN).");
        registro.registrar(EventoLog.TipoEvento.DEADLOCK, "Demo",
                "Esto elimina la espera circular (condición de Coffman #4).");

        Thread avionX = new Thread(() -> {
            try {
                recursoA.acquire();
                registro.registrar(EventoLog.TipoEvento.DEADLOCK, "Recurso-A",
                        "Avión-X adquirió Recurso-A (paso 1)");
                Thread.sleep(300);

                recursoB.acquire();
                registro.registrar(EventoLog.TipoEvento.DEADLOCK, "Recurso-B",
                        "Avión-X adquirió Recurso-B (paso 2) — sin deadlock");

                Thread.sleep(200);
                recursoB.release();
                recursoA.release();
                registro.registrar(EventoLog.TipoEvento.DEADLOCK, "Demo",
                        "✔ Avión-X liberó ambos recursos correctamente.");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Avión-X-Orden");

        Thread avionY = new Thread(() -> {
            try {
                // MISMO ORDEN: A primero, luego B
                recursoA.acquire();
                registro.registrar(EventoLog.TipoEvento.DEADLOCK, "Recurso-A",
                        "Avión-Y adquirió Recurso-A (paso 1)");
                Thread.sleep(300);

                recursoB.acquire();
                registro.registrar(EventoLog.TipoEvento.DEADLOCK, "Recurso-B",
                        "Avión-Y adquirió Recurso-B (paso 2) — sin deadlock");

                Thread.sleep(200);
                recursoB.release();
                recursoA.release();
                registro.registrar(EventoLog.TipoEvento.DEADLOCK, "Demo",
                        "✔ Avión-Y liberó ambos recursos correctamente.");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Avión-Y-Orden");

        avionX.start();
        avionY.start();
        avionX.join();
        avionY.join();

        registro.registrar(EventoLog.TipoEvento.DEADLOCK, "Demo",
                "✔ Con orden global A → B para ambos hilos, no hubo deadlock.");
        registro.registrar(EventoLog.TipoEvento.DEADLOCK, "Demo",
                "✔ Esta misma estrategia usa el aeropuerto: PUERTA → PISTA (siempre).");
    }
}
