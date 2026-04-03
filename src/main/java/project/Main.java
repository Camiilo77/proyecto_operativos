package project;

import project.controller.Aeropuerto;
import project.model.Avion;
import project.view.InterfazAeropuerto;

/**
 * Clase principal que actúa como el orquestador del sistema.
 * Su función es inicializar los componentes y lanzar la simulación.
 */
public class Main {
    public static void main(String[] args) {
        // DEFINICIÓN DE RECURSOS LIMITADOS
        // Establecemos cuántas puertas de embarque tendrá el aeropuerto.
        // Esto alimentará los semáforos de conteo.
        int puertasConfig = 3; 

        // INICIALIZACIÓN DE COMPONENTES
        // Creamos el controlador (Aeropuerto) que gestiona la lógica de los semáforos.
        Aeropuerto aero = new Aeropuerto(puertasConfig);
        
        // Creamos la vista (Interfaz) para la visualización gráfica de los procesos.
        InterfazAeropuerto vista = new InterfazAeropuerto(puertasConfig);
        
        // VINCULACIÓN
        // Conectamos el controlador con la interfaz para que los hilos puedan 
        // enviar actualizaciones visuales en tiempo real.
        aero.setGui(vista);
        
        // Hacemos visible la ventana de Swing.
        vista.setVisible(true);

        // LANZAMIENTO DE PROCESOS CONCURRENTES
        // Simulamos la llegada de 8 aviones (hilos independientes).
        // Al haber solo 3 puertas, aquí es donde se pondrá a prueba la exclusión mutua
        // y la sincronización: los últimos aviones deberán esperar a que los primeros liberen recursos.
        for (int i = 1; i <= 8; i++) {
            // Creamos e iniciamos cada hilo de Avion.
            // El método .start() es el que le indica a la JVM que ejecute el método run() de forma asíncrona.
            new Avion("Avión-" + i, aero).start();
            
            try { 
                // Pequeño retardo entre la creación de cada avión para simular
                // llegadas escalonadas en el tiempo.
                Thread.sleep(500); 
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}