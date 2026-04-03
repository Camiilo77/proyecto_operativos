package project.controller;

import java.util.concurrent.Semaphore;
import project.view.InterfazAeropuerto;

public class Aeropuerto {
    // Semáforo de conteo: Permite que varios aviones (hilos) ocupen puertas 
    // al mismo tiempo, hasta alcanzar el límite definido.
    private final Semaphore puertas;

    // Semáforo binario: Funciona como un Mutex. Solo permite que UN avión
    // use la pista a la vez, garantizando la exclusión mutua.
    private final Semaphore pista;
    
    // Referencia a la ventana para actualizar la interfaz visual.
    private InterfazAeropuerto gui;

    public Aeropuerto(int numPuertas) {
        // Inicializa los recursos disponibles.
        this.puertas = new Semaphore(numPuertas);
        this.pista = new Semaphore(1); 
    }

    public void setGui(InterfazAeropuerto gui) {
        this.gui = gui;
    }

    /**
     * Lógica de aterrizaje: Implementa la "Sincronización Compuesta".
     * Un avión no puede aterrizar si no tiene una puerta donde parquear.
     */
    public void aterrizar(String id) throws InterruptedException {
        gui.escribirLog(id + " solicitando aterrizaje...");
        
        // PASO 1: Reservar una puerta. 
        // Si no hay puertas, el hilo se detiene aquí (espera bloqueada).
        puertas.acquire(); 
        gui.actualizarPuertas(puertas.availablePermits());
        
        // PASO 2: Entrar a la sección crítica (la pista).
        // Solo un hilo puede pasar este punto a la vez.
        pista.acquire();
        gui.actualizarPista(true);
        gui.escribirLog(">>> " + id + " OCUPANDO PISTA para aterrizar.");
        
        // Simulación del tiempo físico que tarda el avión en pista.
        Thread.sleep(1500); 
        
        // PASO 3: Liberar la pista para otros aviones.
        // Importante: No libera la puerta aún porque el avión está parqueado.
        pista.release();
        gui.actualizarPista(false);
        gui.escribirLog(id + " despejó pista y está en PUERTA.");
    }

    /**
     * Lógica de despegue: El avión debe volver a usar la pista (sección crítica)
     * y al final liberar todos los recursos que ocupaba.
     */
    public void despegar(String id) throws InterruptedException {
        // Solicita la pista nuevamente para salir.
        pista.acquire();
        gui.actualizarPista(true);
        gui.escribirLog("<<< " + id + " DESPEGANDO. Usando pista.");
        
        Thread.sleep(1000);
        
        // Al terminar el despegue, libera ambos recursos.
        pista.release(); // Libera la pista.
        puertas.release(); // Libera la puerta, permitiendo que otro avión aterrice.
        
        // Actualiza la interfaz gráfica con los nuevos estados.
        gui.actualizarPista(false);
        gui.actualizarPuertas(puertas.availablePermits());
        gui.escribirLog(id + " ha dejado el aeropuerto.");
    }
}