package com.example.demo1;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;


public class EchoServerMultihilo {

    private static final int PUERTO = 8080;
    // número máximo de usuarios que usen el chat al mismo tiempo
    private static final int MAX_CLIENTES = 20;

    // Contador de usuarios
    private static final AtomicInteger contadorUsuarios = new AtomicInteger(0);

    //Lista de todos los usuarios conectados y cada usuario tiene un PrintWriter para enviarle mensajes
    public static final HashMap<String,ArrayList<ManejadorUsuarioMultihilo>> salas = new HashMap<>();
    //El controlador
    private final HelloController controller;

    //Constructor
    public EchoServerMultihilo(HelloController controller) {
        this.controller = controller;
    }

    public void iniciarServidor() {
        ExecutorService pool = Executors.newFixedThreadPool(MAX_CLIENTES);

        //Nuevo hilo para iniciar el servidor
        new Thread(() -> {
            //nuevo socket del server
            try(ServerSocket serverSocket = new ServerSocket(PUERTO)) {
                while(true){
                    Socket usuarioSocket = serverSocket.accept();
                    pool.execute(new ManejadorUsuarioMultihilo(usuarioSocket,controller));
                }
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }).start();

    }

    public static void enviarSala(String mensaje, String nombreSala){
        synchronized (salas){
            ArrayList<ManejadorUsuarioMultihilo> lista = salas.get(nombreSala);

            if(lista != null){
                for(ManejadorUsuarioMultihilo usuario : lista){
                    usuario.enviarMensaje(mensaje);
                }
            }
        }
    }
    public static String obtenerSala(){
        synchronized (salas){
            if(salas.isEmpty()){
                return "";
            }

            return String.join(" ",salas.keySet());
        }
    }
    //Esto lo tuve que buscar porque no era capaz de listar el nombre de todas las salas en la pantalla principal de cada inicio de usuario
    public static void avisarCambioSala(){
        String listaSalas ="Lista: " + obtenerSala();
        synchronized (salas){
            for(ArrayList<ManejadorUsuarioMultihilo> lista : salas.values()){
                for(ManejadorUsuarioMultihilo usuario : lista){
                    usuario.enviarMensaje(listaSalas);
                }
            }
        }
    }
}