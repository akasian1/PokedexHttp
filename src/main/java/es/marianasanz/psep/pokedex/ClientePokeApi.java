package es.marianasanz.psep.pokedex;

import java.io.*;
import java.math.BigInteger;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;

public class ClientePokeApi {
    private final HttpClient cliente;
    private final URI base;
    private final Map<String, Pokemon> cache = new HashMap<>();

    public ClientePokeApi() {
        this(URI.create("https://pokeapi.co/api/v2/pokemon/"));
    }
    /** La URL configurable permite pruebas locales sin depender de Internet. */
    public ClientePokeApi(URI base) {
        if (!base.toString().endsWith("/")) throw new IllegalArgumentException("La URL base debe terminar en /");
        this.base = base;
        this.cliente = crearClienteHttp();
    }
    // Actividad principal: un solo cliente reutilizado para todas las peticiones.
    public static HttpClient crearClienteHttp() {
        return HttpClient
                .newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }
    public static String normalizarEntrada(String entrada) throws ExcepcionPokedex {
        if (entrada.isEmpty()) {
            throw new ExcepcionPokedex("El nombre de Pokemon no puede estar vacio.");
        }
        String clave = entrada.toLowerCase().trim();
        if (clave.matches("[0-9]+")) {
            BigInteger numero = new BigInteger(clave);
            if (numero.signum() <= 0) throw new ExcepcionPokedex("El identificador debe ser un entero positivo.");
            return numero.toString();
        }
        if (!clave.matches("[a-z0-9-]+") || !clave.matches(".*[a-z].*")) {
            throw new ExcepcionPokedex("Usa un nombre con letras ASCII, números y guiones, o un identificador positivo.");
        }
        // Es un segmento de ruta validado; no se usa URLEncoder para parámetros.
        return clave;
    }
    public Pokemon consultarPokemon(String entrada) throws ExcepcionPokedex, InterruptedException {
        try {
            String pokemon = normalizarEntrada(entrada);
            String url = base.toString() + pokemon;
            System.out.println("URL a consultar : "+url);

            HttpRequest httpRequest = HttpRequest
                    .newBuilder()
                    .GET()
                    .uri(URI.create(url))
                    .header("Accept", "application/json")
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> respuesta = cliente.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            mostrarMetadatos(respuesta);


            if (respuesta.statusCode() == 404) throw new ExcepcionPokedex("HTTP 404: no existe el Pokémon solicitado: " + pokemon);
            if (respuesta.statusCode() != 200) throw new ExcepcionPokedex("Error HTTP " + respuesta.statusCode() + ": no se procesa la ficha.");

            return Pokemon.desdeJson(respuesta.body());
        } catch (HttpTimeoutException error) {
            throw new ExcepcionPokedex("Tiempo de espera agotado al consultar el Pokémon.", error);
        } catch (IOException error) {
            throw new ExcepcionPokedex("Problema de conexión o lectura HTTP: " + error.getMessage(), error);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw error;
        }
    }
    private void mostrarMetadatos(HttpResponse<?> respuesta) {
        System.out.println("El código de estado HTTP : "+respuesta.statusCode());
        System.out.println("La versión HTTP utilizada en la respuesta : "+respuesta.version());
        if (!respuesta.headers().allValues("Content-Type").isEmpty()) {
            System.out.println("El valor de la cabecera Content-Type : "+respuesta.headers().allValues("Content-Type").get(0));
        }
    }
    // Ampliación 2: imágenes.
    public String obtenerUrlImagen(Pokemon pokemon) throws ExcepcionPokedex {
        String url = pokemon.getUrlImagen();
        if (url == null || url.isBlank()) throw new ExcepcionPokedex("No hay imagen disponible para " + pokemon.getNombre() + ".");
        return url;
    }
    public Path descargarImagen(Pokemon pokemon, Path destino) throws ExcepcionPokedex, InterruptedException {
        URI url = null;
        try {
            //TODO 1
            if (!("https".equalsIgnoreCase(url.getScheme()) || "http".equalsIgnoreCase(url.getScheme())) || url.getHost() == null)
                throw new IllegalArgumentException("La imagen necesita una URL HTTP válida");
        } catch (IllegalArgumentException error) {
            throw new ExcepcionPokedex("URL de imagen no válida.", error);
        }
        Path temporal = null;
        try {
            Path carpeta = destino.toAbsolutePath().getParent();
            Files.createDirectories(carpeta);
            temporal = Files.createTempFile(carpeta, "imagen-", ".tmp");
            HttpRequest peticion = null;//TODO 2
            // ofFile escribe el cuerpo en disco, incluso si el servidor responde con un error.
            HttpResponse<Path> respuesta = null;//TODO 3
            //TODO 4
            if (respuesta.statusCode() != 200) throw new ExcepcionPokedex("Error HTTP " + respuesta.statusCode() + " al descargar la imagen.");
            String tipo = respuesta.headers().firstValue("Content-Type").orElse("")
                    .split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
            if (!tipo.matches("image/[a-z0-9.+-]+") || Files.size(temporal) == 0) {
                throw new ExcepcionPokedex("La respuesta no contiene una imagen válida (Content-Type o cuerpo vacío).");
            }
            // Solo una respuesta válida sustituye la imagen previa.
            Files.move(temporal, destino, StandardCopyOption.REPLACE_EXISTING);
            temporal = null;
            return destino;
        } catch (HttpTimeoutException error) {
            throw new ExcepcionPokedex("Tiempo de espera agotado al descargar la imagen.", error);
        } catch (IOException error) {
            throw new ExcepcionPokedex("Error de conexión o de archivo al descargar la imagen: " + error.getMessage(), error);
        } /*catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw error;
        }*/ finally {
            if (temporal != null) {
                try { Files.deleteIfExists(temporal); }
                catch (IOException error) { System.out.println("No se pudo borrar el temporal " + temporal + ": " + error.getMessage()); }
            }
        }
    }
}
