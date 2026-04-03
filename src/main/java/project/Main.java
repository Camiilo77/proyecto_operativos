package project;

import javafx.application.Application;
import project.view.AeropuertoApp;

/**
 * Clase principal que actúa como punto de entrada del sistema.
 * Lanza la aplicación JavaFX que contiene toda la simulación.
 * 
 * La arquitectura del proyecto sigue el patrón MVC:
 * - Model:      Avion, Pista, PuertaEmbarque, EstadoAvion, EventoLog
 * - View:       AeropuertoApp (JavaFX)
 * - Controller: Aeropuerto, RegistroEventos, SimuladorCondicionesCarrera, DetectorDeadlock
 * 
 * Mecanismos de concurrencia utilizados:
 * - java.lang.Thread: Cada avión es un hilo independiente
 * - java.util.concurrent.Semaphore: Binario (pistas) y de conteo (puertas)
 * - java.util.concurrent.locks.ReentrantLock: Protección de asignación de recursos
 */
public class Main {
    public static void main(String[] args) {
        // Lanzar la aplicación JavaFX
        // El método launch() inicia el ciclo de vida de JavaFX:
        // 1. init() → 2. start(Stage) → 3. stop()
        Application.launch(AeropuertoApp.class, args);
    }
}