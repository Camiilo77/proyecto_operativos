package project.controller;

import project.model.EventoLog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

/**
 * Registro compartido de eventos concurrentes.
 * 
 * Todos los hilos (aviones) escriben en este log de forma concurrente.
 * Se usa un ReentrantLock para proteger la escritura y garantizar que
 * cada evento se registre de forma atómica e íntegra.
 * 
 * La clase notifica a listeners (la UI) cada vez que se registra un evento nuevo,
 * permitiendo actualizaciones en tiempo real del panel de log.
 */
public class RegistroEventos {

    /** Lista interna de todos los eventos registrados */
    private final List<EventoLog> eventos;

    /**
     * ReentrantLock para proteger el acceso concurrente a la lista de eventos.
     * Se prefiere sobre synchronized porque:
     * - Permite tryLock() para evitar deadlocks en la demo
     * - Es reentrant (un mismo hilo puede adquirirlo múltiples veces)
     * - Proporciona fairness configurable
     */
    private final ReentrantLock lock;

    /** Listeners a notificar cuando se registra un nuevo evento */
    private final List<Consumer<EventoLog>> listeners;

    public RegistroEventos() {
        this.eventos = new ArrayList<>();
        this.lock = new ReentrantLock(true); // fair = true para orden FIFO
        this.listeners = new ArrayList<>();
    }

    /**
     * Registra un nuevo evento en el log de forma thread-safe.
     * 
     * El ReentrantLock protege la sección crítica: solo un hilo puede
     * escribir en el log a la vez, evitando condiciones de carrera
     * en la lista de eventos.
     * 
     * @param tipo        Tipo del evento (SOLICITUD, ADQUISICION, etc.)
     * @param recurso     Recurso involucrado (ej: "Pista-1", "Puerta-3")
     * @param descripcion Descripción legible del evento
     */
    public void registrar(EventoLog.TipoEvento tipo, String recurso, String descripcion) {
        EventoLog evento = new EventoLog(tipo, recurso, descripcion);
        lock.lock();
        try {
            eventos.add(evento);
        } finally {
            lock.unlock();
        }
        // Notificar fuera del lock para no bloquear otros hilos
        notificarListeners(evento);
    }

    /**
     * Registra un evento pre-construido.
     */
    public void registrar(EventoLog evento) {
        lock.lock();
        try {
            eventos.add(evento);
        } finally {
            lock.unlock();
        }
        notificarListeners(evento);
    }

    /**
     * Agrega un listener que será notificado cada vez que se registre un evento.
     * Usado principalmente por la interfaz gráfica para actualizar el log visual.
     */
    public void agregarListener(Consumer<EventoLog> listener) {
        listeners.add(listener);
    }

    /**
     * Retorna una copia inmutable de todos los eventos registrados.
     */
    public List<EventoLog> getEventos() {
        lock.lock();
        try {
            return Collections.unmodifiableList(new ArrayList<>(eventos));
        } finally {
            lock.unlock();
        }
    }

    /**
     * Retorna el número total de eventos registrados.
     */
    public int getTotal() {
        lock.lock();
        try {
            return eventos.size();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Exporta todos los eventos como texto formateado.
     */
    public String exportarComoTexto() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== REGISTRO DE EVENTOS DEL AEROPUERTO ===\n\n");
        lock.lock();
        try {
            for (EventoLog e : eventos) {
                sb.append(e.toString()).append("\n");
            }
        } finally {
            lock.unlock();
        }
        return sb.toString();
    }

    private void notificarListeners(EventoLog evento) {
        for (Consumer<EventoLog> listener : listeners) {
            try {
                listener.accept(evento);
            } catch (Exception e) {
                // No dejar que un listener roto afecte al sistema
                System.err.println("Error en listener de eventos: " + e.getMessage());
            }
        }
    }
}
