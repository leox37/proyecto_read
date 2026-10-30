package app_read.controlador;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Collection;
import java.util.Objects;

public class ColeccionesController {
    @FXML
    private TextField campo_coleccion;
    @FXML
    private Button boton_crear_coleccion;
    @FXML
    private ComboBox<String> combo_colecciones;
    @FXML
    private Button boton_ok;

    private CreateAction createAction;
    private SelectionAction selectionAction;

    @FXML
    private void initialize() {
        boton_crear_coleccion.setOnAction(event -> crearColeccion());
        boton_ok.setOnAction(event -> seleccionarColeccion());
    }

    public void configurar(Collection<String> colecciones,
                           CreateAction createAction,
                           SelectionAction selectionAction) {
        this.createAction = Objects.requireNonNull(createAction);
        this.selectionAction = Objects.requireNonNull(selectionAction);
        combo_colecciones.getItems().setAll(colecciones);
        if (!colecciones.isEmpty()) {
            combo_colecciones.getSelectionModel().selectFirst();
        }
    }

    private void crearColeccion() {
        String nombre = campo_coleccion.getText().trim();
        if (nombre.isEmpty()) {
            mostrarAviso("Escribe un nombre para la colección.");
            return;
        }

        try {
            createAction.crear(nombre);
            if (!combo_colecciones.getItems().contains(nombre)) {
                combo_colecciones.getItems().add(nombre);
            }
            combo_colecciones.getSelectionModel().select(nombre);
            campo_coleccion.clear();
        } catch (IOException exception) {
            mostrarError(exception.getMessage());
        }
    }

    private void seleccionarColeccion() {
        String seleccionada = combo_colecciones.getValue();
        if (seleccionada == null) {
            mostrarAviso("Crea o selecciona una colección.");
            return;
        }

        try {
            selectionAction.seleccionar(seleccionada);
            ((Stage) boton_ok.getScene().getWindow()).close();
        } catch (IOException exception) {
            mostrarError(exception.getMessage());
        }
    }

    private void mostrarAviso(String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.INFORMATION, mensaje, ButtonType.OK);
        alerta.setTitle("Colecciones");
        alerta.setHeaderText(null);
        alerta.initOwner(boton_ok.getScene().getWindow());
        alerta.showAndWait();
    }

    private void mostrarError(String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.ERROR, mensaje, ButtonType.OK);
        alerta.setTitle("No se pudo guardar la colección");
        alerta.setHeaderText(null);
        alerta.initOwner(boton_ok.getScene().getWindow());
        alerta.showAndWait();
    }

    @FunctionalInterface
    public interface CreateAction {
        void crear(String nombre) throws IOException;
    }

    @FunctionalInterface
    public interface SelectionAction {
        void seleccionar(String nombre) throws IOException;
    }
}
