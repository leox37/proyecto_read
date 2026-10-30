package app_read.vista;

import app_read.modelo.Libro;

import java.io.IOException;
import java.util.List;

public interface Acciones {
    List<Libro> listar(String filtro) throws IOException;

    void leer(Libro libro) throws IOException;
}
