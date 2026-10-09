package es.marianasanz.psep.pokedex;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

public class GestorArchivos {
    private final Path carpeta;
    public GestorArchivos() { this(Path.of("resources")); }
    public GestorArchivos(Path carpeta) { this.carpeta = carpeta; }

    // Actividad principal
    public Path guardarFicha(Pokemon pokemon) throws IOException {
        // TODO
        if (pokemon == null) return null;
        Path path1 = carpeta.resolve("pokemon").resolve(pokemon.getNombre().toLowerCase()+".json");
        Path path2 = escribir(path1,pokemon.getJsonOriginal());
        System.out.println("Se ha guardado en "+path2.toAbsolutePath());
        return path2;
    }
    // Ampliación 2: imágenes.
    public Path rutaImagen(Pokemon pokemon) {
        //TODO
        if (pokemon == null) return null;
        Path path = carpeta.resolve("imagenes").resolve(pokemon.getNombre().toLowerCase()+".png");
        return path;
    }
    // Ampliación 3: comparación. Ya hecho
    public Path guardarComparacion(ResultadoComparacion resultado) throws IOException {
        return escribir(carpeta.resolve("comparaciones").resolve(resultado.primero().getNombre()
                + "_vs_" + resultado.segundo().getNombre() + ".txt"), resultado.informe());
    }
    private Path escribir(Path destino, String contenido) throws IOException {
        Files.createDirectories(destino.getParent());
        // writeString usa CREATE y TRUNCATE_EXISTING: crea o sustituye, siempre en UTF-8.
        Files.writeString(destino, contenido, StandardCharsets.UTF_8);
        return destino;
    }
}
