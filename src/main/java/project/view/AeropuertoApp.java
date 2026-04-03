package project.view;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import project.controller.Aeropuerto;
import project.model.*;

import java.util.List;

/**
 * Aplicación principal JavaFX para la visualización del aeropuerto.
 * 
 * Layout:
 * ┌───────────────────────────────────────────────┐
 * │ TOP: Barra de controles (botones de acción)    │
 * ├──────────────────────────┬────────────────────┤
 * │ CENTER: Canvas visual    │ RIGHT: Stats       │
 * │ (pistas, puertas,       │ (contadores,       │
 * │  aviones animados)      │  estado general)   │
 * ├──────────────────────────┴────────────────────┤
 * │ BOTTOM: Log de eventos en tiempo real          │
 * └───────────────────────────────────────────────┘
 */
public class AeropuertoApp extends Application {

    // Configuración del aeropuerto
    private static final int NUM_PISTAS = 3;
    private static final int NUM_PUERTAS = 5;
    private static final int AVIONES_INICIALES = 10;

    private Aeropuerto aeropuerto;
    private Canvas canvas;
    private ListView<String> logListView;
    private ObservableList<String> logItems;

    // Labels de estadísticas
    private Label lblAvionesAtendidos;
    private Label lblAvionesEnEspera;
    private Label lblPuertasDisponibles;
    private Label lblPistasEstado;

    private int contadorAviones = 0;

    @Override
    public void start(Stage primaryStage) {
        // Inicializar controlador
        aeropuerto = new Aeropuerto(NUM_PISTAS, NUM_PUERTAS);

        // Construir la interfaz
        BorderPane root = new BorderPane();
        root.getStyleClass().add("root-pane");

        root.setTop(crearBarraControles());
        root.setCenter(crearPanelCentral());
        root.setRight(crearPanelEstadisticas());
        root.setBottom(crearPanelLog());

        Scene scene = new Scene(root, 1200, 800);

        // Cargar CSS
        String cssPath = getClass().getResource("/styles/EstilosAeropuerto.css") != null
                ? getClass().getResource("/styles/EstilosAeropuerto.css").toExternalForm()
                : null;
        if (cssPath != null) {
            scene.getStylesheets().add(cssPath);
        }

        primaryStage.setTitle("✈ Aeropuerto Inteligente — UPTC Sistemas Operativos");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(1000);
        primaryStage.setMinHeight(700);
        primaryStage.show();

        // Configurar listener de log
        aeropuerto.getRegistro().agregarListener(evento ->
            Platform.runLater(() -> {
                logItems.add(evento.toString());
                logListView.scrollTo(logItems.size() - 1);
            })
        );

        // Iniciar loop de renderizado del canvas
        iniciarRenderloop();
    }

    /**
     * Crea la barra superior con botones de control.
     */
    private HBox crearBarraControles() {
        HBox toolbar = new HBox(15);
        toolbar.setPadding(new Insets(15, 20, 15, 20));
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.getStyleClass().add("toolbar");

        // Título
        Label titulo = new Label("✈ TORRE DE CONTROL");
        titulo.getStyleClass().add("toolbar-title");

        // Separador visual
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Botones
        Button btnIniciar = crearBoton("▶ Iniciar Simulación", "btn-primary");
        btnIniciar.setOnAction(e -> iniciarSimulacion());

        Button btnAgregar = crearBoton("✚ Agregar Avión", "btn-secondary");
        btnAgregar.setOnAction(e -> agregarAvion());

        Button btnRaceCondition = crearBoton("⚠ Race Condition", "btn-warning");
        btnRaceCondition.setOnAction(e -> {
            VentanaRaceCondition ventana = new VentanaRaceCondition();
            ventana.mostrar();
        });

        Button btnDeadlock = crearBoton("🔒 Deadlock Demo", "btn-danger");
        btnDeadlock.setOnAction(e -> {
            VentanaDeadlock ventana = new VentanaDeadlock();
            ventana.mostrar();
        });

        Button btnLimpiarLog = crearBoton("🗑 Limpiar Log", "btn-neutral");
        btnLimpiarLog.setOnAction(e -> logItems.clear());

        toolbar.getChildren().addAll(titulo, spacer, btnIniciar, btnAgregar,
                btnRaceCondition, btnDeadlock, btnLimpiarLog);

        return toolbar;
    }

    /**
     * Crea el panel central con el canvas de visualización.
     */
    private Pane crearPanelCentral() {
        canvas = new Canvas(750, 450);
        // Usar Pane en vez de StackPane: Canvas no participa en el layout de Pane,
        // evitando el ciclo infinito de resize (layout feedback loop).
        Pane container = new Pane(canvas);
        container.getStyleClass().add("canvas-container");

        // Bind seguro: el canvas sigue el tamaño del contenedor sin causar
        // retroalimentación de layout porque Pane ignora el tamaño de Canvas
        // para calcular su propio tamaño.
        canvas.widthProperty().bind(container.widthProperty());
        canvas.heightProperty().bind(container.heightProperty());

        return container;
    }

    /**
     * Crea el panel derecho con estadísticas del aeropuerto.
     */
    private VBox crearPanelEstadisticas() {
        VBox stats = new VBox(12);
        stats.setPadding(new Insets(15));
        stats.setPrefWidth(250);
        stats.getStyleClass().add("stats-panel");

        Label titulo = new Label("📊 ESTADÍSTICAS");
        titulo.getStyleClass().add("stats-title");

        lblAvionesAtendidos = crearLabelEstadistica("Atendidos: 0");
        lblAvionesEnEspera = crearLabelEstadistica("En espera: 0");
        lblPuertasDisponibles = crearLabelEstadistica("Puertas libres: " + NUM_PUERTAS);
        lblPistasEstado = crearLabelEstadistica("Pistas libres: " + NUM_PISTAS);

        // Leyenda de estados
        Label leyendaTitulo = new Label("── LEYENDA ──");
        leyendaTitulo.getStyleClass().add("leyenda-title");

        VBox leyenda = new VBox(5);
        for (EstadoAvion estado : EstadoAvion.values()) {
            HBox fila = new HBox(8);
            fila.setAlignment(Pos.CENTER_LEFT);
            Region colorBox = new Region();
            colorBox.setPrefSize(14, 14);
            colorBox.setMinSize(14, 14);
            colorBox.setStyle("-fx-background-color: " + estado.getColorHex() + "; -fx-background-radius: 3;");
            Label lbl = new Label(estado.getDescripcion());
            lbl.getStyleClass().add("leyenda-text");
            fila.getChildren().addAll(colorBox, lbl);
            leyenda.getChildren().add(fila);
        }

        // Info de sincronización
        Label syncTitle = new Label("── SINCRONIZACIÓN ──");
        syncTitle.getStyleClass().add("leyenda-title");

        Label syncInfo = new Label(
                "• Pistas: Semáforo binario\n" +
                "• Puertas: Semáforo conteo\n" +
                "• Asignación: ReentrantLock\n" +
                "• Orden: Puerta → Pista"
        );
        syncInfo.getStyleClass().add("sync-info");

        stats.getChildren().addAll(titulo,
                new Separator(), lblAvionesAtendidos, lblAvionesEnEspera,
                lblPuertasDisponibles, lblPistasEstado,
                new Separator(), leyendaTitulo, leyenda,
                new Separator(), syncTitle, syncInfo);

        return stats;
    }

    /**
     * Crea el panel inferior con el log de eventos.
     */
    private VBox crearPanelLog() {
        VBox logPanel = new VBox(5);
        logPanel.setPadding(new Insets(10, 15, 15, 15));
        logPanel.getStyleClass().add("log-panel");
        logPanel.setPrefHeight(200);

        Label titulo = new Label("📜 REGISTRO DE EVENTOS CONCURRENTES");
        titulo.getStyleClass().add("log-title");

        logItems = FXCollections.observableArrayList();
        logListView = new ListView<>(logItems);
        logListView.getStyleClass().add("log-list");
        logListView.setPrefHeight(160);
        VBox.setVgrow(logListView, Priority.ALWAYS);

        logPanel.getChildren().addAll(titulo, logListView);
        return logPanel;
    }

    /**
     * Inicia la simulación lanzando N aviones con retraso escalonado.
     */
    private void iniciarSimulacion() {
        new Thread(() -> {
            for (int i = 0; i < AVIONES_INICIALES; i++) {
                agregarAvion();
                try {
                    Thread.sleep(800); // Llegadas escalonadas
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "Lanzador").start();
    }

    /**
     * Agrega un nuevo avión al sistema.
     */
    private void agregarAvion() {
        contadorAviones++;
        String id = "Avión-" + contadorAviones;
        Avion avion = new Avion(id, aeropuerto);
        aeropuerto.registrarAvion(avion);
        avion.start();
    }

    /**
     * Inicia el loop de renderizado del canvas que se ejecuta a ~60 FPS.
     * Dibuja el estado actual del aeropuerto en cada frame.
     */
    private void iniciarRenderloop() {
        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                dibujarAeropuerto();
                actualizarEstadisticas();
            }
        };
        timer.start();
    }

    /**
     * Dibuja todo el estado visual del aeropuerto en el canvas.
     */
    private void dibujarAeropuerto() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double w = canvas.getWidth();
        double h = canvas.getHeight();

        // Fondo con gradiente oscuro
        gc.setFill(Color.web("#0a0e27"));
        gc.fillRect(0, 0, w, h);

        // Dibujar título del área visual
        gc.setFill(Color.web("#64ffda"));
        gc.setFont(Font.font("Consolas", FontWeight.BOLD, 16));
        gc.fillText("VISTA AÉREA DEL AEROPUERTO", 20, 30);

        // Dibujar pistas
        dibujarPistas(gc, w, h);

        // Dibujar puertas de embarque
        dibujarPuertas(gc, w, h);

        // Dibujar cola de espera
        dibujarColaEspera(gc, w, h);
    }

    /**
     * Dibuja las pistas de aterrizaje con su estado visual.
     */
    private void dibujarPistas(GraphicsContext gc, double w, double h) {
        Pista[] pistas = aeropuerto.getPistas();
        double pistaWidth = (w - 80) / pistas.length;
        double pistaHeight = 100;
        double startY = 60;

        gc.setFill(Color.web("#1a1a3e"));
        gc.fillRoundRect(10, 45, w - 20, pistaHeight + 30, 10, 10);

        gc.setFill(Color.web("#8892b0"));
        gc.setFont(Font.font("Consolas", FontWeight.BOLD, 12));
        gc.fillText("PISTAS DE ATERRIZAJE / DESPEGUE (Semáforo Binario)", 20, startY - 3);

        for (int i = 0; i < pistas.length; i++) {
            double x = 20 + i * (pistaWidth + 10);

            // Fondo de pista
            boolean ocupada = pistas[i].isOcupada();
            if (ocupada) {
                gc.setFill(Color.web("#e74c3c", 0.3));
            } else {
                gc.setFill(Color.web("#2ecc71", 0.2));
            }
            gc.fillRoundRect(x, startY, pistaWidth - 10, pistaHeight, 8, 8);

            // Borde
            gc.setStroke(ocupada ? Color.web("#e74c3c") : Color.web("#2ecc71"));
            gc.setLineWidth(2);
            gc.strokeRoundRect(x, startY, pistaWidth - 10, pistaHeight, 8, 8);

            // Línea central (marcación de pista)
            gc.setStroke(Color.web("#ffffff", 0.4));
            gc.setLineWidth(1);
            gc.setLineDashes(8, 6);
            gc.strokeLine(x + 15, startY + pistaHeight / 2, x + pistaWidth - 25, startY + pistaHeight / 2);
            gc.setLineDashes(null);

            // Nombre de pista
            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("Consolas", FontWeight.BOLD, 13));
            gc.fillText(pistas[i].getNombre(), x + 10, startY + 20);

            // Estado
            String estado = ocupada ? "OCUPADA" : "LIBRE";
            gc.setFill(ocupada ? Color.web("#e74c3c") : Color.web("#2ecc71"));
            gc.setFont(Font.font("Consolas", FontWeight.NORMAL, 11));
            gc.fillText(estado, x + 10, startY + 40);

            // Avión usando la pista
            if (pistas[i].getAvionActual() != null) {
                gc.setFill(Color.web("#f39c12"));
                gc.setFont(Font.font("Consolas", FontWeight.BOLD, 12));
                gc.fillText("✈ " + pistas[i].getAvionActual(), x + 10, startY + 60);
            }

            // Indicador semáforo
            double indicadorX = x + pistaWidth - 30;
            gc.setFill(ocupada ? Color.web("#e74c3c") : Color.web("#2ecc71"));
            gc.fillOval(indicadorX, startY + 10, 12, 12);
        }
    }

    /**
     * Dibuja las puertas de embarque con aviones asignados.
     */
    private void dibujarPuertas(GraphicsContext gc, double w, double h) {
        PuertaEmbarque[] puertas = aeropuerto.getPuertas();
        double startY = 190;
        double puertaWidth = (w - 80) / puertas.length;
        double puertaHeight = 110;

        gc.setFill(Color.web("#1a1a3e"));
        gc.fillRoundRect(10, startY - 15, w - 20, puertaHeight + 35, 10, 10);

        gc.setFill(Color.web("#8892b0"));
        gc.setFont(Font.font("Consolas", FontWeight.BOLD, 12));
        gc.fillText("PUERTAS DE EMBARQUE (Semáforo de Conteo: máx " + puertas.length + ")", 20, startY);

        for (int i = 0; i < puertas.length; i++) {
            double x = 20 + i * (puertaWidth + 5);
            double y = startY + 12;

            boolean ocupada = puertas[i].isOcupada();

            // Fondo
            if (ocupada) {
                gc.setFill(Color.web("#3498db", 0.3));
            } else {
                gc.setFill(Color.web("#2d3436", 0.5));
            }
            gc.fillRoundRect(x, y, puertaWidth - 10, puertaHeight - 20, 8, 8);

            // Borde
            gc.setStroke(ocupada ? Color.web("#3498db") : Color.web("#636e72"));
            gc.setLineWidth(1.5);
            gc.strokeRoundRect(x, y, puertaWidth - 10, puertaHeight - 20, 8, 8);

            // Nombre
            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("Consolas", FontWeight.BOLD, 11));
            gc.fillText(puertas[i].getNombre(), x + 5, y + 18);

            // Estado
            gc.setFill(ocupada ? Color.web("#3498db") : Color.web("#636e72"));
            gc.setFont(Font.font("Consolas", FontWeight.NORMAL, 10));
            gc.fillText(ocupada ? "OCUPADA" : "LIBRE", x + 5, y + 35);

            // Avión asignado
            if (puertas[i].getAvionAsignado() != null) {
                gc.setFill(Color.web("#f1c40f"));
                gc.setFont(Font.font("Consolas", FontWeight.BOLD, 11));
                gc.fillText("✈", x + (puertaWidth - 10) / 2 - 6, y + 58);
                gc.setFont(Font.font("Consolas", FontWeight.NORMAL, 9));
                gc.setFill(Color.web("#ecf0f1"));
                gc.fillText(puertas[i].getAvionAsignado(), x + 5, y + 75);
            }
        }
    }

    /**
     * Dibuja la cola de aviones esperando y aviones activos por estado.
     */
    private void dibujarColaEspera(GraphicsContext gc, double w, double h) {
        double startY = 330;

        gc.setFill(Color.web("#1a1a3e"));
        gc.fillRoundRect(10, startY - 15, w - 20, h - startY + 5, 10, 10);

        gc.setFill(Color.web("#8892b0"));
        gc.setFont(Font.font("Consolas", FontWeight.BOLD, 12));
        gc.fillText("AVIONES EN EL SISTEMA (cada avión = 1 hilo)", 20, startY);

        List<Avion> aviones = aeropuerto.getAvionesActivos();
        double x = 20;
        double y = startY + 15;
        double avionWidth = 120;
        double avionHeight = 35;
        int col = 0;
        int maxCols = Math.max(1, (int) ((w - 40) / (avionWidth + 8)));

        for (Avion avion : aviones) {
            EstadoAvion estado = avion.getEstado();
            if (estado == EstadoAvion.PARTIO) continue;

            double ax = 20 + col * (avionWidth + 8);
            double ay = y;

            // Fondo del avión con color del estado
            gc.setFill(Color.web(estado.getColorHex(), 0.25));
            gc.fillRoundRect(ax, ay, avionWidth, avionHeight, 6, 6);
            gc.setStroke(Color.web(estado.getColorHex()));
            gc.setLineWidth(1.2);
            gc.strokeRoundRect(ax, ay, avionWidth, avionHeight, 6, 6);

            // Nombre del avión
            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("Consolas", FontWeight.BOLD, 10));
            gc.fillText("✈ " + avion.getIdAvion(), ax + 5, ay + 14);

            // Estado
            gc.setFill(Color.web(estado.getColorHex()));
            gc.setFont(Font.font("Consolas", FontWeight.NORMAL, 9));
            gc.fillText(estado.getDescripcion(), ax + 5, ay + 28);

            col++;
            if (col >= maxCols) {
                col = 0;
                y += avionHeight + 5;
            }
        }
    }

    /**
     * Actualiza los labels de estadísticas.
     */
    private void actualizarEstadisticas() {
        lblAvionesAtendidos.setText("✔ Atendidos: " + aeropuerto.getAvionesAtendidos());
        lblAvionesEnEspera.setText("⏳ En espera: " + aeropuerto.getAvionesEnEspera());
        lblPuertasDisponibles.setText("🚪 Puertas libres: " + aeropuerto.getPuertasDisponibles());

        // Contar pistas libres
        int pistasLibres = 0;
        for (Pista p : aeropuerto.getPistas()) {
            if (!p.isOcupada()) pistasLibres++;
        }
        lblPistasEstado.setText("🛬 Pistas libres: " + pistasLibres + "/" + aeropuerto.getNumPistas());
    }

    // --- Helpers de UI ---

    private Button crearBoton(String texto, String styleClass) {
        Button btn = new Button(texto);
        btn.getStyleClass().add(styleClass);
        return btn;
    }

    private Label crearLabelEstadistica(String texto) {
        Label lbl = new Label(texto);
        lbl.getStyleClass().add("stat-label");
        return lbl;
    }

    /**
     * Punto de entrada de la aplicación JavaFX.
     */
    public static void main(String[] args) {
        launch(args);
    }
}
