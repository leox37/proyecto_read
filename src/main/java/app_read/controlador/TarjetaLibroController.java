package app_read.controlador;

import app_read.modelo.Libro;
import javafx.fxml.FXML;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Consumer;

public class TarjetaLibroController {
    @FXML
    private ImageView imagen_portada;
    @FXML
    private Label label_nombre_libro1;
    @FXML
    private Label label_autor;
    @FXML
    private Label label_tipo1;
    @FXML
    private ProgressBar barra_progreso;
    @FXML
    private Label label_progreso;
    @FXML
    private ImageView boton_agregar_favoritos;
    @FXML
    private ImageView boton_agregar_despues;
    @FXML
    private ImageView boton_agregar_leidos;
    @FXML
    private ImageView boton_agregar_coleccion;
    @FXML
    private ImageView boton_mas_tarjeta;

    private Libro libro;
    private LectorAction lectorAction;
    private StateChangeAction stateChangeAction;
    private CollectionAction collectionAction;
    private TrashAction trashAction;
    private EditAction editAction;

    @FXML
    private void initialize() {
        Tooltip.install(imagen_portada, new Tooltip("Abrir libro"));
        Tooltip.install(boton_agregar_favoritos, new Tooltip("Agregar o quitar de favoritos"));
        Tooltip.install(boton_agregar_despues, new Tooltip("Agregar o quitar de Para leer"));
        Tooltip.install(boton_agregar_leidos, new Tooltip("Marcar como leído o pendiente"));
        Tooltip.install(boton_agregar_coleccion, new Tooltip("Agregar a una colección"));
        Tooltip.install(boton_mas_tarjeta, new Tooltip("Más opciones"));
        imagen_portada.setOnMouseClicked(event -> abrirLibro());
        boton_agregar_favoritos.setOnMouseClicked(
                event -> cambiarEstado(actual -> actual.setFavorito(!actual.isFavorito())));
        boton_agregar_despues.setOnMouseClicked(
                event -> cambiarEstado(actual -> actual.setParaLeer(!actual.isParaLeer())));
        boton_agregar_leidos.setOnMouseClicked(
                event -> cambiarEstado(actual -> actual.setLeido(!actual.isLeido())));
        boton_agregar_coleccion.setOnMouseClicked(event -> abrirColecciones());
        boton_mas_tarjeta.setOnMouseClicked(event -> mostrarMasOpciones());
        barra_progreso.setProgress(0);
    }

    public void configurar(Libro libro, LectorAction lectorAction,
                           StateChangeAction stateChangeAction,
                           CollectionAction collectionAction, TrashAction trashAction,
                           EditAction editAction) {
        this.libro = Objects.requireNonNull(libro);
        this.lectorAction = Objects.requireNonNull(lectorAction);
        this.stateChangeAction = Objects.requireNonNull(stateChangeAction);
        this.collectionAction = Objects.requireNonNull(collectionAction);
        this.trashAction = Objects.requireNonNull(trashAction);
        this.editAction = Objects.requireNonNull(editAction);
        label_nombre_libro1.setText(libro.getTitulo());
        label_autor.setText(libro.getAutor().isBlank() ? "Autor desconocido" : libro.getAutor());
        label_tipo1.setText(libro.getFormato());
        actualizarProgreso(libro.getProgresoLectura());
        boton_agregar_favoritos.setImage(icono(libro.isFavorito() ? "star_check.png" : "star.png"));
        boton_agregar_despues.setImage(icono(
                libro.isParaLeer() ? "clock_check.png" : "clock-hour-4.png"));
        boton_agregar_leidos.setImage(icono(libro.isLeido() ? "check_check.png" : "check.png"));
        boton_agregar_coleccion.setImage(icono(
                libro.getColecciones().isEmpty() ? "library.png" : "library_check.png"));
        cargarPortada();
    }

    private void actualizarProgreso(double progreso) {
        double valor = Math.max(0, Math.min(1, progreso));
        barra_progreso.setProgress(valor);
        barra_progreso.setStyle("-fx-accent: #88BDF2;");
        label_progreso.setText(Math.round(valor * 100) + "%");
        Tooltip.install(barra_progreso, new Tooltip("Progreso de lectura: "
                + Math.round(valor * 100) + "%"));
    }

    private void abrirLibro() {
        ejecutarAccion(() -> lectorAction.abrir(libro));
    }

    private void cambiarEstado(Consumer<Libro> cambio) {
        ejecutarAccion(() -> stateChangeAction.actualizar(libro, cambio));
    }

    private void abrirColecciones() {
        collectionAction.abrir(libro);
    }

    private void mostrarMasOpciones() {
        ContextMenu menu = new ContextMenu();
        MenuItem abrir = new MenuItem("Abrir libro");
        abrir.setOnAction(event -> abrirLibro());
        MenuItem editar = new MenuItem("Editar");
        editar.setOnAction(event -> ejecutarAccion(() -> editAction.editar(libro)));
        MenuItem papelera = new MenuItem(libro.isEnPapelera()
                ? "Restaurar de la papelera" : "Enviar a la papelera");
        papelera.setOnAction(event -> ejecutarAccion(() -> trashAction.cambiar(libro)));
        menu.getItems().addAll(abrir, editar, papelera);
        menu.show(boton_mas_tarjeta, javafx.geometry.Side.BOTTOM, 0, 0);
    }

    private void cargarPortada() {
        String ruta = libro.getRutaPortada();
        if (ruta == null || ruta.isBlank()) {
            return;
        }
        Path archivo = Path.of(ruta);
        if (!Files.isRegularFile(archivo)) {
            Tooltip.install(imagen_portada, new Tooltip(
                    "No se encontró la portada personalizada; se muestra la predeterminada."));
            return;
        }

        Image portada = new Image(archivo.toUri().toString(), false);
        if (portada.isError()) {
            Tooltip.install(imagen_portada, new Tooltip(
                    "No se pudo leer la portada personalizada; se muestra la predeterminada."));
            return;
        }
        imagen_portada.setImage(portada);
    }

    private void ejecutarAccion(Accion accion) {
        try {
            accion.ejecutar();
        } catch (IOException | UnsupportedOperationException exception) {
            InicioController.mostrarError(imagen_portada.getScene(), exception.getMessage());
        }
    }

    private static Image icono(String nombre) {
        String ruta = "/app_read/vista/assets/" + nombre;
        var recurso = TarjetaLibroController.class.getResource(ruta);
        if (recurso == null) {
            throw new IllegalStateException("No se encontró el icono: " + ruta);
        }
        return new Image(recurso.toExternalForm());
    }

    @FunctionalInterface
    public interface LectorAction {
        void abrir(Libro libro) throws IOException;
    }

    @FunctionalInterface
    public interface StateChangeAction {
        void actualizar(Libro libro, Consumer<Libro> cambio) throws IOException;
    }

    @FunctionalInterface
    public interface CollectionAction {
        void abrir(Libro libro);
    }

    @FunctionalInterface
    public interface TrashAction {
        void cambiar(Libro libro) throws IOException;
    }

    @FunctionalInterface
    public interface EditAction {
        void editar(Libro libro) throws IOException;
    }

    @FunctionalInterface
    private interface Accion {
        void ejecutar() throws IOException;
    }
}
