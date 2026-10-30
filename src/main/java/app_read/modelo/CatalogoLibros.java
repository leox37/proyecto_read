package app_read.modelo;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

public class CatalogoLibros {
    private static final Type LISTA_LIBROS = new TypeToken<List<Libro>>() { }.getType();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path archivoCatalogo;
    private final List<Libro> libros;
    private final Set<String> colecciones;
    private final Set<String> series;
    private final Set<String> tiposNotas;

    public CatalogoLibros() throws IOException {
        this(Path.of(System.getProperty("user.home"), ".proyecto_read", "libros.json"));
    }

    CatalogoLibros(Path archivoCatalogo) throws IOException {
        this.archivoCatalogo = archivoCatalogo.toAbsolutePath().normalize();
        if (Files.exists(this.archivoCatalogo)) {
            DatosCatalogo datos = leerArchivo();
            libros = datos.libros;
            colecciones = new LinkedHashSet<>(datos.colecciones);
            series = new LinkedHashSet<>(datos.series);
            tiposNotas = new LinkedHashSet<>(datos.tiposNotas);
        } else {
            DatosCatalogo datos = leerCatalogoInicial();
            libros = datos.libros;
            colecciones = new LinkedHashSet<>(datos.colecciones);
            series = new LinkedHashSet<>(datos.series);
            tiposNotas = new LinkedHashSet<>(datos.tiposNotas);
            guardar();
        }
    }

    public List<Libro> listar() {
        return List.copyOf(libros);
    }

    public List<String> listarColecciones() {
        return List.copyOf(colecciones);
    }

    public List<String> listarSeries() {
        return List.copyOf(series);
    }

    public List<String> listarTiposNotas() {
        return List.copyOf(tiposNotas);
    }

    public boolean crearTipoNota(String nombre) throws IOException {
        String normalizado = Objects.requireNonNull(nombre).trim();
        if (normalizado.isEmpty() || tiposNotas.stream()
                .anyMatch(tipo -> tipo.equalsIgnoreCase(normalizado))) {
            return false;
        }
        tiposNotas.add(normalizado);
        try {
            guardar();
        } catch (IOException exception) {
            tiposNotas.remove(normalizado);
            throw exception;
        }
        return true;
    }

    public boolean agregar(Libro libro) throws IOException {
        Objects.requireNonNull(libro);
        Path rutaNueva = Path.of(libro.getRutaArchivo()).toAbsolutePath().normalize();
        boolean yaExiste = libros.stream().anyMatch(guardado ->
                Path.of(guardado.getRutaArchivo()).toAbsolutePath().normalize().equals(rutaNueva));
        if (yaExiste) {
            return false;
        }

        libros.add(libro);
        boolean serieNueva = agregarSerie(libro.getSerie());
        try {
            guardar();
        } catch (IOException exception) {
            libros.remove(libro);
            if (serieNueva) {
                series.remove(libro.getSerie());
            }
            throw exception;
        }
        return true;
    }

    public void actualizar(Libro libro, Consumer<Libro> cambio) throws IOException {
        Objects.requireNonNull(libro);
        Objects.requireNonNull(cambio);
        int indice = libros.indexOf(libro);
        if (indice < 0) {
            throw new IOException("El libro ya no está en el catálogo.");
        }

        Libro anterior = copiar(libros.get(indice));
        cambio.accept(libro);
        boolean serieNueva = agregarSerie(libro.getSerie());
        try {
            guardar();
        } catch (IOException exception) {
            copiarValores(anterior, libro);
            if (serieNueva) {
                series.remove(libro.getSerie());
            }
            throw exception;
        }
    }

    public boolean crearColeccion(String nombre) throws IOException {
        String normalizado = Objects.requireNonNull(nombre).trim();
        if (normalizado.isEmpty()) {
            return false;
        }
        boolean existente = colecciones.stream().anyMatch(coleccion ->
                coleccion.equalsIgnoreCase(normalizado));
        if (existente) {
            return false;
        }

        colecciones.add(normalizado);
        try {
            guardar();
        } catch (IOException exception) {
            colecciones.remove(normalizado);
            throw exception;
        }
        return true;
    }

    private boolean agregarSerie(String serie) {
        String normalizada = serie == null ? "" : serie.trim();
        if (normalizada.isEmpty() || series.stream().anyMatch(guardada ->
                guardada.equalsIgnoreCase(normalizada))) {
            return false;
        }
        return series.add(normalizada);
    }

    private DatosCatalogo leerArchivo() throws IOException {
        try (Reader reader = Files.newBufferedReader(archivoCatalogo, StandardCharsets.UTF_8)) {
            return leerDatos(JsonParser.parseReader(reader));
        } catch (JsonParseException exception) {
            throw new IOException("El catálogo JSON está dañado: " + archivoCatalogo, exception);
        }
    }

    private DatosCatalogo leerCatalogoInicial() throws IOException {
        try (InputStream stream = CatalogoLibros.class.getResourceAsStream("/app_read/modelo/datos.json")) {
            if (stream == null) {
                throw new IOException("No se encontró el catálogo inicial de libros.");
            }
            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return leerDatos(JsonParser.parseReader(reader));
            } catch (JsonParseException exception) {
                throw new IOException("El catálogo inicial de libros no contiene JSON válido.", exception);
            }
        }
    }

    private static DatosCatalogo leerDatos(JsonElement json) throws IOException {
        List<Libro> librosLeidos;
        List<String> coleccionesLeidas = new ArrayList<>();
        List<String> seriesLeidas = new ArrayList<>();
        List<String> tiposNotasLeidos = new ArrayList<>();
        if (json.isJsonArray()) {
            librosLeidos = GSON.fromJson(json, LISTA_LIBROS);
        } else if (json.isJsonObject()) {
            DatosCatalogo almacenados = GSON.fromJson(json, DatosCatalogo.class);
            librosLeidos = almacenados == null ? null : almacenados.libros;
            if (almacenados != null && almacenados.colecciones != null) {
                coleccionesLeidas.addAll(almacenados.colecciones);
            }
            if (almacenados != null && almacenados.series != null) {
                almacenados.series.forEach(serie -> agregarSerieUnica(seriesLeidas, serie));
            }
            if (almacenados != null && almacenados.tiposNotas != null) {
                almacenados.tiposNotas.forEach(tipo -> agregarSerieUnica(tiposNotasLeidos, tipo));
            }
        } else {
            throw new IOException("El catálogo JSON debe ser una lista de libros o un objeto de catálogo.");
        }
        if (librosLeidos == null) {
            throw new IOException("El catálogo JSON debe contener una lista de libros.");
        }
        for (Libro libro : librosLeidos) {
            if (libro == null || libro.getIdLibro() == null || libro.getTitulo() == null
                    || libro.getAutor() == null || libro.getRutaArchivo() == null
                    || libro.getFormato() == null) {
                throw new IOException("El catálogo JSON contiene un libro incompleto.");
            }
            if (libro.getColecciones() != null) {
                coleccionesLeidas.addAll(libro.getColecciones());
            }
            if (!libro.getSerie().isBlank()) {
                agregarSerieUnica(seriesLeidas, libro.getSerie());
            }
        }
        return new DatosCatalogo(new ArrayList<>(librosLeidos),
                new ArrayList<>(new LinkedHashSet<>(coleccionesLeidas)),
                seriesLeidas, tiposNotasLeidos);
    }

    private static void agregarSerieUnica(List<String> series, String nombre) {
        if (nombre != null && !nombre.isBlank()
                && series.stream().noneMatch(serie -> serie.equalsIgnoreCase(nombre.trim()))) {
            series.add(nombre.trim());
        }
    }

    private void guardar() throws IOException {
        Path carpeta = archivoCatalogo.getParent();
        Files.createDirectories(carpeta);
        Path temporal = Files.createTempFile(carpeta, "libros-", ".json.tmp");
        try {
            Files.writeString(temporal, GSON.toJson(new DatosCatalogo(libros,
                    new ArrayList<>(colecciones), new ArrayList<>(series),
                    new ArrayList<>(tiposNotas))), StandardCharsets.UTF_8);
            try {
                Files.move(temporal, archivoCatalogo, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporal, archivoCatalogo, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporal);
        }
    }

    private static Libro copiar(Libro libro) {
        Libro copia = new Libro(libro.getIdLibro(), libro.getTitulo(), libro.getAutor(),
                libro.getRutaArchivo(), libro.getFormato());
        copiarValores(libro, copia);
        return copia;
    }

    private static void copiarValores(Libro origen, Libro destino) {
        destino.setFavorito(origen.isFavorito());
        destino.setParaLeer(origen.isParaLeer());
        destino.setLeido(origen.isLeido());
        destino.setEnPapelera(origen.isEnPapelera());
        destino.setColecciones(origen.getColecciones());
        destino.setSerie(origen.getSerie());
        destino.setRutaPortada(origen.getRutaPortada());
        destino.setProgresoLectura(origen.getProgresoLectura());
        destino.setNotas(origen.getNotas());
    }

    private static final class DatosCatalogo {
        private List<Libro> libros;
        private List<String> colecciones;
        private List<String> series;
        private List<String> tiposNotas;

        private DatosCatalogo(List<Libro> libros, List<String> colecciones, List<String> series,
                              List<String> tiposNotas) {
            this.libros = libros;
            this.colecciones = colecciones;
            this.series = series;
            this.tiposNotas = tiposNotas;
        }
    }
}
