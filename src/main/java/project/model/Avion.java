package project.model;

import project.controller.Aeropuerto;

/**
 * La clase Avion extiende de Thread, lo que permite que cada instancia
 * funcione como un hilo independiente en el sistema.
 * 
 * Cada avión pasa por un ciclo de vida completo:
 * EN_VUELO → ESPERANDO_PUERTA → ESPERANDO_PISTA → ATERRIZANDO → EN_PUERTA → DESPEGANDO → PARTIÓ
 * 
 * Los cambios de estado son observables por la interfaz gráfica para
 * actualizar la visualización en tiempo real.
 */
public class Avion extends Thread {
    private final String idAvion;
    private final Aeropuerto aeropuerto;
    
    /** Estado actual del avión – observable por la UI */
    private volatile EstadoAvion estado;
    
    /** Índice de la pista asignada para aterrizar/despegar */
    private volatile int pistaAsignada = -1;
    
    /** Índice de la puerta de embarque asignada */
    private volatile int puertaAsignada = -1;
    
    /** Momento de llegada al sistema (para estadísticas) */
    private final long tiempoLlegada;

    /**
     * Constructor para inicializar el hilo del avión.
     * @param id   Identificador único del avión (ej: "Avión-1")
     * @param aero Referencia al recurso compartido (el aeropuerto)
     */
    public Avion(String id, Aeropuerto aero) {
        super(id); // Nombrar el Thread con el ID del avión
        this.idAvion = id;
        this.aeropuerto = aero;
        this.estado = EstadoAvion.EN_VUELO;
        this.tiempoLlegada = System.currentTimeMillis();
    }

    /**
     * El método run contiene la lógica que ejecutará el hilo al iniciar.
     * Define el ciclo de vida completo del avión dentro del aeropuerto inteligente.
     * 
     * Cada transición de estado se notifica al controlador para que actualice
     * la interfaz gráfica y el registro de eventos.
     */
    @Override
    public void run() {
        try {
            // FASE 1: Aterrizaje
            // El hilo solicita acceso a los recursos críticos (puerta + pista)
            // Esta llamada implementa la sincronización compuesta:
            // primero adquiere puerta (semáforo de conteo), luego pista (semáforo binario)
            setEstado(EstadoAvion.ESPERANDO_PUERTA);
            aeropuerto.aterrizar(this);

            // FASE 2: Estancia en el Aeropuerto
            // Simulación de tiempo de embarque/desembarque mientras ocupa una puerta.
            // Math.random() genera tiempos variables entre 3 y 7 segundos.
            setEstado(EstadoAvion.EN_PUERTA);
            Thread.sleep((long) (Math.random() * 4000 + 3000));

            // FASE 3: Despegue
            // El hilo solicita nuevamente una pista (sección crítica) para salir.
            // Al finalizar, libera tanto la pista como la puerta de embarque.
            aeropuerto.despegar(this);
            setEstado(EstadoAvion.PARTIO);

        } catch (InterruptedException e) {
            System.err.println(idAvion + " interrumpido.");
            Thread.currentThread().interrupt();
        }
    }

    // --- Getters y Setters ---
    
    public String getIdAvion() { return idAvion; }
    
    public EstadoAvion getEstado() { return estado; }
    
    public void setEstado(EstadoAvion estado) { this.estado = estado; }

    public int getPistaAsignada() { return pistaAsignada; }
    
    public void setPistaAsignada(int pistaAsignada) { this.pistaAsignada = pistaAsignada; }

    public int getPuertaAsignada() { return puertaAsignada; }
    
    public void setPuertaAsignada(int puertaAsignada) { this.puertaAsignada = puertaAsignada; }

    public long getTiempoLlegada() { return tiempoLlegada; }
}