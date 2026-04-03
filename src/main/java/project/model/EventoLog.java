package project.model;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Representa un evento registrado en el log compartido del aeropuerto.
 * Cada evento captura información sobre qué hilo realizó qué acción,
 * en qué recurso y en qué momento, facilitando el análisis de la concurrencia.
 */
public class EventoLog {
    
    /** Tipos de eventos que pueden ocurrir en el sistema */
    public enum TipoEvento {
        SOLICITUD,      // Avión solicita un recurso
        ADQUISICION,    // Avión adquiere un recurso (semáforo)
        LIBERACION,     // Avión libera un recurso
        OPERACION,      // Acción en progreso (aterrizando, despegando)
        RACE_CONDITION, // Evento de demo de condición de carrera
        DEADLOCK,       // Evento de demo de deadlock
        SISTEMA         // Evento del sistema (inicio, fin, etc.)
    }

    private final LocalTime timestamp;
    private final long threadId;
    private final String threadName;
    private final TipoEvento tipo;
    private final String recurso;
    private final String descripcion;

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    public EventoLog(TipoEvento tipo, String recurso, String descripcion) {
        this.timestamp = LocalTime.now();
        this.threadId = Thread.currentThread().getId(); // getId() para compatibilidad con Java 17
        this.threadName = Thread.currentThread().getName();
        this.tipo = tipo;
        this.recurso = recurso;
        this.descripcion = descripcion;
    }

    public LocalTime getTimestamp() { return timestamp; }
    public long getThreadId() { return threadId; }
    public String getThreadName() { return threadName; }
    public TipoEvento getTipo() { return tipo; }
    public String getRecurso() { return recurso; }
    public String getDescripcion() { return descripcion; }

    /**
     * Formato legible del evento para el log visual.
     * Ejemplo: [12:34:56.789] [Avión-3] ADQUISICION | Pista-1 | Pista adquirida para aterrizar
     */
    @Override
    public String toString() {
        return String.format("[%s] [%s] %s | %s | %s",
                timestamp.format(FORMATO),
                threadName,
                tipo.name(),
                recurso,
                descripcion);
    }
}
