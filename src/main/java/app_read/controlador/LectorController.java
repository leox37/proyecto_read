package app_read.controlador;

import app_read.modelo.CatalogoLibros;
import app_read.modelo.Libro;
import app_read.modelo.Nota;
import com.google.gson.Gson;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Window;
import javafx.concurrent.Worker;
import javafx.util.Duration;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public class LectorController {
    private static final Gson GSON = new Gson();

    @FXML private ImageView boton_salir;
    @FXML private ImageView boton_buscar1;
    @FXML private ImageView boton_zoom_positivo;
    @FXML private ImageView boton_zoom_negativo;
    @FXML private ImageView boton_vista_libro;
    @FXML private ImageView boton_vista_pagina;
    @FXML private ImageView boton_girar_izquierda;
    @FXML private ImageView boton_girar_derecha;
    @FXML private ImageView boton_notas;
    @FXML private Label label_nombre_libro1;
    @FXML private Label label_pagina;
    @FXML private Label label_capitulo;
    @FXML private TextField barra_busqueda1;
    @FXML private TextField campo_nuevo_tipo;
    @FXML private Slider slider_pagina;
    @FXML private ComboBox<String> combo_tipo;
    @FXML private ComboBox<String> combo_tipo1;
    @FXML private ColorPicker color_resaltado;
    @FXML private TextArea area_declaracion;
    @FXML private Button boton_guardar_nota;
    @FXML private Button boton_crear_tipo;
    @FXML private VBox panel_notas;
    @FXML private VBox caja_notas;
    @FXML private WebView vista_web;

    private final Timeline actualizador = new Timeline(new KeyFrame(
            Duration.millis(500), event -> actualizarPosicion(false)));
    private CatalogoLibros catalogo;
    private Libro libro;
    private Runnable alVolver;
    private boolean vistaDoble;
    private boolean sincronizandoSlider;
    private boolean progresoFallido;
    private int rotacion;
    private double ultimoProgresoGuardado;

    @FXML
    private void initialize() {
        actualizador.setCycleCount(Timeline.INDEFINITE);
        boton_salir.setOnMouseClicked(event -> volver());
        boton_buscar1.setOnMouseClicked(event -> buscarTexto());
        barra_busqueda1.setOnAction(event -> buscarTexto());
        boton_zoom_positivo.setOnMouseClicked(event -> cambiarZoom(0.1));
        boton_zoom_negativo.setOnMouseClicked(event -> cambiarZoom(-0.1));
        boton_vista_pagina.setOnMouseClicked(event -> cambiarVista(false));
        boton_vista_libro.setOnMouseClicked(event -> cambiarVista(true));
        boton_girar_izquierda.setOnMouseClicked(event -> girar(-90));
        boton_girar_derecha.setOnMouseClicked(event -> girar(90));
        boton_notas.setOnMouseClicked(event -> cambiarVisibilidadNotas());
        boton_guardar_nota.setOnAction(event -> guardarNota());
        boton_crear_tipo.setOnAction(event -> crearTipoNota());
        combo_tipo.valueProperty().addListener((observable, anterior, nuevo) -> mostrarNotas());
        slider_pagina.valueProperty().addListener((observable, anterior, nuevo) -> {
            if (!sincronizandoSlider && libro != null) {
                irAPagina(nuevo.doubleValue());
            }
        });
        vista_web.widthProperty().addListener((observable, anterior, nuevo) -> aplicarDisposicion());
        vista_web.heightProperty().addListener((observable, anterior, nuevo) -> aplicarDisposicion());
        vista_web.widthProperty().addListener((observable, anterior, nuevo) -> ajustarRotacion());
        vista_web.heightProperty().addListener((observable, anterior, nuevo) -> ajustarRotacion());
        vista_web.getEngine().getLoadWorker().stateProperty().addListener(
                (observable, anterior, nuevo) -> {
                    if (nuevo == Worker.State.SUCCEEDED) {
                        prepararDocumento();
                    }
                });
        panel_notas.setVisible(false);
        panel_notas.setManaged(false);
        slider_pagina.setDisable(true);
        label_pagina.setText("Página 1 de 1");
    }

    public void setAlVolver(Runnable alVolver) {
        this.alVolver = Objects.requireNonNull(alVolver);
    }

    public void configurar(CatalogoLibros catalogo, Libro libro) {
        this.catalogo = Objects.requireNonNull(catalogo);
        this.libro = Objects.requireNonNull(libro);
        label_nombre_libro1.setText(libro.getTitulo());
        ultimoProgresoGuardado = libro.getProgresoLectura();
        cargarTiposNota();
        mostrarNotas();
    }

    public void cargarLibro(Libro libro) throws IOException {
        if (this.libro == null) {
            throw new IllegalStateException("El lector debe configurarse antes de cargar un libro.");
        }
        Path ruta = Path.of(libro.getRutaArchivo());
        if (!Files.isRegularFile(ruta)) {
            throw new IOException("No se encontró el archivo: " + ruta);
        }

        String extension = extensionDe(ruta);
        WebEngine motor = vista_web.getEngine();
        switch (extension) {
            case "html", "htm" -> motor.load(ruta.toUri().toString());
            case "txt" -> motor.loadContent(convertirTextoAHtml(
                    Files.readString(ruta, StandardCharsets.UTF_8)), "text/html");
            default -> throw new UnsupportedOperationException(
                    "Formato no compatible: ." + extension
                            + ". Por ahora se admiten archivos HTML, HTM y TXT.");
        }
        label_nombre_libro1.setText(libro.getTitulo());
        label_capitulo.setText("Sin capítulo");
    }

    private void prepararDocumento() {
        progresoFallido = false;
        aplicarDisposicion();
        reaplicarResaltados();
        actualizarPosicion(true);
        actualizador.playFromStart();
    }

    private void aplicarDisposicion() {
        if (vista_web.getEngine().getDocument() == null || vista_web.getWidth() <= 0) {
            return;
        }
        double progresoActual = obtenerProgreso();
        int columnas = vistaDoble ? 2 : 1;
        double anchoColumna = Math.max(240, vista_web.getWidth() / columnas - 50);
        String script = "(function(){"
                + "let s=document.getElementById('lector-disposicion');"
                + "if(!s){s=document.createElement('style');s.id='lector-disposicion';"
                + "document.head.appendChild(s);}"
                + "s.textContent='html{height:100%;overflow:auto!important;scroll-behavior:auto!important;}'"
                + "+'body{box-sizing:border-box!important;width:" + anchoColumna
                + "px!important;height:" + Math.max(250, vista_web.getHeight() - 40)
                + "px!important;margin:20px!important;column-width:" + anchoColumna
                + "px!important;column-gap:40px!important;column-fill:auto!important;}'"
                + "+'img,video,table{max-width:" + (anchoColumna - 20)
                + "px!important;}';})()";
        vista_web.getEngine().executeScript(script);
        ajustarRotacion();
        ScrollMetrics metricas = obtenerMetricas();
        desplazar(metricas.maximoDesplazamiento() * progresoActual, metricas.horizontal());
        actualizarPosicion(false);
    }

    private void cambiarVista(boolean doble) {
        if (vistaDoble == doble) {
            return;
        }
        double progreso = obtenerProgreso();
        vistaDoble = doble;
        aplicarDisposicion();
        posicionarPorProgreso(progreso);
    }

    private void girar(int grados) {
        rotacion = Math.floorMod(rotacion + grados, 360);
        vista_web.setRotate(rotacion);
        ajustarRotacion();
    }

    private void ajustarRotacion() {
        if (vista_web.getWidth() <= 0 || vista_web.getHeight() <= 0) {
            return;
        }
        if (rotacion % 180 == 0) {
            vista_web.setScaleX(1);
            vista_web.setScaleY(1);
            return;
        }
        double factor = Math.min(vista_web.getWidth() / vista_web.getHeight(),
                vista_web.getHeight() / vista_web.getWidth());
        vista_web.setScaleX(factor);
        vista_web.setScaleY(factor);
    }

    private void cambiarVisibilidadNotas() {
        boolean mostrar = !panel_notas.isVisible();
        panel_notas.setVisible(mostrar);
        panel_notas.setManaged(mostrar);
        aplicarDisposicion();
    }

    private void cambiarZoom(double incremento) {
        vista_web.setZoom(Math.max(0.5, Math.min(2.0, vista_web.getZoom() + incremento)));
        aplicarDisposicion();
    }

    private void buscarTexto() {
        String texto = barra_busqueda1.getText().trim();
        if (texto.isEmpty() || vista_web.getEngine().getDocument() == null) {
            return;
        }
        String textoCodificado = URLEncoder.encode(texto, StandardCharsets.UTF_8)
                .replace("+", "%20");
        Object resultado = vista_web.getEngine().executeScript(
                "window.find(decodeURIComponent('" + textoCodificado + "'))");
        if (Boolean.FALSE.equals(resultado)) {
            mostrarAviso("No se encontró el texto: " + texto);
        }
        actualizarPosicion(false);
    }

    private void actualizarPosicion(boolean restaurarGuardado) {
        if (libro == null || vista_web.getEngine().getDocument() == null) {
            return;
        }
        if (restaurarGuardado) {
            posicionarPorProgreso(libro.getProgresoLectura());
        }
        ScrollMetrics metricas = obtenerMetricas();
        int total = Math.max(1, metricas.totalPaginas());
        int actual = Math.max(1, Math.min(total, metricas.paginaActual()));
        label_pagina.setText("Página " + actual + " de " + total);
        sincronizandoSlider = true;
        slider_pagina.setMin(1);
        slider_pagina.setMax(total);
        slider_pagina.setValue(actual);
        slider_pagina.setDisable(total <= 1);
        sincronizandoSlider = false;
        actualizarCapitulo();

        double progreso = obtenerProgreso();
        if (!progresoFallido && Math.abs(progreso - ultimoProgresoGuardado) >= 0.005) {
            guardarProgreso(progreso);
        }
    }

    private void actualizarCapitulo() {
        Object titulo = vista_web.getEngine().executeScript(
                "(function(){let e=document.elementFromPoint(8,24);"
                        + "while(e&&e!==document.body){if(/^H[1-6]$/.test(e.tagName))"
                        + "return e.innerText.trim();e=e.parentElement;}return '';})()");
        if (titulo instanceof String texto && !texto.isBlank()) {
            label_capitulo.setText(texto);
        } else {
            label_capitulo.setText("Sin capítulo");
        }
    }

    private double obtenerProgreso() {
        ScrollMetrics metricas = obtenerMetricas();
        return metricas.maximoDesplazamiento() == 0 ? 0
                : Math.max(0, Math.min(1,
                metricas.posicion() / metricas.maximoDesplazamiento()));
    }

    private void irAPagina(double pagina) {
        ScrollMetrics metricas = obtenerMetricas();
        double destino = Math.min(metricas.maximoDesplazamiento(),
                Math.max(0, (pagina - 1) * metricas.tamanoPagina()));
        desplazar(destino, metricas.horizontal());
        actualizarPosicion(false);
    }

    private void posicionarPorProgreso(double progreso) {
        double acotado = Math.max(0, Math.min(1, progreso));
        ScrollMetrics metricas = obtenerMetricas();
        desplazar(metricas.maximoDesplazamiento() * acotado, metricas.horizontal());
    }

    private ScrollMetrics obtenerMetricas() {
        String resultado = (String) vista_web.getEngine().executeScript(
                "(function(){const e=document.scrollingElement||document.documentElement;"
                        + "const mx=Math.max(0,e.scrollWidth-e.clientWidth);"
                        + "const my=Math.max(0,e.scrollHeight-e.clientHeight);"
                        + "const h=mx>1;const tam=h?e.clientWidth:e.clientHeight;"
                        + "const max=h?mx:my;const pos=h?e.scrollLeft:e.scrollTop;"
                        + "return JSON.stringify({horizontal:h,tamano:tam,maximo:max,posicion:pos,"
                        + "paginas:Math.max(1,Math.ceil(max/Math.max(1,tam))+1)});})()");
        JsonObject datos = JsonParser.parseString(resultado).getAsJsonObject();
        boolean horizontal = datos.get("horizontal").getAsBoolean();
        double tamano = datos.get("tamano").getAsDouble();
        double maximo = datos.get("maximo").getAsDouble();
        double posicion = datos.get("posicion").getAsDouble();
        int total = Math.max(1, datos.get("paginas").getAsInt());
        int pagina = Math.min(total, (int) Math.floor(posicion / Math.max(1, tamano)) + 1);
        return new ScrollMetrics(horizontal, tamano, maximo, posicion, total, pagina);
    }

    private void desplazar(double posicion, boolean horizontal) {
        String eje = horizontal ? "scrollLeft" : "scrollTop";
        vista_web.getEngine().executeScript(
                "(function(){const e=document.scrollingElement||document.documentElement;"
                        + "e." + eje + "=" + posicion + ";})()");
    }

    private void guardarProgreso(double progreso) {
        try {
            catalogo.actualizar(libro, actual -> actual.setProgresoLectura(progreso));
            ultimoProgresoGuardado = progreso;
        } catch (IOException exception) {
            progresoFallido = true;
            actualizador.stop();
            mostrarAviso("No se pudo guardar el progreso de lectura: " + exception.getMessage());
        }
    }

    private void cargarTiposNota() {
        combo_tipo.getItems().setAll("Todas");
        combo_tipo.getItems().addAll(catalogo.listarTiposNotas());
        combo_tipo.getSelectionModel().selectFirst();
        combo_tipo1.getItems().setAll(catalogo.listarTiposNotas());
        if (!combo_tipo1.getItems().isEmpty()) {
            combo_tipo1.getSelectionModel().selectFirst();
        }
    }

    private void crearTipoNota() {
        String tipo = campo_nuevo_tipo.getText().trim();
        if (tipo.isEmpty()) {
            mostrarAviso("Escribe un nombre para el tipo de nota.");
            return;
        }
        try {
            if (!catalogo.crearTipoNota(tipo)) {
                mostrarAviso("Ese tipo ya existe o el nombre está vacío.");
                return;
            }
            campo_nuevo_tipo.clear();
            cargarTiposNota();
            combo_tipo1.getSelectionModel().select(tipo);
            combo_tipo.getSelectionModel().select(tipo);
        } catch (IOException exception) {
            mostrarAviso("No se pudo crear el tipo de nota: " + exception.getMessage());
        }
    }

    private void guardarNota() {
        if (combo_tipo1.getValue() == null) {
            mostrarAviso("Crea o selecciona un tipo de nota antes de guardar.");
            return;
        }
        if (vista_web.getEngine().getDocument() == null) {
            mostrarAviso("Espera a que termine de cargar el documento.");
            return;
        }
        Object seleccion = vista_web.getEngine().executeScript(
                "window.getSelection().toString().replace(/\\s+/g,' ').trim()");
        String fragmento = seleccion instanceof String texto ? texto : "";
        if (fragmento.isBlank()) {
            mostrarAviso("Selecciona primero un fragmento del texto que quieras anotar.");
            return;
        }

        String color = String.format(Locale.ROOT, "#%02X%02X%02X",
                Math.round(color_resaltado.getValue().getRed() * 255),
                Math.round(color_resaltado.getValue().getGreen() * 255),
                Math.round(color_resaltado.getValue().getBlue() * 255));
        Nota nota = new Nota(UUID.randomUUID().toString(), libro.getIdLibro(), fragmento,
                area_declaracion.getText().trim(), combo_tipo1.getValue(), color);
        try {
            catalogo.actualizar(libro, actual -> {
                List<Nota> notas = new ArrayList<>(actual.getNotas());
                notas.add(nota);
                actual.setNotas(notas);
            });
            resaltarSeleccion(nota.getIdNota(), color);
            area_declaracion.clear();
            mostrarNotas();
        } catch (IOException exception) {
            mostrarAviso("No se pudo guardar la nota: " + exception.getMessage());
        }
    }

    private void resaltarSeleccion(String id, String color) {
        vista_web.getEngine().executeScript("(function(){"
                + "const s=window.getSelection();if(!s||s.rangeCount===0||s.isCollapsed)return false;"
                + "const r=s.getRangeAt(0);const span=document.createElement('span');"
                + "span.dataset.notaId=" + GSON.toJson(id)
                + ";span.style.setProperty('background-color'," + GSON.toJson(color) + ",'important');"
                + "span.style.setProperty('-webkit-box-decoration-break','clone');"
                + "try{r.surroundContents(span);}catch(e){span.appendChild(r.extractContents());"
                + "r.insertNode(span);}s.removeAllRanges();return true;})()");
    }

    private void reaplicarResaltados() {
        List<NotaResaltado> resaltados = libro.getNotas().stream()
                .filter(nota -> !nota.getFragmentoOrigen().isBlank())
                .map(nota -> new NotaResaltado(nota.getIdNota(), nota.getFragmentoOrigen(),
                        nota.getColorResaltado()))
                .toList();
        if (resaltados.isEmpty()) {
            return;
        }
        String json = GSON.toJson(resaltados);
        vista_web.getEngine().executeScript("(function(){const notas=" + json + ";"
                + "window.scrollTo(0,0);for(const n of notas){window.getSelection().removeAllRanges();"
                + "if(window.find(n.fragmento)){const s=window.getSelection();"
                + "const r=s.getRangeAt(0);const span=document.createElement('span');"
                + "span.dataset.notaId=n.id;"
                + "span.style.setProperty('background-color',n.color,'important');"
                + "span.style.setProperty('-webkit-box-decoration-break','clone');"
                + "try{r.surroundContents(span);}catch(e){span.appendChild(r.extractContents());"
                + "r.insertNode(span);}}}window.getSelection().removeAllRanges();})()");
    }

    private void mostrarNotas() {
        if (caja_notas == null || libro == null) {
            return;
        }
        caja_notas.getChildren().clear();
        String filtro = combo_tipo.getValue();
        for (Nota nota : libro.getNotas()) {
            if (filtro != null && !filtro.equals("Todas")
                    && !filtro.equals(nota.getCategoriaTema())) {
                continue;
            }
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(
                        "/app_read/vista/fxml/nota.fxml"));
                Parent tarjeta = loader.load();
                NotaController controlador = loader.getController();
                controlador.configurar(nota, () -> eliminarNota(nota));
                caja_notas.getChildren().add(tarjeta);
            } catch (IOException exception) {
                mostrarAviso("No se pudo mostrar una nota: " + exception.getMessage());
                return;
            }
        }
    }

    private void eliminarNota(Nota nota) {
        try {
            catalogo.actualizar(libro, actual -> {
                List<Nota> notas = new ArrayList<>(actual.getNotas());
                notas.removeIf(guardada -> guardada.getIdNota().equals(nota.getIdNota()));
                actual.setNotas(notas);
            });
            quitarResaltado(nota.getIdNota());
            mostrarNotas();
        } catch (IOException exception) {
            mostrarAviso("No se pudo borrar la nota: " + exception.getMessage());
        }
    }

    private void volver() {
        if (alVolver == null) {
            return;
        }
        actualizador.stop();
        if (vista_web.getEngine().getDocument() != null) {
            guardarProgreso(obtenerProgreso());
        }
        alVolver.run();
    }

    private void quitarResaltado(String idNota) {
        vista_web.getEngine().executeScript("(function(){const id=" + GSON.toJson(idNota) + ";"
                + "for(const span of document.querySelectorAll('[data-nota-id]')){"
                + "if(span.dataset.notaId===id){span.replaceWith(...span.childNodes);}}})()");
    }

    private void mostrarAviso(String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.INFORMATION, mensaje, ButtonType.OK);
        alerta.setTitle("Lector");
        alerta.setHeaderText(null);
        Window ventana = vista_web.getScene() == null ? null : vista_web.getScene().getWindow();
        if (ventana != null) {
            alerta.initOwner(ventana);
        }
        alerta.showAndWait();
    }

    private static String extensionDe(Path ruta) {
        String nombre = ruta.getFileName().toString();
        int punto = nombre.lastIndexOf('.');
        return punto < 0 ? "" : nombre.substring(punto + 1).toLowerCase(Locale.ROOT);
    }

    private static String convertirTextoAHtml(String texto) {
        String escapado = texto.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
        return "<!doctype html><html><head><meta charset=\"UTF-8\">"
                + "<style>body{font-family:serif;line-height:1.6;}"
                + "pre{white-space:pre-wrap;overflow-wrap:anywhere;}</style>"
                + "</head><body><pre>" + escapado + "</pre></body></html>";
    }

    private record NotaResaltado(String id, String fragmento, String color) {
    }

    private record ScrollMetrics(boolean horizontal, double tamanoPagina,
                                 double maximoDesplazamiento, double posicion,
                                 int totalPaginas, int paginaActual) {
    }
}
