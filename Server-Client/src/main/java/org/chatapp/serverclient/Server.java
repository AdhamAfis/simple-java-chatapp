package org.chatapp.serverclient;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    // Some initalizations to make the code more readable and understandble 
    private static final int PORT_NUMBER = 5000;
    private ServerSocket serverSocket;
    private static ArrayList<ClientHandler> clients;
    private static final int THREAD_POOL_SIZE = 10; // 10 concurrent threads are allowed to start together
    private static final ExecutorService threadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

    // Constructor to initialize the Server and listen for client connections
    public Server(int port) {
        try {
            serverSocket = new ServerSocket(port);
            // Using [SERVER] to make sure we don't have to decrypt server announcements because they are already not encrypted
            System.out.println("[SERVER] Server started.");
            clients = new ArrayList<>(); // Create an arraylist that will hold all online users/threads
            System.out.println("[SERVER] Waiting for Clients");

            while (true) {
                Socket clientSocket = serverSocket.accept(); // Accept any and every connection 
                System.out.println("[SERVER] New Client Connected: " + clientSocket);

                // Create a new ClientHandler for the connected client and start it in a new thread
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clients.add(clientHandler);
                threadPool.submit(clientHandler); // Add in to the limited-10 thread pool
            }
        } catch (IOException e) {
            System.err.println("[SERVER] Error starting the server: " + e.getMessage());
        } finally {
            closeServerSocket();
        }
    }

    // Closes the ServerSocket and shuts down the thread pool
    private void closeServerSocket() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
                System.out.println("[SERVER] ServerSocket closed.");
                threadPool.shutdownNow(); // Shutdown all threads in the pool
            }
        } catch (IOException e) {
            System.err.println("[SERVER] Error closing ServerSocket: " + e.getMessage());
        }
    }

    // Removes a client from the list of connected clients
    private static void removeClient(ClientHandler clientHandler) {
        clients.remove(clientHandler);
        System.out.println("[SERVER] Client removed: " + clientHandler.clientSocket);
    }

    // The ClientHandler class represents a thread that handles communication with a single client
    private static class ClientHandler implements Runnable {
        private Socket clientSocket;
        private BufferedReader in;
        private PrintWriter out;
        private String clientNickname;

        // Constructor to initialize the ClientHandler with the client's socket
        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
            try {
                // Set up input and output streams
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                out = new PrintWriter(clientSocket.getOutputStream(), true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Runs the thread, handling communication with the client
        @Override
        public void run() {
            try {
                authenticateUser();
                broadcast("[SERVER] " + clientNickname + " has joined the chat.", true);
                String message;
                while ((message = in.readLine()) != null) {
                    message = decryptMessage(message); // Decrypt incoming message
                    if (message.equalsIgnoreCase("exit")) { // Exit the thread
                        break;
                    } else if (message.startsWith("/private")) { // Calls the handlePrivateMessage method
                        handlePrivateMessage(message);
                    } else if (message.startsWith("/file")) { // Calls the handleFileTransfer method
                        handleFileTransfer(message);
                    } else {
                        broadcast(clientNickname + ": " + message, false); // If none of the above cases match, broadcast the message to everyone
                    }
                }
            } catch (IOException e) {
                System.out.println(e);
            } finally {
                closeResources();
                removeClient(this);
            }
        }

        // Handles the transfer of a file from the client to the server
        private void handleFileTransfer(String message) {
            try {
                String[] parts = message.split(" ", 2); 
                String fileName = parts[1]; // Split the message to take the filepath out
                String fileData = in.readLine(); // Read the encoded bytes coming in
                byte[] decodedFileData = Base64.getDecoder().decode(fileData); // Decode those bytes

                // Create a new file on the server and copy the decoded content
                Path filePath = Path.of("C:\\" + fileName);
                Files.write(filePath, decodedFileData);

                out.println("[SERVER] File received by " + clientNickname + " and saved at: " + filePath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Authenticates the user by prompting for a unique nickname
        private void authenticateUser() throws IOException {
            out.println("[SERVER] Enter your nickname:");

            while (true) {
                clientNickname = in.readLine();
                clientNickname = decryptMessage(clientNickname); // Decrypts the nickname to display it in the thread

                if (clientNickname == null) {
                    return;
                }

                if (!isNicknameTaken(clientNickname)) {
                    break;
                } else {
                    out.println("[SERVER] Nickname is already taken. Please choose another one:");
                }
            }
        }

        // Checks if a nickname is already in use by another client
        private boolean isNicknameTaken(String nickname) {
            for (ClientHandler client : clients) {
                if (client != this && client.clientNickname.equals(nickname)) { // To avoid giving a true if it's the same thread
                    return true;
                }
            }
            return false;
        }

        // Broadcasts a message to all clients or a private message to a specific client
        private void broadcast(String str, boolean server) {
            for (ClientHandler client : clients) {
                if (server || client != this) { // To avoid the server broadcasting, or the thread sending the message
                    client.out.println(encryptMessage(str));
                }
            }
        }

        // Handles a private message from one client to another
        private void handlePrivateMessage(String message) {
            String[] parts = message.split(" ", 3);
            String recipient = parts[1];
            String privateMessage = parts[2];

            for (ClientHandler client : clients) {
                if (client.clientNickname.equals(recipient)) {
                    // Send private message to the recipient and a confirmation to the sender
                    client.out.println("[SERVER] (Private from " + clientNickname + "): " + privateMessage);
                    out.println("[SERVER] (Private to " + recipient + "): " + privateMessage);
                    return;
                }
            }
        }

        // Closes resources associated with the client
        private void closeResources() {
            try {
                if (in != null) in.close();
                if (out != null) out.close();
                if (clientSocket != null) clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Encrypts a message using Base64 encoding
    private static String encryptMessage(String str) {
        if (str.startsWith("[SERVER]")) {
            return str;
        } else {
            byte[] bytesEncoded = Base64.getEncoder().encode(str.getBytes());
            return new String(bytesEncoded);
        }
    }

    // Decrypts a message using Base64 decoding
    private static String decryptMessage(String str) {
        if (Base64.getEncoder().encodeToString(str.getBytes()).equals(str)) {
            // The string is not Base64 encoded, return it as is
            return str;
        } else {
            // The string is Base64 encoded, decode it
            String originalString = new String(Base64.getDecoder().decode(str));
            return originalString;
        }
    }

    // Main method to start the server
    public static void main(String[] args) throws IOException {
        new Server(PORT_NUMBER);
    }
}
