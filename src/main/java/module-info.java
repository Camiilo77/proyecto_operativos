module project {
    requires javafx.controls;
    requires javafx.graphics;

    opens project to javafx.graphics;
    opens project.view to javafx.graphics;
    opens project.model to javafx.graphics;
    opens project.controller to javafx.graphics;

    exports project;
    exports project.model;
    exports project.controller;
    exports project.view;
}
