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
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Ventana visual independiente para la demostración de Deadlocks.
 * 
 * Muestra gráficamente dos aviones intentando adquirir dos recursos:
 * - Escenario 1: Orden INVERSO → Deadlock (espera circular visible)
 * - Escenario 2: Orden GLOBAL → Sin deadlock (prevención)
 * 
 * Ventana completamente separada de la simulación del aeropuerto.
 */
public class VentanaDeadlock {

    private final Stage stage;
    private Canvas canvas;
    private Label lblEstado;
    private Label lblResultado;

    // Estado visual de los recursos
    private volatile String duenoRecursoA = null;  // quién tiene el recurso A
    private volatile String duenoRecursoB = null;  // quién tiene el recurso B
    private volatile String avionXEstado = "Inactivo";
    private volatile String avionYEstado = "Inactivo";
    private volatile boolean avionXEsperandoA = false;
    private volatile boolean avionXEsperandoB = false;
    private volatile boolean avionYEsperandoA = false;
    private volatile boolean avionYEsperandoB = false;
    private volatile boolean deadlockDetectado = false;
    private volatile boolean demoEnCurso = false;
    private volatile String faseActual = "Presiona un botón para iniciar";
    private volatile int pasoActual = 0;

    private AnimationTimer timer;

    public VentanaDeadlock() {
        stage = new Stage();
        stage.setTitle("🔒 Demo Visual — Deadlocks");

        VBox root = new VBox(10);
        root.setPadding(new Insets(15));
        root.setStyle("-fx-background-color: #0a0e27;");

        // Título
        Label titulo = new Label("🔒 DEADLOCKS — Visualización");
        titulo.setFont(Font.font("Consolas", FontWeight.BOLD, 18));
        titulo.setTextFill(Color.web("#e74c3c"));

        // Botones
        HBox botones = new HBox(15);
        botones.setAlignment(Pos.CENTER);

        Button btnDeadlock = new Button("▶ Provocar Deadlock (Orden Inverso)");
        btnDeadlock.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-family: Consolas; -fx-font-size: 13; -fx-padding: 8 20; -fx-background-radius: 6; -fx-cursor: hand;");
        btnDeadlock.setOnAction(e -> ejecutarDemo(true));

        Button btnPrevencion = new Button("▶ Prevención (Orden Global)");
        btnPrevencion.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-family: Consolas; -fx-font-size: 13; -fx-padding: 8 20; -fx-background-radius: 6; -fx-cursor: hand;");
        btnPrevencion.setOnAction(e -> ejecutarDemo(false));

        botones.getChildren().addAll(btnDeadlock, btnPrevencion);

        // Canvas
        canvas = new Canvas(700, 420);
        Pane canvasContainer = new Pane(canvas);
        canvasContainer.setStyle("-fx-background-color: #0d1137; -fx-border-color: #1a1a4e; -fx-border-radius: 8; -fx-background-radius: 8;");
        canvas.widthProperty().bind(canvasContainer.widthProperty());
        canvas.heightProperty().bind(canvasContainer.heightProperty());

        // Estado y resultado
        lblEstado = new Label("Presiona un botón para iniciar la demostración");
        lblEstado.setFont(Font.font("Consolas", FontWeight.NORMAL, 13));
        lblEstado.setTextFill(Color.web("#8892b0"));

        lblResultado = new Label("");
        lblResultado.setFont(Font.font("Consolas", FontWeight.BOLD, 14));
        lblResultado.setTextFill(Color.web("#64ffda"));
        lblResultado.setWrapText(true);

        root.getChildren().addAll(titulo, botones, canvasContainer, lblEstado, lblResultado);
        VBox.setVgrow(canvasContainer, Priority.ALWAYS);

        Scene scene = new Scene(root, 750, 600);
        stage.setScene(scene);

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
     * Ejecuta la demo de deadlock visual con aviones y recursos.
     * @param provocarDeadlock true = orden inverso (deadlock), false = orden global (prevención)
     */
    private void ejecutarDemo(boolean provocarDeadlock) {
        if (demoEnCurso) return;

        // Reset
        duenoRecursoA = null;
        duenoRecursoB = null;
        avionXEstado = "Preparándose...";
        avionYEstado = "Preparándose...";
        avionXEsperandoA = false;
        avionXEsperandoB = false;
        avionYEsperandoA = false;
        avionYEsperandoB = false;
        deadlockDetectado = false;
        demoEnCurso = true;
        pasoActual = 0;

        faseActual = provocarDeadlock
                ? "DEADLOCK — Orden inverso de adquisición"
                : "PREVENCIÓN — Orden global (A → B para ambos)";

        Platform.runLater(() -> {
            lblEstado.setText(faseActual);
            lblResultado.setText("");
        });

        Semaphore recursoA = new Semaphore(1);
        Semaphore recursoB = new Semaphore(1);

        Thread avionX = new Thread(() -> {
            try {
                Thread.sleep(500);

                // Avión-X siempre empieza con Recurso-A
                avionXEstado = "Solicitando Recurso-A...";
                avionXEsperandoA = true;
                pasoActual = 1;
                recursoA.acquire();
                duenoRecursoA = "Avión-X";
                avionXEsperandoA = false;
                avionXEstado = "✔ Tiene Recurso-A";
                pasoActual = 2;

                Thread.sleep(1500); // Pausa visual

                // Ahora intenta Recurso-B
                avionXEstado = "Solicitando Recurso-B...";
                avionXEsperandoB = true;
                pasoActual = 3;

                if (provocarDeadlock) {
                    boolean ok = recursoB.tryAcquire(4, TimeUnit.SECONDS);
                    if (!ok) {
                        deadlockDetectado = true;
                        avionXEstado = "⚠ BLOQUEADO — DEADLOCK";
                        Platform.runLater(() -> {
                            lblResultado.setText("⚠ ¡DEADLOCK DETECTADO! Avión-X no pudo obtener Recurso-B (timeout 4s). "
                                    + "Avión-Y tiene B y espera A que tiene X → Espera circular.");
                            lblResultado.setTextFill(Color.web("#e74c3c"));
                        });
                        Thread.sleep(2000);
                        recursoA.release();
                        duenoRecursoA = null;
                        avionXEstado = "Liberó recursos (cleanup)";
                    } else {
                        duenoRecursoB = "Avión-X";
                        avionXEsperandoB = false;
                        avionXEstado = "✔ Tiene A y B";
                        Thread.sleep(800);
                        recursoB.release();
                        duenoRecursoB = null;
                        recursoA.release();
                        duenoRecursoA = null;
                        avionXEstado = "✔ Terminó correctamente";
                    }
                } else {
                    recursoB.acquire();
                    duenoRecursoB = "Avión-X";
                    avionXEsperandoB = false;
                    avionXEstado = "✔ Tiene A y B — trabajando";
                    Thread.sleep(1200);
                    recursoB.release();
                    duenoRecursoB = null;
                    recursoA.release();
                    duenoRecursoA = null;
                    avionXEstado = "✔ Terminó correctamente";
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Avión-X-Visual");

        Thread avionY = new Thread(() -> {
            try {
                Thread.sleep(500);

                if (provocarDeadlock) {
                    // ORDEN INVERSO: B primero, luego A
                    avionYEstado = "Solicitando Recurso-B...";
                    avionYEsperandoB = true;
                    recursoB.acquire();
                    duenoRecursoB = "Avión-Y";
                    avionYEsperandoB = false;
                    avionYEstado = "✔ Tiene Recurso-B";

                    Thread.sleep(1500);

                    avionYEstado = "Solicitando Recurso-A...";
                    avionYEsperandoA = true;

                    boolean ok = recursoA.tryAcquire(4, TimeUnit.SECONDS);
                    if (!ok) {
                        deadlockDetectado = true;
                        avionYEstado = "⚠ BLOQUEADO — DEADLOCK";
                        Thread.sleep(2000);
                        recursoB.release();
                        duenoRecursoB = null;
                        avionYEstado = "Liberó recursos (cleanup)";
                    } else {
                        duenoRecursoA = "Avión-Y";
                        avionYEsperandoA = false;
                        avionYEstado = "✔ Tiene A y B";
                        Thread.sleep(800);
                        recursoA.release();
                        duenoRecursoA = null;
                        recursoB.release();
                        duenoRecursoB = null;
                        avionYEstado = "✔ Terminó correctamente";
                    }
                } else {
                    // MISMO ORDEN: A primero, luego B (prevención)
                    avionYEstado = "Solicitando Recurso-A...";
                    avionYEsperandoA = true;
                    recursoA.acquire();
                    duenoRecursoA = "Avión-Y";
                    avionYEsperandoA = false;
                    avionYEstado = "✔ Tiene Recurso-A";

                    Thread.sleep(500);

                    avionYEstado = "Solicitando Recurso-B...";
                    avionYEsperandoB = true;
                    recursoB.acquire();
                    duenoRecursoB = "Avión-Y";
                    avionYEsperandoB = false;
                    avionYEstado = "✔ Tiene A y B — trabajando";

                    Thread.sleep(1200);
                    recursoB.release();
                    duenoRecursoB = null;
                    recursoA.release();
                    duenoRecursoA = null;
                    avionYEstado = "✔ Terminó correctamente";
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Avión-Y-Visual");

        new Thread(() -> {
            avionX.start();
            avionY.start();
            try {
                avionX.join();
                avionY.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            if (!deadlockDetectado) {
                Platform.runLater(() -> {
                    lblResultado.setText("✔ Ambos aviones completaron sin deadlock. "
                            + "El orden global A → B para ambos hilos eliminó la espera circular.");
                    lblResultado.setTextFill(Color.web("#2ecc71"));
                });
            }
            demoEnCurso = false;
        }, "Demo-Deadlock-Awaiter").start();
    }

    /**
     * Dibuja el estado visual de la demo de deadlock.
     */
    private void dibujar() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double w = canvas.getWidth();
        double h = canvas.getHeight();

        gc.setFill(Color.web("#0d1137"));
        gc.fillRect(0, 0, w, h);

        // Título
        gc.setFill(deadlockDetectado ? Color.web("#e74c3c") : Color.web("#8892b0"));
        gc.setFont(Font.font("Consolas", FontWeight.BOLD, 14));
        gc.fillText(faseActual, 20, 25);

        double centerX = w / 2;
        double centerY = h / 2;

        // ═══ RECURSOS (centro superior e inferior) ═══
        double recursoW = 160;
        double recursoH = 60;

        // Recurso A (arriba-centro)
        double raX = centerX - recursoW / 2;
        double raY = 50;
        dibujarRecurso(gc, raX, raY, recursoW, recursoH, "RECURSO A",
                "Semaphore(1)", duenoRecursoA, "#3498db");

        // Recurso B (abajo-centro)
        double rbX = centerX - recursoW / 2;
        double rbY = h - recursoH - 60;
        dibujarRecurso(gc, rbX, rbY, recursoW, recursoH, "RECURSO B",
                "Semaphore(1)", duenoRecursoB, "#9b59b6");

        // ═══ AVIÓN X (izquierda) ═══
        double avionW = 180;
        double avionH = 130;
        double axX = 20;
        double axY = centerY - avionH / 2;
        dibujarAvion(gc, axX, axY, avionW, avionH, "✈ AVIÓN-X", avionXEstado,
                "#3498db", avionXEsperandoA || avionXEsperandoB,
                duenoRecursoA != null && duenoRecursoA.equals("Avión-X"),
                duenoRecursoB != null && duenoRecursoB.equals("Avión-X"));

        // ═══ AVIÓN Y (derecha) ═══
        double ayX = w - avionW - 20;
        double ayY = centerY - avionH / 2;
        dibujarAvion(gc, ayX, ayY, avionW, avionH, "✈ AVIÓN-Y", avionYEstado,
                "#e74c3c", avionYEsperandoA || avionYEsperandoB,
                duenoRecursoA != null && duenoRecursoA.equals("Avión-Y"),
                duenoRecursoB != null && duenoRecursoB.equals("Avión-Y"));

        // ═══ FLECHAS de conexión ═══
        gc.setLineWidth(2);

        // Avión-X → Recurso A
        dibujarFlecha(gc, axX + avionW, axY + 30, raX, raY + recursoH / 2,
                duenoRecursoA != null && duenoRecursoA.equals("Avión-X") ? "#2ecc71" :
                        avionXEsperandoA ? "#f39c12" : "#2d3436");

        // Avión-X → Recurso B
        dibujarFlecha(gc, axX + avionW, axY + avionH - 30, rbX, rbY + recursoH / 2,
                duenoRecursoB != null && duenoRecursoB.equals("Avión-X") ? "#2ecc71" :
                        avionXEsperandoB ? "#f39c12" : "#2d3436");

        // Avión-Y → Recurso A
        dibujarFlecha(gc, ayX, ayY + 30, raX + recursoW, raY + recursoH / 2,
                duenoRecursoA != null && duenoRecursoA.equals("Avión-Y") ? "#2ecc71" :
                        avionYEsperandoA ? "#f39c12" : "#2d3436");

        // Avión-Y → Recurso B
        dibujarFlecha(gc, ayX, ayY + avionH - 30, rbX + recursoW, rbY + recursoH / 2,
                duenoRecursoB != null && duenoRecursoB.equals("Avión-Y") ? "#2ecc71" :
                        avionYEsperandoB ? "#f39c12" : "#2d3436");

        // ═══ INDICADOR DE DEADLOCK ═══
        if (deadlockDetectado) {
            gc.setFill(Color.web("#e74c3c", 0.1));
            gc.fillRect(0, 0, w, h);

            gc.setFill(Color.web("#e74c3c"));
            gc.setFont(Font.font("Consolas", FontWeight.BOLD, 22));
            gc.fillText("⚠ DEADLOCK — ESPERA CIRCULAR", centerX - 200, centerY);

            gc.setFont(Font.font("Consolas", FontWeight.NORMAL, 12));
            gc.fillText("X tiene A y espera B  |  Y tiene B y espera A", centerX - 170, centerY + 22);
        }

        // Leyenda
        gc.setFill(Color.web("#636e72"));
        gc.setFont(Font.font("Consolas", FontWeight.NORMAL, 10));
        gc.fillText("Verde = adquirido  |  Naranja = esperando  |  Gris = sin relación", 20, h - 10);
    }

    private void dibujarRecurso(GraphicsContext gc, double x, double y, double w, double h,
                                 String nombre, String tipo, String dueno, String color) {
        boolean ocupado = dueno != null;

        gc.setFill(Color.web(ocupado ? color : "#1a1a3e", ocupado ? 0.3 : 1));
        gc.fillRoundRect(x, y, w, h, 10, 10);
        gc.setStroke(Color.web(ocupado ? color : "#636e72"));
        gc.setLineWidth(2);
        gc.strokeRoundRect(x, y, w, h, 10, 10);

        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Consolas", FontWeight.BOLD, 13));
        gc.fillText(nombre, x + 15, y + 22);

        gc.setFill(Color.web("#8892b0"));
        gc.setFont(Font.font("Consolas", FontWeight.NORMAL, 10));
        gc.fillText(tipo, x + 15, y + 38);

        gc.setFill(Color.web(ocupado ? "#f1c40f" : "#636e72"));
        gc.setFont(Font.font("Consolas", FontWeight.BOLD, 11));
        gc.fillText(ocupado ? "→ " + dueno : "LIBRE", x + 15, y + 54);
    }

    private void dibujarAvion(GraphicsContext gc, double x, double y, double w, double h,
                               String nombre, String estado, String color,
                               boolean esperando, boolean tieneA, boolean tieneB) {
        gc.setFill(Color.web("#1a1a3e"));
        gc.fillRoundRect(x, y, w, h, 10, 10);

        if (esperando && !tieneA && !tieneB) {
            gc.setStroke(Color.web("#f39c12"));
            gc.setLineWidth(3);
        } else if (estado.contains("DEADLOCK")) {
            gc.setStroke(Color.web("#e74c3c"));
            gc.setLineWidth(3);
        } else {
            gc.setStroke(Color.web(color, 0.6));
            gc.setLineWidth(1.5);
        }
        gc.strokeRoundRect(x, y, w, h, 10, 10);

        gc.setFill(Color.web(color));
        gc.setFont(Font.font("Consolas", FontWeight.BOLD, 14));
        gc.fillText(nombre, x + 12, y + 25);

        gc.setFill(Color.web("#ccd6f6"));
        gc.setFont(Font.font("Consolas", FontWeight.NORMAL, 10));
        gc.fillText(estado, x + 12, y + 45);

        // Indicadores de recursos
        gc.setFont(Font.font("Consolas", FontWeight.NORMAL, 10));

        gc.setFill(Color.web(tieneA ? "#2ecc71" : "#636e72"));
        gc.fillText("Recurso-A: " + (tieneA ? "✔ TIENE" : "✘ No"), x + 12, y + 75);

        gc.setFill(Color.web(tieneB ? "#2ecc71" : "#636e72"));
        gc.fillText("Recurso-B: " + (tieneB ? "✔ TIENE" : "✘ No"), x + 12, y + 95);

        // Overlay si deadlock
        if (estado.contains("DEADLOCK")) {
            gc.setFill(Color.web("#e74c3c", 0.1));
            gc.fillRoundRect(x, y, w, h, 10, 10);
        }
    }

    private void dibujarFlecha(GraphicsContext gc, double x1, double y1, double x2, double y2, String color) {
        gc.setStroke(Color.web(color));
        gc.setLineWidth(2);
        gc.setLineDashes(null);
        gc.strokeLine(x1, y1, x2, y2);

        // Punta de flecha
        double angle = Math.atan2(y2 - y1, x2 - x1);
        double arrowLen = 10;
        gc.strokeLine(x2, y2, x2 - arrowLen * Math.cos(angle - Math.PI / 6), y2 - arrowLen * Math.sin(angle - Math.PI / 6));
        gc.strokeLine(x2, y2, x2 - arrowLen * Math.cos(angle + Math.PI / 6), y2 - arrowLen * Math.sin(angle + Math.PI / 6));
    }
}
