
package app;

import java.util.List;
import modelo.Libro;

public class PruebaLectorEpub {

    public static void main(String[] args) {

        String ruta = System.getProperty("user.home")
                + "\\Downloads\\pg2000-images-3.epub";

        Libro libro = new Libro(
                "LIB001",
                "Libro de prueba",
                "Autor de prueba",
                ruta,
                "EPUB"
        );

        LectorEpub lector = new LectorEpub();

        try {
            List<String> secciones = lector.leerContenido(libro);

            System.out.println("Libro: " + libro.getTitulo());
            System.out.println("Formato: " + libro.getFormato());
            System.out.println("Lectura completada correctamente.");
            System.out.println("Secciones extraidas: " + secciones.size());

            if (!secciones.isEmpty()) {
                System.out.println("\nPrimera seccion:");
                System.out.println(secciones.get(0));
            }

        } catch (Exception e) {
            System.out.println("Error al leer el libro:");
            e.printStackTrace();
        }
    }
}
