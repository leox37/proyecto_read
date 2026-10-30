package app_read.modelo;
import java.time.LocalDateTime;
import java.util.Objects;

public class Nota {
    private String idNota;
    private String idLibro;
    private String fragmentoOrigen;
    private String contenidoTexto;
    private String categoriaTema;
    private String fechaCreacion;
    private String colorResaltado = "#FFFF00";

    public Nota() {
    }

    public Nota(String idNota, String idLibro, String fragmentoOrigen, String contenidoTexto, String categoriaTema) {
        this(idNota, idLibro, fragmentoOrigen, contenidoTexto, categoriaTema, "#FFFF00");
    }

    public Nota(String idNota, String idLibro, String fragmentoOrigen, String contenidoTexto,
                String categoriaTema, String colorResaltado) {
        this.idNota = idNota;
        this.idLibro = idLibro;
        this.fragmentoOrigen = fragmentoOrigen;
        this.contenidoTexto = contenidoTexto;
        this.categoriaTema = categoriaTema;
        this.fechaCreacion = LocalDateTime.now().toString();
        this.colorResaltado = Objects.requireNonNullElse(colorResaltado, "#FFFF00");
    }

    public String getIdNota() { return idNota;}
    public String getIdLibro() {return idLibro;}
    public String getFragmentoOrigen() { return fragmentoOrigen;}
    public String getContenidoTexto() {return contenidoTexto;}
    public void setContenidoTexto(String contenidoTexto) { this.contenidoTexto = contenidoTexto;}
    public String getCategoriaTema() {return categoriaTema;}
    public LocalDateTime getFechaCreacion() {
        return fechaCreacion == null ? LocalDateTime.now() : LocalDateTime.parse(fechaCreacion);
    }
    public String getColorResaltado() { return colorResaltado == null ? "#FFFF00" : colorResaltado; }
}
