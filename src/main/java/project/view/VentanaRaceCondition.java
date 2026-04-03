package project.view;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.util.concurrent.Semaphore;

/**
 * Ventana visual independiente para la demostración de Condiciones de Carrera.
 * 
 * Muestra gráficamente dos hilos compitiendo por modificar un contador compartido:
 * - Escenario 1: SIN protección → resultado incorrecto (race condition visible)
 * - Escenario 2: CON semáforo binario → resultado correcto
 * 
 * Esta ventana es completamente independiente de la simulación principal del aeropuerto.
 */
public class VentanaRaceCondition {

    private final Stage stage;
    private Canvas canvas;
    private Label lblEstado;
    private Label lblResultado;

    // Estado de la simulación visual
    private volatile int contadorCompartido = 0;
    private volatile int progresoHiloA = 0;
    private volatile int progresoHiloB = 0;
    private volatile boolean hiloAActivo = false;
    private volatile boolean hiloBActivo = false;
    private volatile boolean hiloAEnSeccionCritica = false;
    private volatile boolean hiloBEnSeccionCritica = false;
    private volatile boolean demoEnCurso = false;
    private volatile boolean conProteccion = false;
    private volatile String faseActual = "Presiona un botón para iniciar";
    private volatile int resultadoEsperado = 0;

    private static final int ITERACIONES = 500;
    private AnimationTimer timer;

    public VentanaRaceCondition() {
        stage = new Stage();
        stage.setTitle("⚠ Demo Visual — Condiciones de Carrera");

        VBox root = new VBox(10);
        root.setPadding(new Insets(15));
        root.setStyle("-fx-background-color: #0a0e27;");

        // Título
        Label titulo = new Label("⚠ CONDICIONES DE CARRERA — Visualización");
        titulo.setFont(Font.font("Consolas", FontWeight.BOLD, 18));
        titulo.setTextFill(Color.web("#f39c12"));

        // Botones
        HBox botones = new HBox(15);
        botones.setAlignment(Pos.CENTER);

        Button btnSinProteccion = new Button("▶ Sin Protección (Race Condition)");
        btnSinProteccion.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-family: Consolas; -fx-font-size: 13; -fx-padding: 8 20; -fx-background-radius: 6; -fx-cursor: hand;");
        btnSinProteccion.setOnAction(e -> ejecutarDemo(false));

        Button btnConProteccion = new Button("▶ Con Semáforo (Protegido)");
        btnConProteccion.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-family: Consolas; -fx-font-size: 13; -fx-padding: 8 20; -fx-background-radius: 6; -fx-cursor: hand;");
        btnConProteccion.setOnAction(e -> ejecutarDemo(true));

        botones.getChildren().addAll(btnSinProteccion, btnConProteccion);

        // Canvas
        canvas = new Canvas(700, 400);
        Pane canvasContainer = new Pane(canvas);
        canvasContainer.setStyle("-fx-background-color: #0d1137; -fx-border-color: #1a1a4e; -fx-border-radius: 8; -fx-background-radius: 8;");
        canvas.widthProperty().bind(canvasContainer.widthProperty());
        canvas.heightProperty().bind(canvasContainer.heightProperty());

        // Estado y resultado
        lblEstado = new Label("Presiona un botón para iniciar la demostración");
        lblEstado.setFont(Font.font("Consolas", FontWeight.NORMAL, 13));
        lblEstado.setTextFill(Color.web("#8892b0"));

        lblResultado = new Label("");
        lblResultado.setFont(Font.font("Consolas", FontWeight.BOLD, 15));
        lblResultado.setTextFill(Color.web("#64ffda"));

        root.getChildren().addAll(titulo, botones, canvasContainer, lblEstado, lblResultado);
        VBox.setVgrow(canvasContainer, javafx.scene.layout.Priority.ALWAYS);

        Scene scene = new Scene(root, 750, 550);
        stage.setScene(scene);

        // Iniciar render loop
        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                dibujar();
            }
        };
        timer.start();

        stage.setOnCloseRequest(e -> timer.stop());
    }

    public void mostrar() {
        stage.show();
        stage.toFront();
    }

    /**
     * Ejecuta la demo visual: los hilos van más lento para que se vea la animación.
     */
    private void ejecutarDemo(boolean protegido) {
        if (demoEnCurso) return;

        // Reset
        contadorCompartido = 0;
        progresoHiloA = 0;
        progresoHiloB = 0;
        hiloAActivo = false;
        hiloBActivo = false;
        hiloAEnSeccionCritica = false;
        hiloBEnSeccionCritica = false;
        conProteccion = protegido;
        demoEnCurso = true;
        resultadoEsperado = ITERACIONES * 2;

        faseActual = protegido
                ? "CON PROTECCIÓN — Semaphore(1) como mutex"
                : "SIN PROTECCIÓN — ¡Race condition activa!";

        Platform.runLater(() -> {
            lblEstado.setText(faseActual);
            lblResultado.setText("");
        });

        Semaphore mutex = protegido ? new Semaphore(1) : null;

        // Hilo A
        Thread hiloA = new Thread(() -> {
            hiloAActivo = true;
            for (int i = 0; i < ITERACIONES; i++) {
                try {
                    if (mutex != null) {
                        mutex.acquire();
                        hiloAEnSeccionCritica = true;
                    }

                    // Operación NO atómica: leer + incrementar + escribir
                    hiloAEnSeccionCritica = true;
                    int temp = contadorCompartido;
                    Thread.sleep(1); // Pausa para hacer visible la race condition
                    contadorCompartido = temp + 1;
                    progresoHiloA = i + 1;
                    hiloAEnSeccionCritica = false;

                    if (mutex != null) {
                        mutex.release();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            hiloAActivo = false;
        }, "Hilo-A-Visual");

        // Hilo B
        Thread hiloB = new Thread(() -> {
            hiloBActivo = true;
            for (int i = 0; i < ITERACIONES; i++) {
                try {
                    if (mutex != null) {
                        mutex.acquire();
                        hiloBEnSeccionCritica = true;
                    }

                    hiloBEnSeccionCritica = true;
                    int temp = contadorCompartido;
                    Thread.sleep(1);
                    contadorCompartido = temp + 1;
                    progresoHiloB = i + 1;
                    hiloBEnSeccionCritica = false;

                    if (mutex != null) {
                        mutex.release();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            hiloBActivo = false;
        }, "Hilo-B-Visual");

        // Lanzar ambos y esperar resultado en hilo aparte
        new Thread(() -> {
            hiloA.start();
            hiloB.start();
            try {
                hiloA.join();
                hiloB.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            int perdidas = resultadoEsperado - contadorCompartido;
            Platform.runLater(() -> {
                if (perdidas > 0) {
                    lblResultado.setText("⚠ Resultado: " + contadorCompartido + " / " + resultadoEsperado
                            + " — ¡" + perdidas + " operaciones PERDIDAS por Race Condition!");
                    lblResultado.setTextFill(Color.web("#e74c3c"));
                } else {
                    lblResultado.setText("✔ Resultado: " + contadorCompartido + " / " + resultadoEsperado
                            + " — Correcto. El semáforo protegió la sección crítica.");
                    lblResultado.setTextFill(Color.web("#2ecc71"));
                }
            });
            demoEnCurso = false;
        }, "Demo-Awaiter").start();
    }

    /**
     * Dibuja el estado visual de la carrera en el canvas.
     */
    private void dibujar() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double w = canvas.getWidth();
        double h = canvas.getHeight();

        // Fondo
        gc.setFill(Color.web("#0d1137"));
        gc.fillRect(0, 0, w, h);

        // Título del modo
        gc.setFill(conProteccion ? Color.web("#2ecc71") : Color.web("#e74c3c"));
        gc.setFont(Font.font("Consolas", FontWeight.BOLD, 14));
        gc.fillText(faseActual, 20, 25);

        double centerX = w / 2;

        // ═══ CONTADOR COMPARTIDO (centro) ═══
        double contadorW = 180;
        double contadorH = 70;
        double contadorX = centerX - contadorW / 2;
        double contadorY = 50;

        gc.setFill(Color.web("#1a1a3e"));
        gc.fillRoundRect(contadorX, contadorY, contadorW, contadorH, 10, 10);
        gc.setStroke(Color.web("#f39c12"));
        gc.setLineWidth(2);
        gc.strokeRoundRect(contadorX, contadorY, contadorW, contadorH, 10, 10);

        gc.setFill(Color.web("#8892b0"));
        gc.setFont(Font.font("Consolas", FontWeight.NORMAL, 11));
        gc.fillText("RECURSO COMPARTIDO", contadorX + 15, contadorY + 18);

        gc.setFill(Color.web("#f1c40f"));
        gc.setFont(Font.font("Consolas", FontWeight.BOLD, 28));
        gc.fillText(String.valueOf(contadorCompartido), contadorX + contadorW / 2 - 20, contadorY + 55);

        // ═══ SEMÁFORO (si aplica) ═══
        if (conProteccion) {
            double semX = centerX - 60;
            double semY = contadorY + contadorH + 10;
            gc.setFill(Color.web("#2ecc71", 0.2));
            gc.fillRoundRect(semX, semY, 120, 28, 6, 6);
            gc.setStroke(Color.web("#2ecc71"));
            gc.setLineWidth(1);
            gc.strokeRoundRect(semX, semY, 120, 28, 6, 6);
            gc.setFill(Color.web("#2ecc71"));
            gc.setFont(Font.font("Consolas", FontWeight.BOLD, 11));
            gc.fillText("🔒 Semaphore(1)", semX + 8, semY + 18);
        }

        double hiloY = 170;
        double hiloW = w / 2 - 40;
        double hiloH = h - hiloY - 20;

        // ═══ HILO A (izquierda) ═══
        dibujarHilo(gc, 20, hiloY, hiloW, hiloH,
                "HILO A", "#3498db", hiloAActivo, hiloAEnSeccionCritica,
                progresoHiloA, ITERACIONES);

        // ═══ HILO B (derecha) ═══
        dibujarHilo(gc, centerX + 20, hiloY, hiloW, hiloH,
                "HILO B", "#e74c3c", hiloBActivo, hiloBEnSeccionCritica,
                progresoHiloB, ITERACIONES);

        // Flechas hacia el contador
        gc.setStroke(Color.web("#f39c12", 0.5));
        gc.setLineWidth(1.5);
        gc.setLineDashes(5, 4);
        // Flecha A
        gc.strokeLine(20 + hiloW / 2, hiloY, contadorX + 20, contadorY + contadorH);
        // Flecha B
        gc.strokeLine(centerX + 20 + hiloW / 2, hiloY, contadorX + contadorW - 20, contadorY + contadorH);
        gc.setLineDashes(null);

        // Resultado esperado
        gc.setFill(Color.web("#8892b0"));
        gc.setFont(Font.font("Consolas", FontWeight.NORMAL, 11));
        gc.fillText("Esperado: " + resultadoEsperado + "  |  Real: " + contadorCompartido, 20, h - 5);
    }

    /**
     * Dibuja la representación visual de un hilo.
     */
    private void dibujarHilo(GraphicsContext gc, double x, double y, double w, double h,
                              String nombre, String color, boolean activo,
                              boolean enSeccionCritica, int progreso, int total) {
        // Fondo del hilo
        gc.setFill(Color.web("#1a1a3e"));
        gc.fillRoundRect(x, y, w, h, 10, 10);

        // Borde: cambia de color si está en sección crítica
        if (enSeccionCritica) {
            gc.setStroke(Color.web("#f39c12"));
            gc.setLineWidth(3);
        } else {
            gc.setStroke(Color.web(color, 0.6));
            gc.setLineWidth(1.5);
        }
        gc.strokeRoundRect(x, y, w, h, 10, 10);

        // Nombre
        gc.setFill(Color.web(color));
        gc.setFont(Font.font("Consolas", FontWeight.BOLD, 14));
        gc.fillText(nombre, x + 15, y + 25);

        // Estado
        String estado;
        String estadoColor;
        if (!activo && progreso == 0) {
            estado = "⏸ Esperando";
            estadoColor = "#636e72";
        } else if (enSeccionCritica) {
            estado = "🔴 EN SECCIÓN CRÍTICA";
            estadoColor = "#f39c12";
        } else if (activo) {
            estado = "▶ Ejecutando";
            estadoColor = "#2ecc71";
        } else {
            estado = "✔ Terminado";
            estadoColor = "#2ecc71";
        }
        gc.setFill(Color.web(estadoColor));
        gc.setFont(Font.font("Consolas", FontWeight.NORMAL, 11));
        gc.fillText(estado, x + 15, y + 45);

        // Barra de progreso
        double barX = x + 15;
        double barY = y + 60;
        double barW = w - 30;
        double barH = 20;

        gc.setFill(Color.web("#2d3436"));
        gc.fillRoundRect(barX, barY, barW, barH, 5, 5);

        double progW = (barW * progreso) / Math.max(total, 1);
        gc.setFill(Color.web(color));
        gc.fillRoundRect(barX, barY, progW, barH, 5, 5);

        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Consolas", FontWeight.BOLD, 10));
        gc.fillText(progreso + " / " + total, barX + 5, barY + 14);

        // Operación que ejecuta
        gc.setFill(Color.web("#8892b0"));
        gc.setFont(Font.font("Consolas", FontWeight.NORMAL, 10));
        gc.fillText("Operación: contador++", x + 15, y + 105);
        gc.fillText("(leer → sumar → escribir)", x + 15, y + 120);

        // Indicador de sección crítica
        if (enSeccionCritica) {
            gc.setFill(Color.web("#f39c12", 0.15));
            gc.fillRoundRect(x + 5, y + 5, w - 10, h - 10, 8, 8);
        }
    }
}
