package com.example.demo1;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.text.Text;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.lang.System.out;

public class HelloController {
    @FXML
    private Button botonEnviar1;
    @FXML
    private TextField mensajeU1;
    @FXML
    private TextArea chat1;
    @FXML
    public Button botonDesconectar1;
    @FXML
    public Pane pantallaNombre;
    @FXML
    public TextField nombreUsuario;
    @FXML
    public Button botonEntrar;
    @FXML
    public TextField nombreChat;
    @FXML
    public TextArea listaChats;

    //Lo que se cambió fue que añadimos sala y lista de salas que esos datos los vamos a tener que pasar a los usuarios

    @FXML
    public void initialize(){
        //Iniciar pool para clientes
        ExecutorService poolUsuarios = Executors.newFixedThreadPool(20);
        iniciarServidor();
        botonEntrar.setOnAction(evento -> {
            String nombreUsuarioFinal = nombreUsuario.getText();
            String sala = nombreChat.getText();
            //Antes de iniciar el servidor los botones de los usuarios debería estar desactivados
            configurarUsuario(nombreUsuarioFinal,botonEnviar1,mensajeU1,chat1,botonDesconectar1,sala,listaChats);
            pantallaNombre.setVisible(false);
            pantallaNombre.setDisable(true);
        });
    }

    public void configurarUsuario(String nombreUsuarioFinal, Button botonEnviar, TextField campoMensaje, TextArea zonaChat, Button botonDesconectar, String sala, TextArea listaChats){
        //Lista de los usuarios que se le pasan a la clase UsuarioInterfaz
        final UsuarioInterfaz[] usuario = {null};
        usuario[0] = new UsuarioInterfaz("localhost",8080,zonaChat,botonDesconectar,nombreUsuarioFinal,sala,listaChats);
        //Iniciamos un hilo de ese usuario
        new Thread(usuario[0]).start();
        //Primero escribimos un mensaje que si es nulo no hace nada, y ese mensaje se envia
        botonEnviar.setOnAction(evento -> {
            if(usuario[0] != null){
                usuario[0].enviarMensaje(campoMensaje.getText());
                campoMensaje.clear();
            }
        });
        //Al darle a desconectar, se desconecta en la clase de UusarioInterfaz
        botonDesconectar.setOnAction(evento -> {
            if(usuario[0] != null){
                usuario[0].desconectar();
                out.println("\nUsuario desconectado");
                //Y se cambia los datos de los botones
                botonDesconectar.setText("Ha salido");
            }
        });
    }
    //Función para iniciar el servidor y tenenmos que tener un hilo que inicie para que vaya teniendo en cuenta los chats que se van creando y enseñarlos en la pantalla principal de carga
    public void iniciarServidor(){
        //Para iniciar el servidor
        EchoServerMultihilo servidor = new EchoServerMultihilo(this);
        servidor.iniciarServidor();
        //Esto aparece en el panel del chat grupal
        out.println("Servidor Iniciado");
        //Activar todos los botones de los usuarios
        botonEnviar1.setDisable(false);
        Thread hiloLista = new Thread(() -> {
            try (Socket socketLista = new Socket("localhost", 8080);
                 BufferedReader entrada = new BufferedReader(new InputStreamReader(socketLista.getInputStream()));
                 PrintWriter salida = new PrintWriter(socketLista.getOutputStream(), true)) {
                 salida.println(" ");
                 salida.println(" ");

                 String respuesta;
                 while ((respuesta = entrada.readLine()) != null) {
                    if (respuesta.startsWith("Lista: ")) {
                        String soloNombres = respuesta.substring(7);
                        // IMPORTANTE: Esto actualiza el TextArea de la pantalla principal
                        Platform.runLater(() -> listaChats.setText(soloNombres));
                    }
                 }
            } catch (IOException e) {
                out.println("Error en hilo de escucha de salas");
            }
        });
        hiloLista.start();
    }
}