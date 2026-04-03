package project.model;

import java.util.concurrent.Semaphore;

/**
 * Representa una pista de aterrizaje/despegue del aeropuerto.
 * Cada pista es un recurso crítico protegido por un SEMÁFORO BINARIO (Semaphore(1)),
 * lo que garantiza que solo UN avión puede usarla a la vez (exclusión mutua).
 * 
 * La pista funciona como una sección crítica: ningún otro hilo puede acceder
 * a ella mientras un avión la está utilizando.
 */
public class Pista {
    private final int id;
    private final String nombre;
    
    /** 
     * Semáforo binario: Semaphore(1) == Mutex.
     * Solo permite 1 permiso, asegurando exclusión mutua en la pista.
     */
    private final Semaphore semaforo;
    
    /** Referencia al avión que actualmente usa la pista (null si está libre) */
    private volatile String avionActual;
    
    /** Estado de ocupación para consulta rápida por la UI */
    private volatile boolean ocupada;

    public Pista(int id) {
        this.id = id;
        this.nombre = "Pista-" + (id + 1);
        this.semaforo = new Semaphore(1); // Semáforo BINARIO: máximo 1 permiso
        this.avionActual = null;
        this.ocupada = false;
    }

    /**
     * Intenta adquirir la pista (bloquea si está ocupada).
     * Este método es bloqueante: el hilo se detendrá hasta que la pista esté libre.
     */
    public void adquirir(String idAvion) throws InterruptedException {
        semaforo.acquire();
        this.avionActual = idAvion;
        this.ocupada = true;
    }

    /**
     * Libera la pista para que otro avión pueda usarla.
     * Incrementa el contador del semáforo binario de 0 a 1.
     */
    public void liberar() {
        this.avionActual = null;
        this.ocupada = false;
        semaforo.release();
    }

    /**
     * Intenta adquirir la pista sin bloquear.
     * @return true si la pista fue adquirida, false si está ocupada.
     */
    public boolean intentarAdquirir(String idAvion) {
        if (semaforo.tryAcquire()) {
            this.avionActual = idAvion;
            this.ocupada = true;
            return true;
        }
        return false;
    }

    // Getters
    public int getId() { return id; }
    public String getNombre() { return nombre; }
    public Semaphore getSemaforo() { return semaforo; }
    public String getAvionActual() { return avionActual; }
    public boolean isOcupada() { return ocupada; }
}
