package com.example.demo1;

import javafx.application.Platform;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.concurrent.*;

public class ManejadorUsuarioMultihilo implements Runnable {
    // Socket para comunicarse con el cliente asignado a este manejador
    private final Socket socket;
    // Identificador simple del usuario;
    private final HelloController controller;
    private String nombreUsuario;
    private String nombreChat;
    public PrintWriter salida;

    //Constructor
    public ManejadorUsuarioMultihilo(Socket socket, HelloController controller) {
        this.socket = socket;
        this.controller = controller;
    }

    public void enviarMensaje(String msg) {
        salida.println(msg);
    }

    @Override
    public void run() {
        try (BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            salida = new PrintWriter(socket.getOutputStream(), true);
            //Leer los datos que nos interesan el nombre del usuario y la sala
            this.nombreUsuario = entrada.readLine();
            this.nombreChat = entrada.readLine();

            synchronized (EchoServerMultihilo.salas){
                if(EchoServerMultihilo.salas.get(this.nombreChat) == null ){
                    EchoServerMultihilo.salas.put(this.nombreChat,new ArrayList<>());
                }
                EchoServerMultihilo.salas.get(this.nombreChat).add(this);
                EchoServerMultihilo.avisarCambioSala();
            }
            //Mensaje de bienvenida al usuario
            EchoServerMultihilo.enviarSala(nombreUsuario + " se ha unido al chat: " + nombreChat, nombreChat);
            //Mensaje al chat grupal
            Platform.runLater(() -> System.out.println("\nSe ha unido al chat: "+ nombreChat +" el usuario " + nombreUsuario));
            String mensaje;
            while ((mensaje = entrada.readLine()) != null) {
                //Para gestionar los comandos si empiezan en barra
                if(mensaje.startsWith("/")){
                    gestionarMensajes(mensaje);
                }
                else {
                    EchoServerMultihilo.enviarSala(nombreUsuario + " : " + mensaje, nombreChat);
                }
            }
        } catch (IOException e) {
            Platform.runLater(() -> System.out.println("Error en el usurario de " + nombreUsuario + "\n"));
        } finally {

        }
    }

    //La función para gestionar los comandos
    public void gestionarMensajes(String mensajeRecibido){
        String[] partes = mensajeRecibido.split(" ",2);
        String comando = partes[0].toLowerCase();

        switch(comando){
            //echar a alguien con la función de abajo y tiene que tener nombre al lado sino no hace nada
            case "/kick":{
                if(partes.length > 1){
                    eliminarUsuario(partes[1].toLowerCase());
                }
                else{
                    this.enviarMensaje("Debes de poner un nombre.");
                }
                break;
            }
            //Que se vea el mensaje en mayuscula
            case "/shout":{
                if(partes.length > 1){
                    String mensaje = partes[1];
                    EchoServerMultihilo.enviarSala(nombreUsuario + ": " + mensaje.toUpperCase(), nombreChat);
                }
                else{
                    this.enviarMensaje("No solo vale con el comando, escribe algo más");
                }
                break;
            }
            //Un número aleatorio de un dado
            case "/roll":{
                int dado = (int)(Math.random()*6)+1;
                EchoServerMultihilo.enviarSala(nombreUsuario + " sacó un : " + dado + " en el dado", nombreChat);
                break;
            }
            //liSta los usuarios
            case "/list":{
                ArrayList<ManejadorUsuarioMultihilo> lista = EchoServerMultihilo.salas.get(nombreChat);
                String resultado = "Usuarios en la sala: ";
                for (ManejadorUsuarioMultihilo u : lista) {
                    resultado += u.nombreUsuario + ",";
                }
                this.enviarMensaje(resultado);
                break;
            }
            //Para limpiar el chat
            case "/clear":{
                EchoServerMultihilo.enviarSala("Limpiar", nombreChat);
                break;
            }
            //Para cambiar el nombre del usuario
            case "/nick":{
                if(partes.length > 1){
                    String nombreViejo = this.nombreUsuario;
                    this.nombreUsuario = partes[1];
                    EchoServerMultihilo.enviarSala(nombreViejo + " ahora es " + nombreUsuario, nombreChat);
                }
                else{
                    this.enviarMensaje("Hay que poner un nuevo nombre");
                }
                break;
            }
            default:{
                this.enviarMensaje("Comando no válido");
                break;
            }
        }
    }

    //función para eliminar un usuario del chat cerrando su socket
    public void eliminarUsuario(String nombreEliminar){
        synchronized (EchoServerMultihilo.salas){
            ArrayList<ManejadorUsuarioMultihilo> usuarios = EchoServerMultihilo.salas.get(nombreChat);
            for (ManejadorUsuarioMultihilo usuario : usuarios) {
                String nombreUsuario = usuario.nombreUsuario.toLowerCase();
                if (nombreUsuario.equalsIgnoreCase(nombreEliminar)) {
                    usuario.enviarMensaje("Has sido expulsado del chat.");
                    try {
                        usuario.socket.close();
                    }
                    catch(Exception e){
                         e.printStackTrace();
                    }
                    return;
                }
            }
            this.enviarMensaje("No se encontró al usuario " + nombreEliminar);
        }
    }
}