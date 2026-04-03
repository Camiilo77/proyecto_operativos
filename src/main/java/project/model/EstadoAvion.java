package project.model;

/**
 * Enumeración que define los posibles estados de un avión dentro del sistema.
 * Cada estado representa una fase en el ciclo de vida del avión en el aeropuerto.
 * Esta máquina de estados permite a la interfaz gráfica reaccionar visualmente
 * a cada transición del hilo.
 */
public enum EstadoAvion {
    /** El avión está en el aire aproximándose al aeropuerto */
    EN_VUELO("En Vuelo", "#3498db"),
    
    /** El avión espera a que se libere una puerta de embarque (semáforo de conteo) */
    ESPERANDO_PUERTA("Esperando Puerta", "#f39c12"),
    
    /** El avión tiene puerta asignada y espera una pista libre (semáforo binario) */
    ESPERANDO_PISTA("Esperando Pista", "#e67e22"),
    
    /** El avión está usando la pista para aterrizar (sección crítica) */
    ATERRIZANDO("Aterrizando", "#e74c3c"),
    
    /** El avión está estacionado en su puerta de embarque */
    EN_PUERTA("En Puerta", "#2ecc71"),
    
    /** El avión está usando la pista para despegar (sección crítica) */
    DESPEGANDO("Despegando", "#9b59b6"),
    
    /** El avión ha abandonado el aeropuerto */
    PARTIO("Partió", "#95a5a6");

    private final String descripcion;
    private final String colorHex;

    EstadoAvion(String descripcion, String colorHex) {
        this.descripcion = descripcion;
        this.colorHex = colorHex;
    }

    public String getDescripcion() { return descripcion; }
    public String getColorHex() { return colorHex; }
}
