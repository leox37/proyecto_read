package app_read.controlador;

import app_read.modelo.CatalogoLibros;
import app_read.modelo.Libro;
import app_read.vista.Acciones;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;

public class InicioController implements Acciones {
    private static final System.Logger LOGGER = System.getLogger(InicioController.class.getName());

    @FXML
    private VBox caja_tarjetas;
    @FXML
    private VBox menu_lateral;
    @FXML
    private TextField barra_busqueda;
    @FXML
    private Button boton_menu;
    @FXML
    private ImageView boton_mas;
    @FXML
    private ImageView boton_buscar;
    @FXML
    private HBox boton_libros;
    @FXML
    private HBox boton_favoritos;
    @FXML
    private HBox boton_para_leer;
    @FXML
    private HBox boton_leidos;
    @FXML
    private HBox boton_autores;
    @FXML
    private HBox boton_series;
    @FXML
    private HBox boton_colecciones;
    @FXML
    private HBox boton_formatos;
    @FXML
    private HBox boton_papelera;
    @FXML
    private Label label_seccion;

    private CatalogoLibros catalogo;
    private Vista vista = Vista.TODOS;
    private String valorVista = "";
    private boolean menuExpandido;



    @FXML
    private void initialize() {
        Tooltip.install(boton_mas, new Tooltip("Añadir un libro HTML, HTM o TXT"));
        boton_menu.setOnAction(event -> cambiarMenu());
        boton_mas.setOnMouseClicked(event -> agregarLibro());
        boton_buscar.setOnMouseClicked(event -> actualizarTarjetas());
        barra_busqueda.setOnAction(event -> actualizarTarjetas());
        barra_busqueda.textProperty().addListener((observable, anterior, nuevo) -> actualizarTarjetas());

        boton_libros.setOnMouseClicked(event -> mostrarLibros());
        boton_favoritos.setOnMouseClicked(event -> mostrarFiltro(Vista.FAVORITOS, "Favoritos"));
        boton_para_leer.setOnMouseClicked(event -> mostrarFiltro(Vista.PARA_LEER, "Para leer"));
        boton_leidos.setOnMouseClicked(event -> mostrarFiltro(Vista.LEIDOS, "Leídos"));
        boton_autores.setOnMouseClicked(event -> mostrarFiltro(Vista.AUTORES, "Autores"));
        boton_series.setOnMouseClicked(event -> mostrarFiltro(Vista.SERIES, "Series"));
        boton_colecciones.setOnMouseClicked(event -> mostrarFiltro(Vista.COLECCIONES, "Colecciones"));
        boton_formatos.setOnMouseClicked(event -> seleccionarFormato());
        boton_papelera.setOnMouseClicked(event -> mostrarFiltro(Vista.PAPELERA, "Papelera"));
        setMenuExpandido(false);

        try {
            catalogo = new CatalogoLibros();
            actualizarTarjetas();
        } catch (IOException exception) {
            deshabilitarBiblioteca();
            mostrarError(null, "No se pudo cargar el catálogo de libros: " + exception.getMessage());
        }

    }

    private void mostrarColeccion(String nombre) {
        vista = Vista.COLECCION;
        valorVista = nombre;
        label_seccion.setText("Colección: " + nombre);
        actualizarTarjetas();
    }

    @Override
    public List<Libro> listar(String filtro) {
        String consulta = normalizar(filtro);
        return obtenerCatalogo().listar().stream()
                .filter(this::coincideVista)
                .filter(libro -> coincideBusqueda(libro, consulta))
                .toList();
    }

    @Override
    public void leer(Libro libro) throws IOException {
        Scene scene = caja_tarjetas.getScene();
        Stage ventana = (Stage) scene.getWindow();
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/app_read/vista/fxml/lector_panel.fxml"));
        Parent vistaLector = loader.load();
        LectorController controlador = loader.getController();
        controlador.configurar(obtenerCatalogo(), libro);
        controlador.cargarLibro(libro);

        Parent vistaBiblioteca = scene.getRoot();
        controlador.setAlVolver(() -> {
            scene.setRoot(vistaBiblioteca);
            actualizarTarjetas();
            ventana.setResizable(false);
            ventana.sizeToScene();
        });
        ventana.setResizable(true);
        scene.setRoot(vistaLector);
        ventana.sizeToScene();
    }

    private void cambiarMenu() {
        setMenuExpandido(!menuExpandido);
    }

    private void setMenuExpandido(boolean expandido) {
        menuExpandido = expandido;
        menu_lateral.setPrefWidth(expandido ? 190 : 42);
        menu_lateral.setMinWidth(expandido ? 190 : 42);
        for (Node nodo : menu_lateral.getChildren()) {
            if (nodo instanceof HBox fila) {
                fila.setPrefWidth(expandido ? 190 : 42);
                for (Node elemento : fila.getChildren()) {
                    if (elemento instanceof Label etiqueta) {
                        etiqueta.setVisible(expandido);
                        etiqueta.setManaged(expandido);
                    }
                }
            }
        }
        boton_menu.setText(expandido ? "Cerrar menú" : "Menú");
    }

    private void mostrarLibros() {
        vista = Vista.TODOS;
        valorVista = "";
        label_seccion.setText("Libros y Documentos");
        actualizarTarjetas();
    }

    private void mostrarFiltro(Vista nuevaVista, String titulo) {
        vista = nuevaVista;
        valorVista = "";
        label_seccion.setText(titulo);
        actualizarTarjetas();
    }

    private void seleccionarFormato() {
        List<String> formatos = obtenerCatalogo().listar().stream()
                .filter(libro -> !libro.isEnPapelera())
                .map(Libro::getFormato)
                .filter(Objects::nonNull)
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
        if (formatos.isEmpty()) {
            mostrarAviso(caja_tarjetas.getScene(), "Aún no hay formatos en la biblioteca.");
            return;
        }

        ChoiceDialog<String> dialogo = new ChoiceDialog<>(formatos.getFirst(), formatos);
        dialogo.setTitle("Filtrar por formato");
        dialogo.setHeaderText("Elige el formato de los libros");
        dialogo.setContentText("Formato:");
        dialogo.initOwner(caja_tarjetas.getScene().getWindow());
        dialogo.showAndWait().ifPresent(formato -> {
            vista = Vista.FORMATO;
            valorVista = formato;
            label_seccion.setText("Formato: " + formato);
            actualizarTarjetas();
        });
    }

    private void agregarLibro() {
        Scene scene = caja_tarjetas.getScene();
        FileChooser selector = new FileChooser();
        selector.setTitle("Añadir libro a la biblioteca");
        selector.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                "Libros compatibles (HTML, HTM, TXT)", "*.html", "*.htm", "*.txt"));

        File archivo = selector.showOpenDialog(scene.getWindow());
        if (archivo == null) {
            return;
        }

        Libro libro = new Libro(
                archivo.getAbsolutePath(),
                nombreSinExtension(archivo),
                "",
                archivo.getAbsolutePath(),
                extensionDe(archivo));

        try {
            if (!obtenerCatalogo().agregar(libro)) {
                mostrarAviso(scene, "Este libro ya está en la biblioteca.");
                return;
            }
            mostrarLibros();
        } catch (IOException exception) {
            mostrarError(scene, "No se pudo guardar el libro: " + exception.getMessage());
        }
    }

    private void actualizarTarjetas() {
        if (catalogo == null) {
            return;
        }
        try {
            List<Node> tarjetas = switch (vista) {
                case AUTORES -> crearTarjetasAutores();
                case SERIES -> crearTarjetasSeries();
                case COLECCIONES -> crearTarjetasColecciones();
                default -> crearTarjetasLibros(listar(barra_busqueda.getText()));
            };
            if (tarjetas.isEmpty()) {
                tarjetas.add(mensajeVacio());
            }
            caja_tarjetas.getChildren().setAll(tarjetas);
        } catch (IOException exception) {
            LOGGER.log(System.Logger.Level.ERROR, "No se pudieron cargar las tarjetas de la biblioteca.",
                    exception);
            mostrarError(caja_tarjetas.getScene(), "No se pudo actualizar la biblioteca: "
                    + exception.getMessage());
        }
    }

    private List<Node> crearTarjetasLibros(List<Libro> libros) throws IOException {
        List<Node> tarjetas = new ArrayList<>();
        for (Libro libro : libros) {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/app_read/vista/fxml/tarjeta_libro.fxml"));
            tarjetas.add(loader.load());
            TarjetaLibroController controlador = loader.getController();
            controlador.configurar(libro, this::leer, this::actualizarLibro,
                    libroActual -> abrirDialogoColeccion(libroActual,
                            seleccionado -> mostrarColeccion(seleccionado)),
                    this::cambiarPapelera, this::editarLibro);
        }
        return tarjetas;
    }

    private List<Node> crearTarjetasSeries() {
        List<Node> tarjetas = new ArrayList<>();
        String busqueda = normalizar(barra_busqueda.getText());
        for (String serie : obtenerCatalogo().listarSeries().stream()
                .filter(nombre -> normalizar(nombre).contains(busqueda)).toList()) {
            long cantidad = obtenerCatalogo().listar().stream()
                    .filter(libro -> !libro.isEnPapelera())
                    .filter(libro -> libro.getSerie().equalsIgnoreCase(serie))
                    .count();
            HBox tarjeta = new HBox(14);
            tarjeta.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            tarjeta.setPrefHeight(64);
            tarjeta.setStyle("-fx-background-color: #384959; -fx-padding: 12;");
            ImageView imagen = new ImageView(cargarImagen("/app_read/vista/assets/align-center.png"));
            imagen.setFitWidth(38);
            imagen.setFitHeight(38);
            imagen.setPreserveRatio(true);
            Label nombre = new Label(serie + " (" + cantidad + ")");
            nombre.setStyle("-fx-text-fill: white; -fx-font-size: 15px;");
            tarjeta.getChildren().addAll(imagen, nombre);
            tarjeta.setOnMouseClicked(event -> {
                vista = Vista.SERIE;
                valorVista = serie;
                label_seccion.setText("Serie: " + serie);
                actualizarTarjetas();
            });
            tarjetas.add(tarjeta);
        }
        return tarjetas;
    }

    private List<Node> crearTarjetasAutores() {
        List<String> autores = obtenerCatalogo().listar().stream()
                .filter(libro -> !libro.isEnPapelera())
                .map(Libro::getAutor)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(autor -> !autor.isEmpty())
                .distinct()
                .filter(autor -> normalizar(autor).contains(normalizar(barra_busqueda.getText())))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
        List<Node> tarjetas = new ArrayList<>();
        for (String autor : autores) {
            HBox tarjeta = new HBox(14);
            tarjeta.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            tarjeta.setPrefHeight(72);
            tarjeta.setStyle("-fx-background-color: #384959; -fx-padding: 12;");
            ImageView imagen = new ImageView(cargarImagen("/app_read/vista/assets/user.png"));
            imagen.setFitWidth(42);
            imagen.setFitHeight(42);
            imagen.setPreserveRatio(true);
            Label nombre = new Label(autor);
            nombre.setStyle("-fx-text-fill: white; -fx-font-size: 15px;");
            tarjeta.getChildren().addAll(imagen, nombre);
            tarjeta.setOnMouseClicked(event -> {
                vista = Vista.AUTOR;
                valorVista = autor;
                label_seccion.setText("Autor: " + autor);
                actualizarTarjetas();
            });
            tarjetas.add(tarjeta);
        }
        return tarjetas;
    }

    private List<Node> crearTarjetasColecciones() {
        List<Node> tarjetas = new ArrayList<>();
        Button nuevaColeccion = new Button("Crear colección");
        nuevaColeccion.setOnAction(event -> abrirDialogoColeccion(null, ignorada -> actualizarTarjetas()));
        tarjetas.add(nuevaColeccion);

        for (String coleccion : obtenerCatalogo().listarColecciones()) {
            long cantidad = obtenerCatalogo().listar().stream()
                    .filter(libro -> !libro.isEnPapelera())
                    .filter(libro -> libro.getColecciones().contains(coleccion))
                    .count();
            HBox tarjeta = new HBox(14);
            tarjeta.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            tarjeta.setPrefHeight(64);
            tarjeta.setStyle("-fx-background-color: #384959; -fx-padding: 12;");
            ImageView imagen = new ImageView(cargarImagen("/app_read/vista/assets/library.png"));
            imagen.setFitWidth(38);
            imagen.setFitHeight(38);
            imagen.setPreserveRatio(true);
            Label nombre = new Label(coleccion + " (" + cantidad + ")");
            nombre.setStyle("-fx-text-fill: white; -fx-font-size: 15px;");
            tarjeta.getChildren().addAll(imagen, nombre);
            tarjeta.setOnMouseClicked(event -> {
                vista = Vista.COLECCION;
                valorVista = coleccion;
                label_seccion.setText("Colección: " + coleccion);
                actualizarTarjetas();
            });
            tarjetas.add(tarjeta);
        }
        return tarjetas;
    }

    private boolean coincideVista(Libro libro) {
        return switch (vista) {
            case TODOS -> !libro.isEnPapelera();
            case FAVORITOS -> !libro.isEnPapelera() && libro.isFavorito();
            case PARA_LEER -> !libro.isEnPapelera() && libro.isParaLeer();
            case LEIDOS -> !libro.isEnPapelera() && libro.isLeido();
            case PAPELERA -> libro.isEnPapelera();
            case FORMATO -> !libro.isEnPapelera() && libro.getFormato().equalsIgnoreCase(valorVista);
            case AUTOR -> !libro.isEnPapelera() && libro.getAutor().equalsIgnoreCase(valorVista);
            case SERIE -> !libro.isEnPapelera() && libro.getSerie().equalsIgnoreCase(valorVista);
            case COLECCION -> !libro.isEnPapelera() && libro.getColecciones().contains(valorVista);
            case AUTORES, SERIES, COLECCIONES -> false;
        };
    }

    private boolean coincideBusqueda(Libro libro, String consulta) {
        return consulta.isEmpty()
                || normalizar(libro.getTitulo()).contains(consulta)
                || normalizar(libro.getAutor()).contains(consulta)
                || normalizar(libro.getFormato()).contains(consulta);
    }

    private Label mensajeVacio() {
        String mensaje = switch (vista) {
            case TODOS -> "Tu biblioteca está vacía. Usa el botón de tres puntos para añadir un libro.";
            case FAVORITOS -> "Aún no tienes libros favoritos.";
            case PARA_LEER -> "No tienes libros en la lista «Para leer».";
            case LEIDOS -> "Aún no has marcado libros como leídos.";
            case PAPELERA -> "La papelera está vacía.";
            case FORMATO -> "No hay libros de este formato.";
            case AUTOR -> "No hay libros para este autor.";
            case SERIE -> "No hay libros para esta serie.";
            case COLECCION -> "Esta colección aún no tiene libros.";
            case AUTORES -> "No hay autores en la biblioteca.";
            case SERIES -> "Aún no hay series en la biblioteca.";
            case COLECCIONES -> "Aún no has creado colecciones.";
        };
        Label etiqueta = new Label(mensaje);
        etiqueta.setWrapText(true);
        return etiqueta;
    }

    private void actualizarLibro(Libro libro, Consumer<Libro> cambio) throws IOException {
        obtenerCatalogo().actualizar(libro, cambio);
        actualizarTarjetas();
    }

    private void cambiarPapelera(Libro libro) throws IOException {
        obtenerCatalogo().actualizar(libro, actual -> actual.setEnPapelera(!actual.isEnPapelera()));
        actualizarTarjetas();
    }

    private void editarLibro(Libro libro) throws IOException {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/app_read/vista/fxml/sub_editar.fxml"));
        Parent vistaEditar = loader.load();
        EditarLibroController controlador = loader.getController();
        controlador.configurar(libro, obtenerCatalogo().listarSeries(),
                (actual, titulo, autor, serie, portada) -> actualizarLibro(actual, editado -> {
                    editado.setTitulo(titulo);
                    editado.setAutor(autor);
                    editado.setSerie(serie);
                    editado.setRutaPortada(portada);
                }));

        Stage dialogo = new Stage();
        dialogo.initOwner(caja_tarjetas.getScene().getWindow());
        dialogo.initModality(Modality.WINDOW_MODAL);
        dialogo.setTitle("Editar libro");
        dialogo.setScene(new Scene(vistaEditar));
        dialogo.setResizable(false);
        dialogo.showAndWait();
    }

    private void abrirDialogoColeccion(Libro libro, Consumer<String> alSeleccionar) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/app_read/vista/fxml/sub_colecciones.fxml"));
            Parent vistaColecciones = loader.load();
            ColeccionesController controlador = loader.getController();
            controlador.configurar(obtenerCatalogo().listarColecciones(),
                    nombre -> crearColeccion(nombre),
                    nombre -> {
                        if (libro != null) {
                            agregarLibroAColeccion(libro, nombre);
                        } else {
                            alSeleccionar.accept(nombre);
                        }
                    });

            Stage dialogo = new Stage();
            dialogo.initOwner(caja_tarjetas.getScene().getWindow());
            dialogo.initModality(Modality.WINDOW_MODAL);
            dialogo.setTitle("Colecciones");
            dialogo.setScene(new Scene(vistaColecciones));
            dialogo.setResizable(false);
            dialogo.showAndWait();
        } catch (IOException exception) {
            mostrarError(caja_tarjetas.getScene(), "No se pudo abrir colecciones: "
                    + exception.getMessage());
        }
    }

    private void crearColeccion(String nombre) throws IOException {
        if (!obtenerCatalogo().crearColeccion(nombre)) {
            throw new IOException("La colección ya existe o el nombre está vacío.");
        }
    }

    private void agregarLibroAColeccion(Libro libro, String nombre) {
        try {
            actualizarLibro(libro, actual -> {
                List<String> colecciones = new ArrayList<>(actual.getColecciones());
                if (!colecciones.contains(nombre)) {
                    colecciones.add(nombre);
                }
                actual.setColecciones(colecciones);
            });
        } catch (IOException exception) {
            mostrarError(caja_tarjetas.getScene(), "No se pudo añadir el libro a la colección: "
                    + exception.getMessage());
        }
    }

    private CatalogoLibros obtenerCatalogo() {
        if (catalogo == null) {
            throw new IllegalStateException("El catálogo no se pudo inicializar.");
        }
        return catalogo;
    }

    private void deshabilitarBiblioteca() {
        boton_mas.setDisable(true);
        boton_buscar.setDisable(true);
        barra_busqueda.setDisable(true);
        boton_menu.setDisable(true);
        menu_lateral.setDisable(true);
    }

    private static Image cargarImagen(String ruta) {
        var recurso = InicioController.class.getResource(ruta);
        if (recurso == null) {
            throw new IllegalStateException("No se encontró el icono: " + ruta);
        }
        return new Image(recurso.toExternalForm());
    }

    private static String extensionDe(File archivo) {
        String nombre = archivo.getName();
        int punto = nombre.lastIndexOf('.');
        return punto < 0 ? "" : nombre.substring(punto + 1).toUpperCase(Locale.ROOT);
    }

    private static String nombreSinExtension(File archivo) {
        String nombre = archivo.getName();
        int punto = nombre.lastIndexOf('.');
        return punto < 0 ? nombre : nombre.substring(0, punto);
    }

    private static String normalizar(String texto) {
        return Objects.requireNonNullElse(texto, "").trim().toLowerCase(Locale.ROOT);
    }

    static void mostrarError(Scene scene, String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.ERROR, mensaje, ButtonType.OK);
        alerta.setTitle("Error");
        alerta.setHeaderText(null);
        if (scene != null && scene.getWindow() != null) {
            alerta.initOwner(scene.getWindow());
        }
        alerta.showAndWait();
    }

    private static void mostrarAviso(Scene scene, String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.INFORMATION, mensaje, ButtonType.OK);
        alerta.setTitle("Biblioteca");
        alerta.setHeaderText(null);
        if (scene != null && scene.getWindow() != null) {
            alerta.initOwner(scene.getWindow());
        }
        alerta.showAndWait();
    }

    private enum Vista {
        TODOS,
        FAVORITOS,
        PARA_LEER,
        LEIDOS,
        AUTORES,
        AUTOR,
        COLECCIONES,
        COLECCION,
        SERIES,
        SERIE,
        FORMATO,
        PAPELERA
    }
}
