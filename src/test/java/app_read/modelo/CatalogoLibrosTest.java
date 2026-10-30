package app_read.modelo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CatalogoLibrosTest {
    @TempDir
    Path carpetaTemporal;

    @Test
    void persisteLosLibrosYEvitaDuplicados() throws IOException {
        Path archivoCatalogo = carpetaTemporal.resolve("libros.json");
        Path archivoLibro = carpetaTemporal.resolve("libro.txt");
        CatalogoLibros catalogo = new CatalogoLibros(archivoCatalogo);
        Libro libro = new Libro("1", "Libro de prueba", "Autora", archivoLibro.toString(), "TXT");

        assertTrue(catalogo.agregar(libro));
        assertFalse(catalogo.agregar(libro));

        CatalogoLibros catalogoRecargado = new CatalogoLibros(archivoCatalogo);
        assertEquals(1, catalogoRecargado.listar().size());
        assertEquals("Libro de prueba", catalogoRecargado.listar().getFirst().getTitulo());
        assertTrue(Files.readString(archivoCatalogo).contains("Libro de prueba"));
    }

    @Test
    void informaCuandoElCatalogoNoEsJsonValido() throws IOException {
        Path archivoCatalogo = carpetaTemporal.resolve("libros.json");
        Files.writeString(archivoCatalogo, "{");

        assertThrows(IOException.class, () -> new CatalogoLibros(archivoCatalogo));
    }

    @Test
    void persisteEstadosColeccionesYPapelera() throws IOException {
        Path archivoCatalogo = carpetaTemporal.resolve("libros.json");
        CatalogoLibros catalogo = new CatalogoLibros(archivoCatalogo);
        Libro libro = new Libro("2", "Lectura", "Autor", carpetaTemporal.resolve("lectura.txt").toString(),
                "TXT");
        catalogo.agregar(libro);
        assertTrue(catalogo.crearColeccion("Favoritos de verano"));

        catalogo.actualizar(libro, actual -> {
            actual.setFavorito(true);
            actual.setParaLeer(true);
            actual.setLeido(true);
            actual.setEnPapelera(true);
            actual.setColecciones(java.util.List.of("Favoritos de verano"));
        });

        CatalogoLibros recargado = new CatalogoLibros(archivoCatalogo);
        Libro persistido = recargado.listar().getFirst();
        assertTrue(persistido.isFavorito());
        assertTrue(persistido.isParaLeer());
        assertTrue(persistido.isLeido());
        assertTrue(persistido.isEnPapelera());
        assertEquals(java.util.List.of("Favoritos de verano"), persistido.getColecciones());
        assertEquals(java.util.List.of("Favoritos de verano"), recargado.listarColecciones());
    }

    @Test
    void cargaCatalogoEnFormatoAnteriorYLoMigraAlActualizarlo() throws IOException {
        Path archivoCatalogo = carpetaTemporal.resolve("libros.json");
        Files.writeString(archivoCatalogo, """
                [{"idLibro":"3","titulo":"Antiguo","autor":"Autora",
                  "rutaArchivo":"libro.txt","formato":"TXT"}]
                """);

        CatalogoLibros catalogo = new CatalogoLibros(archivoCatalogo);
        assertEquals("Antiguo", catalogo.listar().getFirst().getTitulo());
        assertTrue(catalogo.crearColeccion("Migrada"));
        assertTrue(Files.readString(archivoCatalogo).contains("\"libros\""));
    }

    @Test
    void persisteSeriesYPortadasPersonalizadas() throws IOException {
        Path archivoCatalogo = carpetaTemporal.resolve("libros.json");
        CatalogoLibros catalogo = new CatalogoLibros(archivoCatalogo);
        Libro libro = new Libro("4", "Saga", "Autora", carpetaTemporal.resolve("saga.txt").toString(),
                "TXT");
        catalogo.agregar(libro);

        catalogo.actualizar(libro, actual -> {
            actual.setSerie("Trilogía");
            actual.setRutaPortada(carpetaTemporal.resolve("portada.jpg").toString());
        });

        CatalogoLibros recargado = new CatalogoLibros(archivoCatalogo);
        assertEquals(java.util.List.of("Trilogía"), recargado.listarSeries());
        assertEquals("Trilogía", recargado.listar().getFirst().getSerie());
        assertEquals(carpetaTemporal.resolve("portada.jpg").toString(),
                recargado.listar().getFirst().getRutaPortada());
    }

    @Test
    void persisteProgresoNotasYTiposDeNota() throws IOException {
        Path archivoCatalogo = carpetaTemporal.resolve("libros.json");
        CatalogoLibros catalogo = new CatalogoLibros(archivoCatalogo);
        Libro libro = new Libro("5", "Notas", "Autora", carpetaTemporal.resolve("notas.txt").toString(),
                "TXT");
        catalogo.agregar(libro);
        assertTrue(catalogo.crearTipoNota("Idea"));

        catalogo.actualizar(libro, actual -> {
            actual.setProgresoLectura(0.6);
            actual.setNotas(java.util.List.of(new Nota("n1", "5", "fragmento", "comentario",
                    "Idea", "#12AB34")));
        });

        CatalogoLibros recargado = new CatalogoLibros(archivoCatalogo);
        Libro persistido = recargado.listar().getFirst();
        assertEquals(0.6, persistido.getProgresoLectura());
        assertEquals(java.util.List.of("Idea"), recargado.listarTiposNotas());
        assertEquals("fragmento", persistido.getNotas().getFirst().getFragmentoOrigen());
        assertEquals("comentario", persistido.getNotas().getFirst().getContenidoTexto());
        assertEquals("#12AB34", persistido.getNotas().getFirst().getColorResaltado());
    }
}
