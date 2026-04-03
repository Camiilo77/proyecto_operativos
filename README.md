
# ✈️ Simulación de Aeropuerto Inteligente - Sistemas Operativos

Este proyecto es una simulación concurrente desarrollada en **Java** que modela el funcionamiento de un aeropuerto. Se enfoca en la gestión de recursos críticos (pistas y puertas de embarque) mediante el uso de **hilos y semáforos**, garantizando la exclusión mutua y evitando condiciones de carrera.

## 📋 Características
* **Concurrencia:** Cada avión es un hilo (`Thread`) independiente.
* **Exclusión Mutua:** Uso de semáforos binarios para asegurar que solo un avión use la pista a la vez.
* **Gestión de Recursos:** Semáforos de conteo para controlar el acceso limitado a las puertas de embarque.
* **Sincronización Compuesta:** Los aviones solo aterrizan si tienen una puerta reservada y la pista despejada.
* **Interfaz Gráfica:** Visualización en tiempo real mediante **Java Swing**.

## 🛠️ Tecnologías y Dependencias
* **Lenguaje:** Java 17+
* **Gestor de Dependencias:** Maven
* **Librerías Críticas:**
  * `java.util.concurrent.Semaphore`
  * `java.lang.Thread`
  * `javax.swing`



## 🏗️ Estructura del Proyecto
```text
src/main/java/project/
├── Main.java              # Clase orquestadora (Punto de entrada)
├── control/
│   └── Aeropuerto.java    # Controlador de recursos y semáforos
├── model/
│   └── Avion.java         # Definición del hilo y ciclo de vida
└── view/
    └── InterfazAeropuerto.java # Interfaz gráfica (Swing)
```

## 🚀 Instalación y Ejecución

1. **Clonar el repositorio:**
   ```bash
   git clone https://github.com/tu-usuario/aeropuerto-concurrente.git
   ```

2. **Compilar con Maven:**
   ```bash
   mvn clean compile
   ```

3. **Ejecutar la aplicación:**
   Ejecuta la clase `Main.java` desde tu IDE (IntelliJ/Eclipse) o mediante:
   ```bash
   mvn exec:java -Dexec.mainClass="project.Main"
   ```

## 🛡️ Prevención de Deadlocks
Para evitar interbloqueos, el sistema implementa una **estrategia de orden global de asignación de recursos**. Un hilo de avión debe adquirir obligatoriamente el permiso de la **Puerta de Embarque** antes de intentar adquirir el permiso de la **Pista**. Esto garantiza que ningún avión quede bloqueado en la pista sin tener un lugar donde estacionar.

## 👥 Autores
* **Camilo Rodriguez** - *Sistemas Operativos - UPTC*
* **Cristian Eslava** - *Sistemas Operativos - UPTC*

---
