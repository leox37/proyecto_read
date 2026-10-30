
package app;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.github.mertakdut.BookSection;
import com.github.mertakdut.Reader;
import com.github.mertakdut.exception.OutOfPagesException;

import modelo.Libro;

public class LectorEpub {

    // Comprueba si el archivo tiene extension EPUB.
    public boolean esArchivoEpub(Path archivo) {
        if (archivo == null || archivo.getFileName() == null) {
            return false;
        }

        return archivo.getFileName()
                .toString()
                .toLowerCase(Locale.ROOT)
                .endsWith(".epub");
    }

    // Lee el contenido de un archivo EPUB usando su ruta.
    public List<String> leerContenido(Path archivo) throws IOException {

        if (!esArchivoEpub(archivo)) {
            throw new IllegalArgumentException(
                    "El archivo debe tener formato EPUB."
            );
        }

        if (!Files.isRegularFile(archivo)) {
            throw new IOException(
                    "El archivo no existe o no es un archivo valido: "
                            + archivo
            );
        }

        List<String> secciones = new ArrayList<>();

        try {
            Reader lector = new Reader();
            lector.setMaxContentPerSection(1000);
            lector.setIsIncludingTextContent(true);
            lector.setFullContent(archivo.toAbsolutePath().toString());

            int pagina = 0;

            while (true) {
                BookSection seccion;

                try {
                    seccion = lector.readSection(pagina);
                } catch (OutOfPagesException e) {
                    // La biblioteca indica que se alcanzo el final.
                    break;
                }

                if (seccion == null) {
                    break;
                }

                String texto = seccion.getSectionTextContent();

                if (texto != null && !texto.isBlank()) {
                    secciones.add(texto);
                }

                pagina++;
            }

        } catch (Exception e) {
            throw new IOException(
                    "No se pudo completar la lectura del EPUB: "
                            + e.getMessage(),
                    e
            );
        }

        return secciones;
    }

    // Lee un libro utilizando los datos de la clase Libro.
    public List<String> leerContenido(Libro libro) throws IOException {

        if (libro == null) {
            throw new IllegalArgumentException(
                    "El libro no puede ser nulo."
            );
        }

        if (libro.getRutaArchivo() == null
                || libro.getRutaArchivo().isBlank()) {
            throw new IllegalArgumentException(
                    "El libro no tiene una ruta de archivo."
            );
        }

        if (libro.getFormato() == null
                || !libro.getFormato().equalsIgnoreCase("EPUB")) {
            throw new IllegalArgumentException(
                    "El formato del libro debe ser EPUB."
            );
        }

        return leerContenido(Path.of(libro.getRutaArchivo()));
    }
}
