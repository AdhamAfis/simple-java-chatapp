package org.chatapp.serverclient;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.io.IOException;
import javafx.application.Platform;

public class Client extends Application {
    // Server address and port
    private static final String SERVER_ADDRESS = "127.0.0.1";
    private static final int SERVER_PORT = 5000;
    
    // Socket and I/O streams
    private Socket socket;
    private BufferedReader inputFromServer;
    private PrintWriter outputToServer;
    
    // UI components
    private TextArea chatArea;
    private TextField messageField;

    // Method to encrypt a message
    private String encryptMessage(String str) {
        if (!str.startsWith("[SERVER]")) {
            // Encode the message using Base64 encoding
            byte[] bytesEncoded = Base64.getEncoder().encode(str.getBytes());
            return new String(bytesEncoded);
        }
        return str;
    }

    // Method to decrypt a message
    private String decryptMessage(String str) {
        if (Base64.getEncoder().encodeToString(str.getBytes()).equals(str)) {
            // If the message is already encoded, return it as is
            return str;
        } else {
            // Decode the message using Base64 decoding
            String originalString = new String(Base64.getDecoder().decode(str));
            return originalString;
        }
    }

    // Constructor
    public Client() {
        try {
            // Create a socket and initialize I/O streams
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            inputFromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            outputToServer = new PrintWriter(socket.getOutputStream(), true);
            
            // Start a new thread to receive messages from the server
            new Thread(this::receiveMessages).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Method to receive messages from the server
    public void receiveMessages() {
        try {
            String serverMessage;
            while ((serverMessage = inputFromServer.readLine()) != null) {
                if (serverMessage.startsWith("[SERVER]")) {
                    // Append server messages directly to the chat area
                    appendToChatArea(serverMessage);
                } else {
                    // Decrypt and append other messages to the chat area
                    appendToChatArea(decryptMessage(serverMessage));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // Close resources when done
            closeResources();
        }
    }

    // Method to send a message to the server
    private void sendMessage(String message) {
        if (message.startsWith("/sendfile")) {
            // If the message is a file command, send the file
            sendFile(message);
        } else {
            // Encrypt and send the message
            String encrypted = encryptMessage(message);
            // Append the decrypted message to the chat area
            appendToChatArea("[Me]: " + decryptMessage(encrypted));
            outputToServer.println(encrypted);
        }
    }

    // Method to send a file to the server
    private void sendFile(String message) {
        String[] parts = message.split(" ", 2);
        String filePath = parts[1];
        try {
            // Read the file data and encode it using Base64
            Path path = Paths.get(filePath);
            byte[] fileData = Files.readAllBytes(path);
            String fileName = path.getFileName().toString();
            String encrypted = encryptMessage("/file " + fileName);
            
            // Send the file name and data to the server
            outputToServer.println(encrypted);
            outputToServer.println(Base64.getEncoder().encodeToString(fileData));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Method to close the socket and I/O streams
    public void closeResources() {
        try {
            if (inputFromServer != null) inputFromServer.close();
            if (outputToServer != null) outputToServer.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Method to append a message to the chat area
    private void appendToChatArea(String message) {
        Platform.runLater(() -> {
            // Append the message to the chat area and add a new line
            chatArea.appendText(message + "\n");
        });
    }


    @Override
    public void start(Stage primaryStage) {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        chatArea = new TextArea();
        chatArea.setEditable(false);
        root.setCenter(chatArea);

        messageField = new TextField();
        messageField.setOnAction(e -> {
            String message = messageField.getText();
            sendMessage(message);
            messageField.clear();
        });

        Button sendButton = new Button("Send");
        sendButton.setOnAction(e -> {
            String message = messageField.getText();
            sendMessage(message);
            messageField.clear();
        });

        VBox inputBox = new VBox(10, messageField, sendButton);
        root.setBottom(inputBox);

        Scene scene = new Scene(root, 400, 300);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Chat Client");
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
