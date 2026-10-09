package es.marianasanz.psep.pokedex;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.InputMismatchException;
import java.util.Scanner;

public class Main {
    private final Scanner entrada;
    private final ClientePokeApi cliente = new ClientePokeApi();
    private final GestorArchivos archivos = new GestorArchivos();
    private final ServicioPokedex servicio = new ServicioPokedex();
    private Main(Scanner entrada) { this.entrada = entrada; }

    public static void main(String[] argumentos) {
        // Mantener la salida en UTF-8 también en terminales Windows.
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        try (Scanner entrada = new Scanner(System.in, StandardCharsets.UTF_8)) {
            new Main(entrada).ejecutar();
        }
    }
    // Actividad principal: cada opción lee líneas con el mismo Scanner.
    private void ejecutar() {
        boolean continuar = true;
        while (continuar && !Thread.currentThread().isInterrupted()) {
            System.out.println("\n1. Consultar Pokémon.\n2. Descargar imagen de un Pokémon.\n3. Comparar dos Pokémon.\n0. Salir.");
            System.out.print("Opción: ");
            if (!entrada.hasNextLine()) throw new InputMismatchException();
            String opcion = entrada.nextLine();
            if (opcion == null) break;
            try {
                switch (opcion.trim()) {
                    case "1" -> consultar();
                    case "2" -> descargar();
                    case "3" -> comparar();
                    case "0" -> continuar = false;
                    default -> System.out.println("Opción incorrecta. Introduce 0, 1, 2 o 3.");
                }
            } catch (ExcepcionPokedex error) {
                System.out.println(error.getMessage());
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                System.out.println("Operación interrumpida. Se termina de forma ordenada.");
                continuar = false;
            }
        }
        System.out.println("Fin de la Pokédex.");
    }

    private Pokemon solicitarPokemon(String mensaje) throws ExcepcionPokedex, InterruptedException {
        System.out.print(mensaje);
        if (!entrada.hasNextLine()) return null;
        String texto = entrada.nextLine();
        if (texto == null) return null;
        Pokemon pokemon = cliente.consultarPokemon(texto);
        return pokemon;
    }
    private void guardarFicha(Pokemon pokemon) {
        // TODO
        try {
            archivos.guardarFicha(pokemon);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    private void consultar() throws ExcepcionPokedex, InterruptedException {
        //TODO
        Pokemon pokemon = solicitarPokemon("Introduce nombre o el identificador : ");
        servicio.mostrarFicha(pokemon);
        guardarFicha(pokemon);
    }
    // Ampliación 2: imágenes.
    private void descargar() throws ExcepcionPokedex, InterruptedException {
        // TODO
        Pokemon pokemon = solicitarPokemon("Introduce nombre o el identificador : ");
        Path destino = archivos.rutaImagen(pokemon); // TODO
        if (Files.exists(destino)) {
            while (true) {
                System.out.print("La imagen ya existe. ¿Conservar (c) o descargar de nuevo (d)? ");
                if (!entrada.hasNextLine()) throw new InputMismatchException();
                String respuesta = entrada.nextLine();
                if (respuesta == null) return;
                if (respuesta.trim().equalsIgnoreCase("c")) {
                    System.out.println("Imagen conservada: " + destino.toAbsolutePath());
                    return;
                }
                if (respuesta.trim().equalsIgnoreCase("d")) break;
                System.out.println("Introduce c o d.");
            }
        }
        // TODO: Si no está la imagen, entonces hay que descargarla.
        cliente.descargarImagen(pokemon,destino);


    }
    // Ampliación 3: comparación.
    private void comparar() throws ExcepcionPokedex, InterruptedException {
        //TODO
        Pokemon pokemon1 = solicitarPokemon("Introduce nombre o el identificador del primer Pokemon : ");
        if (pokemon1 == null) {
            System.out.println("No se ha encontrado pokemon introducido");
            return;
        }
        Pokemon pokemon2 = solicitarPokemon("Introduce nombre o el identificador del segundo Pokemon : ");
        if (pokemon2 == null) {
            System.out.println("No se ha encontrado pokemon introducido");
            return;
        }

        ResultadoComparacion resultado = servicio.compararPokemon(pokemon1,pokemon2);
        System.out.println(resultado.informe());
        //TODO
        try { System.out.println("Informe guardado en: " + archivos.guardarComparacion(resultado).toAbsolutePath()); }
        catch (IOException error) { System.out.println("No se pudo guardar el informe: " + error.getMessage()); }
    }
}
