package project.model;

/**
 * Representa una puerta de embarque individual en el aeropuerto.
 * Las puertas son recursos limitados gestionados por un SEMÁFORO DE CONTEO
 * a nivel del Aeropuerto (Semaphore(N) donde N = número total de puertas).
 * 
 * Esta clase modela cada puerta individual con su estado y avión asignado,
 * permitiendo a la interfaz gráfica mostrar cada puerta de forma visual.
 */
public class PuertaEmbarque {
    private final int id;
    private final String nombre;
    
    /** Referencia al avión que ocupa esta puerta (null si está libre) */
    private volatile String avionAsignado;
    
    /** Estado de la puerta */
    private volatile boolean ocupada;

    public PuertaEmbarque(int id) {
        this.id = id;
        this.nombre = "Puerta-" + (id + 1);
        this.avionAsignado = null;
        this.ocupada = false;
    }

    /**
     * Asigna un avión a esta puerta.
     * Nota: La sincronización de acceso se maneja a nivel del Aeropuerto
     * mediante el semáforo de conteo y ReentrantLock.
     */
    public void asignar(String idAvion) {
        this.avionAsignado = idAvion;
        this.ocupada = true;
    }

    /**
     * Libera esta puerta, desvinculando al avión.
     */
    public void liberar() {
        this.avionAsignado = null;
        this.ocupada = false;
    }

    // Getters
    public int getId() { return id; }
    public String getNombre() { return nombre; }
    public String getAvionAsignado() { return avionAsignado; }
    public boolean isOcupada() { return ocupada; }
}
