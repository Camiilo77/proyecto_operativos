package project.controller;

import project.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Controlador principal del aeropuerto - "Torre de Control".
 * 
 * Gestiona todos los recursos críticos del aeropuerto utilizando:
 * - SEMÁFOROS BINARIOS: Uno por cada pista (Semaphore(1)), garantizando
 *   que solo un avión use cada pista a la vez (exclusión mutua).
 * - SEMÁFORO DE CONTEO: Para las puertas de embarque (Semaphore(N)),
 *   permitiendo que hasta N aviones ocupen puertas simultáneamente.
 * - REENTRANT LOCK: Para proteger la asignación/liberación de recursos
 *   específicos (saber cuál pista o puerta asignar de forma atómica).
 * 
 * ESTRATEGIA DE PREVENCIÓN DE DEADLOCKS:
 * Se implementa un ORDEN GLOBAL de adquisición de recursos:
 *   1° → Puerta de embarque (semáforo de conteo)
 *   2° → Pista (semáforo binario)
 * Todos los hilos siguen este orden, eliminando la posibilidad de
 * espera circular (condición necesaria para deadlock).
 */
public class Aeropuerto {

    /** Array de pistas de aterrizaje (cada una con su semáforo binario) */
    private final Pista[] pistas;

    /** Array de puertas de embarque */
    private final PuertaEmbarque[] puertas;

    /**
     * Semáforo de conteo: Controla el número máximo de aviones
     * que pueden estar en puertas de embarque simultáneamente.
     * Semaphore(numPuertas) → N permisos disponibles.
     */
    private final Semaphore semaforoPuertas;

    /**
     * ReentrantLock: Protege las secciones donde se buscan y asignan
     * recursos específicos (cuál pista concreta o cuál puerta concreta).
     * Sin este lock, dos hilos podrían asignar la misma puerta/pista
     * (condición de carrera en la asignación).
     */
    private final ReentrantLock lockAsignacion;

    /** Registro de eventos compartido (thread-safe) */
    private final RegistroEventos registro;

    /** Lista de aviones actualmente en el sistema */
    private final List<Avion> avionesActivos;

    /** Lock para proteger la lista de aviones activos */
    private final ReentrantLock lockAviones;

    /** Listeners para notificar cambios de estado a la UI */
    private final List<Runnable> cambioListeners;

    /** Contadores de estadísticas */
    private volatile int avionesAtendidos = 0;
    private volatile int avionesEnEspera = 0;

    /**
     * Construye el aeropuerto con la cantidad especificada de recursos.
     * @param numPistas  Número de pistas de aterrizaje (cada una con semáforo binario)
     * @param numPuertas Número de puertas de embarque (semáforo de conteo)
     */
    public Aeropuerto(int numPistas, int numPuertas) {
        // Inicializar pistas: cada una con su propio semáforo binario
        this.pistas = new Pista[numPistas];
        for (int i = 0; i < numPistas; i++) {
            pistas[i] = new Pista(i);
        }

        // Inicializar puertas de embarque
        this.puertas = new PuertaEmbarque[numPuertas];
        for (int i = 0; i < numPuertas; i++) {
            puertas[i] = new PuertaEmbarque(i);
        }

        // Semáforo de conteo: permite hasta numPuertas aviones en puertas
        this.semaforoPuertas = new Semaphore(numPuertas, true); // fair=true para evitar starvation

        this.lockAsignacion = new ReentrantLock(true);
        this.registro = new RegistroEventos();
        this.avionesActivos = new ArrayList<>();
        this.lockAviones = new ReentrantLock();
        this.cambioListeners = new ArrayList<>();

        registro.registrar(EventoLog.TipoEvento.SISTEMA, "Aeropuerto",
                "Aeropuerto inicializado con " + numPistas + " pistas y " + numPuertas + " puertas.");
    }

    /**
     * ATERRIZAJE: Implementa la SINCRONIZACIÓN COMPUESTA.
     * Un avión solo puede aterrizar si hay puerta Y pista disponibles.
     * 
     * ORDEN DE ADQUISICIÓN (prevención de deadlock):
     * 1. Adquirir puerta (semáforo de conteo) → puede esperar si todas ocupadas
     * 2. Asignar puerta concreta (ReentrantLock) → protege contra race condition
     * 3. Adquirir pista (semáforo binario) → puede esperar si todas ocupadas
     * 4. Asignar pista concreta (ReentrantLock) → protege contra race condition
     * 5. Aterrizar (sección crítica en la pista)
     * 6. Liberar pista (mantiene puerta)
     */
    public void aterrizar(Avion avion) throws InterruptedException {
        String id = avion.getIdAvion();

        // Registrar solicitud
        registro.registrar(EventoLog.TipoEvento.SOLICITUD, "Aeropuerto",
                id + " solicita aterrizar.");
        avion.setEstado(EstadoAvion.ESPERANDO_PUERTA);
        avionesEnEspera++;
        notificarCambio();

        // PASO 1: Adquirir permiso de puerta (SEMÁFORO DE CONTEO)
        // Si no hay puertas disponibles, el hilo se BLOQUEA aquí
        semaforoPuertas.acquire();

        // PASO 2: Asignar una puerta específica (REENTRANT LOCK)
        // El lock protege la búsqueda de la puerta libre para evitar
        // que dos hilos elijan la misma puerta (condición de carrera)
        int puertaIdx = -1;
        lockAsignacion.lock();
        try {
            for (int i = 0; i < puertas.length; i++) {
                if (!puertas[i].isOcupada()) {
                    puertas[i].asignar(id);
                    puertaIdx = i;
                    break;
                }
            }
        } finally {
            lockAsignacion.unlock();
        }

        avion.setPuertaAsignada(puertaIdx);
        registro.registrar(EventoLog.TipoEvento.ADQUISICION,
                puertas[puertaIdx].getNombre(),
                id + " tiene asignada " + puertas[puertaIdx].getNombre());
        notificarCambio();

        // PASO 3: Esperar una pista libre (SEMÁFORO BINARIO)
        avion.setEstado(EstadoAvion.ESPERANDO_PISTA);
        notificarCambio();

        // Buscar una pista libre entre todas las disponibles
        int pistaIdx = adquirirPistaDisponible(id);
        avion.setPistaAsignada(pistaIdx);
        avionesEnEspera--;

        registro.registrar(EventoLog.TipoEvento.ADQUISICION,
                pistas[pistaIdx].getNombre(),
                id + " OCUPANDO " + pistas[pistaIdx].getNombre() + " para aterrizar.");

        // PASO 4: ATERRIZAR (sección crítica en la pista)
        avion.setEstado(EstadoAvion.ATERRIZANDO);
        notificarCambio();
        Thread.sleep(2000); // Simula el tiempo físico de aterrizaje

        // PASO 5: Liberar la pista (el avión va a la puerta)
        pistas[pistaIdx].liberar();
        avion.setPistaAsignada(-1);

        registro.registrar(EventoLog.TipoEvento.LIBERACION,
                pistas[pistaIdx].getNombre(),
                id + " liberó " + pistas[pistaIdx].getNombre() + " y se dirige a " + puertas[puertaIdx].getNombre());

        avion.setEstado(EstadoAvion.EN_PUERTA);
        notificarCambio();
    }

    /**
     * DESPEGUE: El avión debe nuevamente usar una pista (sección crítica)
     * y al final liberar TODOS los recursos que ocupa.
     * 
     * ORDEN DE LIBERACIÓN (inverso a la adquisición):
     * 1. Adquirir pista (semáforo binario)
     * 2. Despegar (sección crítica)
     * 3. Liberar pista (semáforo binario)
     * 4. Liberar puerta (semáforo de conteo)
     */
    public void despegar(Avion avion) throws InterruptedException {
        String id = avion.getIdAvion();
        int puertaIdx = avion.getPuertaAsignada();

        registro.registrar(EventoLog.TipoEvento.SOLICITUD, "Aeropuerto",
                id + " solicita despegar desde " + puertas[puertaIdx].getNombre());

        avion.setEstado(EstadoAvion.ESPERANDO_PISTA);
        notificarCambio();

        // PASO 1: Adquirir pista para despegar (SEMÁFORO BINARIO)
        int pistaIdx = adquirirPistaDisponible(id);
        avion.setPistaAsignada(pistaIdx);

        registro.registrar(EventoLog.TipoEvento.ADQUISICION,
                pistas[pistaIdx].getNombre(),
                id + " OCUPANDO " + pistas[pistaIdx].getNombre() + " para DESPEGAR.");

        // PASO 2: DESPEGAR (sección crítica en la pista)
        avion.setEstado(EstadoAvion.DESPEGANDO);
        notificarCambio();
        Thread.sleep(1500); // Simula el tiempo físico de despegue

        // PASO 3: Liberar pista (SEMÁFORO BINARIO)
        pistas[pistaIdx].liberar();
        avion.setPistaAsignada(-1);

        registro.registrar(EventoLog.TipoEvento.LIBERACION,
                pistas[pistaIdx].getNombre(),
                id + " liberó " + pistas[pistaIdx].getNombre());

        // PASO 4: Liberar puerta (SEMÁFORO DE CONTEO)
        lockAsignacion.lock();
        try {
            if (puertaIdx >= 0 && puertaIdx < puertas.length) {
                puertas[puertaIdx].liberar();
            }
        } finally {
            lockAsignacion.unlock();
        }
        semaforoPuertas.release();
        avion.setPuertaAsignada(-1);

        avionesAtendidos++;
        registro.registrar(EventoLog.TipoEvento.LIBERACION,
                puertas[puertaIdx].getNombre(),
                id + " liberó " + puertas[puertaIdx].getNombre() + " y ha dejado el aeropuerto.");
        notificarCambio();
    }

    /**
     * Busca una pista libre y la adquiere de forma atómica.
     * Usa el ReentrantLock para iterar las pistas y los semáforos binarios
     * de cada pista para garantizar exclusión mutua.
     * 
     * @return El índice de la pista adquirida
     */
    private int adquirirPistaDisponible(String idAvion) throws InterruptedException {
        while (true) {
            lockAsignacion.lock();
            try {
                for (int i = 0; i < pistas.length; i++) {
                    if (pistas[i].intentarAdquirir(idAvion)) {
                        return i;
                    }
                }
            } finally {
                lockAsignacion.unlock();
            }
            // Ninguna pista libre, esperar brevemente antes de reintentar
            Thread.sleep(100);
        }
    }

    // --- Gestión de aviones activos ---

    public void registrarAvion(Avion avion) {
        lockAviones.lock();
        try {
            avionesActivos.add(avion);
        } finally {
            lockAviones.unlock();
        }
        notificarCambio();
    }

    public void removerAvion(Avion avion) {
        lockAviones.lock();
        try {
            avionesActivos.remove(avion);
        } finally {
            lockAviones.unlock();
        }
        notificarCambio();
    }

    public List<Avion> getAvionesActivos() {
        lockAviones.lock();
        try {
            return new ArrayList<>(avionesActivos);
        } finally {
            lockAviones.unlock();
        }
    }

    // --- Listeners para la UI ---

    public void agregarCambioListener(Runnable listener) {
        cambioListeners.add(listener);
    }

    private void notificarCambio() {
        for (Runnable listener : cambioListeners) {
            try {
                listener.run();
            } catch (Exception e) {
                // No dejar que un listener roto detenga el sistema
            }
        }
    }

    // --- Getters ---

    public Pista[] getPistas() { return pistas; }
    public PuertaEmbarque[] getPuertas() { return puertas; }
    public RegistroEventos getRegistro() { return registro; }
    public int getAvionesAtendidos() { return avionesAtendidos; }
    public int getAvionesEnEspera() { return avionesEnEspera; }
    public int getPuertasDisponibles() { return semaforoPuertas.availablePermits(); }
    public int getNumPistas() { return pistas.length; }
    public int getNumPuertas() { return puertas.length; }
}