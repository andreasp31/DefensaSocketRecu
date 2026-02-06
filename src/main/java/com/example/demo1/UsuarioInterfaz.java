package com.example.demo1;

import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class UsuarioInterfaz implements Runnable{
    //Atributos del usuario
    private String host;
    private int puerto;
    private TextArea areaChat;
    private PrintWriter salida;
    private Button botonDesconectar;
    private Socket socket;
    private boolean conectado = true;
    private String nombreUsuario;
    public String nombreChat;
    public TextArea listaSalas;

    //Le añadimos el atributo de la lista de sala y nombre del chat

    //Constructor
    public UsuarioInterfaz(String host, int puerto, TextArea areaChat, Button botonDesconectar, String nombreUsuario, String nombreChat,TextArea listaSalas) {
        this.host = host;
        this.puerto = puerto;
        this.areaChat = areaChat;
        this.botonDesconectar = botonDesconectar;
        this.nombreUsuario = nombreUsuario;
        this.nombreChat= nombreChat;
        this.listaSalas = listaSalas;
    }
    //Función de enviar mensaje
    public void enviarMensaje(String msg) {
        if (salida != null) salida.println(msg);
    }


    @Override
    public void run() {
        //Iniciar el socket
        try (Socket socket = new Socket(host, puerto);
             BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            salida = new PrintWriter(socket.getOutputStream(), true);

            enviarMensaje(nombreUsuario);
            enviarMensaje(nombreChat);

            String respuesta;
            //Mensajes de los usuarios
            while (conectado && (respuesta = entrada.readLine()) != null) {
                //Si el mensaje empieza con lista cogemos los nombres y los enseñamos en la pantalla de lista de chats
                if(respuesta.startsWith("Lista: ")){
                    String soloNombres = respuesta.substring(7);
                    Platform.runLater(() -> {
                        listaSalas.setText(soloNombres);
                    });
                }
                //Esto es para limpiar el chat con el comando de clear
                else if(respuesta.equals("Limpiar")){
                    Platform.runLater(()->areaChat.clear());
                }
                else{
                    String finalRespuesta = respuesta;
                    Platform.runLater(() -> areaChat.appendText(finalRespuesta + "\n"));
                }

            }
        } catch (IOException e) {
            //Si hay un error
            Platform.runLater(() -> areaChat.appendText("Desconectado del servidor.\n"));
        }
    }

    //Cerrar el socket para desconectar el puente del cliente
    public void desconectar() {
        conectado = false;
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
                Platform.runLater(() -> System.out.println("Se ha desconectado " + nombreUsuario + "\n"));
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        Platform.runLater(() ->
                areaChat.appendText("Te has desconectado.\n"));
    }
}