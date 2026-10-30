package app_read.controlador;

import app_read.modelo.Libro;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Objects;

public class EditarLibroController {
    @FXML
    private TextField campo_titulo;
    @FXML
    private TextField campo_autor;
    @FXML
    private TextField campo_nueva_serie;
    @FXML
    private ComboBox<String> combo_series;
    @FXML
    private Button boton_seleccionar_portada;
    @FXML
    private Button boton_restaurar_portada;
    @FXML
    private Button boton_guardar_cambios;
    @FXML
    private Label label_portada;

    private Libro libro;
    private SaveAction alGuardar;
    private String rutaPortada;

    @FXML
    private void initialize() {
        boton_seleccionar_portada.setOnAction(event -> seleccionarPortada());
        boton_restaurar_portada.setOnAction(event -> {
            rutaPortada = null;
            actualizarEtiquetaPortada();
        });
        boton_guardar_cambios.setOnAction(event -> guardarCambios());
    }

    public void configurar(Libro libro, Collection<String> series, SaveAction alGuardar) {
        this.libro = Objects.requireNonNull(libro);
        this.alGuardar = Objects.requireNonNull(alGuardar);
        campo_titulo.setText(libro.getTitulo());
        campo_autor.setText(libro.getAutor());
        combo_series.getItems().setAll(series);
        if (!libro.getSerie().isBlank() && combo_series.getItems().contains(libro.getSerie())) {
            combo_series.getSelectionModel().select(libro.getSerie());
        }
        rutaPortada = libro.getRutaPortada();
        actualizarEtiquetaPortada();
    }

    private void seleccionarPortada() {
        FileChooser selector = new FileChooser();
        selector.setTitle("Seleccionar portada del libro");
        selector.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                "Imágenes", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"));
        File actual = rutaPortada == null ? null : new File(rutaPortada);
        if (actual != null && actual.isFile()) {
            if (actual.getParentFile() != null && actual.getParentFile().isDirectory()) {
                selector.setInitialDirectory(actual.getParentFile());
            }
            selector.setInitialFileName(actual.getName());
        }

        File seleccionada = selector.showOpenDialog(boton_seleccionar_portada.getScene().getWindow());
        if (seleccionada != null) {
            rutaPortada = seleccionada.getAbsolutePath();
            actualizarEtiquetaPortada();
        }
    }

    private void actualizarEtiquetaPortada() {
        label_portada.setText(rutaPortada == null || rutaPortada.isBlank()
                ? "Portada predeterminada"
                : new File(rutaPortada).getName());
    }

    private void guardarCambios() {
        String titulo = campo_titulo.getText().trim();
        if (titulo.isEmpty()) {
            mostrarAviso("El título del libro no puede estar vacío.");
            return;
        }

        String nuevaSerie = campo_nueva_serie.getText().trim();
        String serie = nuevaSerie.isEmpty()
                ? Objects.requireNonNullElse(combo_series.getValue(), "")
                : nuevaSerie;
        try {
            alGuardar.guardar(libro, titulo, campo_autor.getText().trim(), serie, rutaPortada);
            ((Stage) boton_guardar_cambios.getScene().getWindow()).close();
        } catch (IOException exception) {
            mostrarAviso(exception.getMessage());
        }
    }

    private void mostrarAviso(String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.WARNING, mensaje, ButtonType.OK);
        alerta.setTitle("No se pudieron guardar los cambios");
        alerta.setHeaderText(null);
        alerta.initOwner(boton_guardar_cambios.getScene().getWindow());
        alerta.showAndWait();
    }

    @FunctionalInterface
    public interface SaveAction {
        void guardar(Libro libro, String titulo, String autor, String serie, String rutaPortada)
                throws IOException;
    }
}
