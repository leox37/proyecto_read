
package app_read.modelo;

import java.util.ArrayList;
import java.util.List;

public class Libro {
    private String idLibro;
    private String titulo;
    private String autor;
    private String rutaArchivo;
    private String formato;
    private String serie = "";
    private String rutaPortada;
    private boolean favorito;
    private boolean paraLeer;
    private boolean leido;
    private boolean enPapelera;
    private List<String> colecciones = new ArrayList<>();
    private double progresoLectura;
    private List<Nota> notas = new ArrayList<>();

    public Libro() {
    }

    // Constructor
    public Libro(String idLibro, String titulo, String autor, String rutaArchivo, String formato) {
        this.idLibro = idLibro;
        this.titulo = titulo;
        this.autor = autor;
        this.rutaArchivo = rutaArchivo;
        this.formato = formato;
    }

    // Constructor secundario (por si el formato no se especifica inmediatamente)
    public Libro(String idLibro, String titulo, String autor, String rutaArchivo) {
        this(idLibro, titulo, autor, rutaArchivo, "EPUB");
    }

    // Métodos Getters y Setters
    public String getIdLibro() {
        return idLibro;
    }

    public void setIdLibro(String idLibro) {
        this.idLibro = idLibro;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getAutor() {
        return autor;
    }

    public void setAutor(String autor) {
        this.autor = autor;
    }

    public String getRutaArchivo() {
        return rutaArchivo;
    }

    public void setRutaArchivo(String rutaArchivo) {
        this.rutaArchivo = rutaArchivo;
    }

    public String getFormato() {
        return formato;
    }

    public void setFormato(String formato) {
        this.formato = formato;
    }

    public String getSerie() {
        return serie == null ? "" : serie;
    }

    public void setSerie(String serie) {
        this.serie = serie == null ? "" : serie.trim();
    }

    public String getRutaPortada() {
        return rutaPortada;
    }

    public void setRutaPortada(String rutaPortada) {
        this.rutaPortada = rutaPortada;
    }

    public boolean isFavorito() {
        return favorito;
    }

    public void setFavorito(boolean favorito) {
        this.favorito = favorito;
    }

    public boolean isParaLeer() {
        return paraLeer;
    }

    public void setParaLeer(boolean paraLeer) {
        this.paraLeer = paraLeer;
    }

    public boolean isLeido() {
        return leido;
    }

    public void setLeido(boolean leido) {
        this.leido = leido;
    }

    public boolean isEnPapelera() {
        return enPapelera;
    }

    public void setEnPapelera(boolean enPapelera) {
        this.enPapelera = enPapelera;
    }

    public List<String> getColecciones() {
        return colecciones == null ? List.of() : List.copyOf(colecciones);
    }

    public void setColecciones(List<String> colecciones) {
        this.colecciones = new ArrayList<>(colecciones == null ? List.of() : colecciones);
    }

    public double getProgresoLectura() {
        return progresoLectura;
    }

    public void setProgresoLectura(double progresoLectura) {
        this.progresoLectura = Math.max(0, Math.min(1, progresoLectura));
    }

    public List<Nota> getNotas() {
        return notas == null ? List.of() : List.copyOf(notas);
    }

    public void setNotas(List<Nota> notas) {
        this.notas = new ArrayList<>(notas == null ? List.of() : notas);
    }
}
