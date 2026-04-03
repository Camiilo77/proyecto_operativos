package project.view;

import javax.swing.*;
import java.awt.*;

/**
 * Esta clase hereda de JFrame para crear la ventana principal de la simulación.
 * Se encarga de mostrar visualmente el estado de los recursos críticos.
 */
public class InterfazAeropuerto extends JFrame {
    private JTextArea log;
    private JLabel lblPista, lblPuertas;

    /**
     * Constructor que configura los componentes visuales de la interfaz.
     * @param puertasTotales El número inicial de puertas disponibles.
     */
    public InterfazAeropuerto(int puertasTotales) {
        // Configuración básica de la ventana
        setTitle("Simulación Aeropuerto Inteligente - UPTC");
        setSize(550, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // PANEL DE ESTADO: Muestra información visual rápida sobre los recursos.
        // Se usa un GridLayout para dividir el espacio de forma equitativa.
        JPanel pnlStatus = new JPanel(new GridLayout(1, 2));
        
        // Indicador de Pista: Cambia de color según el estado (Verde/Rojo).
        lblPista = new JLabel("PISTA: LIBRE", SwingConstants.CENTER);
        lblPista.setOpaque(true);
        lblPista.setBackground(Color.GREEN);
        
        // Indicador de Puertas: Muestra el conteo de espacios disponibles.
        lblPuertas = new JLabel("PUERTAS LIBRES: " + puertasTotales, SwingConstants.CENTER);
        
        pnlStatus.add(lblPista);
        pnlStatus.add(lblPuertas);
        add(pnlStatus, BorderLayout.NORTH);

        // REGISTRO DE EVENTOS (LOG): Área de texto para el histórico de operaciones.
        // Es un "Log compartido" donde todos los hilos (aviones) escriben sus acciones.
        log = new JTextArea();
        log.setEditable(false); // Evita que el usuario modifique el texto manualmente.
        log.setBackground(Color.BLACK);
        log.setForeground(Color.CYAN);
        
        // Se añade dentro de un JScrollPane para permitir el desplazamiento si hay muchos mensajes.
        add(new JScrollPane(log), BorderLayout.CENTER);
    }

    /**
     * Añade un mensaje al log de eventos.
     * SwingUtilities.invokeLater asegura que la actualización ocurra en el hilo de la GUI,
     * evitando errores de concurrencia visual.
     */
    public void escribirLog(String msj) {
        SwingUtilities.invokeLater(() -> log.append(msj + "\n"));
    }

    /**
     * Actualiza visualmente el estado de la pista.
     * @param ocupada true para mostrar OCUPADA (Rojo), false para LIBRE (Verde).
     */
    public void actualizarPista(boolean ocupada) {
        SwingUtilities.invokeLater(() -> {
            lblPista.setText(ocupada ? "PISTA: OCUPADA" : "PISTA: LIBRE");
            lblPista.setBackground(ocupada ? Color.RED : Color.GREEN);
        });
    }

    /**
     * Actualiza el texto del contador de puertas disponibles.
     * @param disponibles El número actual de permisos en el semáforo de puertas.
     */
    public void actualizarPuertas(int disponibles) {
        SwingUtilities.invokeLater(() -> lblPuertas.setText("PUERTAS LIBRES: " + disponibles));
    }
}