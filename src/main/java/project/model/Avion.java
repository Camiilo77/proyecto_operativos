package project.model;

import project.controller.Aeropuerto;

/**
 * La clase Avion extiende de Thread, lo que permite que cada instancia 
 * funcione como un hilo independiente en el sistema
 */
public class Avion extends Thread {
    private String idAvion;
    private Aeropuerto aeropuerto;

    /**
     * Constructor para inicializar el hilo del avión.
     * @param id Identificador único del avión.
     * @param aero Referencia al recurso compartido (el aeropuerto)
     */
    public Avion(String id, Aeropuerto aero) {
        this.idAvion = id;
        this.aeropuerto = aero;
    }

    /**
     * El método run contiene la lógica que ejecutará el hilo al iniciar
     * Define el ciclo de vida del proceso dentro del aeropuerto inteligente.
     */
    @Override
    public void run() {
        try {
            // FASE 1: Aterrizaje
            // El hilo solicita acceso a los recursos críticos (pista y puerta)
            // Esta llamada implementa la sincronización compuesta
            aeropuerto.aterrizar(idAvion);
            
            // FASE 2: Estancia en el Aeropuerto
            // Simulación de tiempo de embarque/desembarque mientras ocupa una puerta
            // El uso de Math.random() simula tiempos de proceso variables
            Thread.sleep((long) (Math.random() * 5000 + 2000));
            
            // FASE 3: Despegue
            // El hilo solicita nuevamente la pista (sección crítica) para salir
            // Al finalizar, libera tanto la pista como la puerta de embarque
            aeropuerto.despegar(idAvion);
            
        } catch (InterruptedException e) {
            // Manejo de excepciones en caso de que el hilo sea interrumpido abruptamente.
            System.err.println(idAvion + " interrumpido.");
        }
    }
}