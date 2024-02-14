package org.chatapp.serverclient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class Proxy {
    private static final int PROXY_PORT = 6000;
    private ServerSocket proxyServerSocket;

    public Proxy() {
        try {
            // Initialize the Proxy with a ServerSocket on the specified port
            proxyServerSocket = new ServerSocket(PROXY_PORT);
            System.out.println("[PROXY] Proxy started on port " + PROXY_PORT);

            while (true) {
                // Accept incoming connection from a client
                Socket clientSocket = proxyServerSocket.accept();
                System.out.println("[PROXY] Client connected: " + clientSocket);

                // Connect to the actual server
                Socket serverSocket = new Socket("127.0.0.1", 5000);

                // Start separate threads for forwarding messages from client to server and vice versa
                new Thread(() -> forwardMessages(clientSocket, serverSocket)).start();
                new Thread(() -> forwardMessages(serverSocket, clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeProxyServerSocket();
        }
    }

    // Forwards messages between two sockets
    private void forwardMessages(Socket fromSocket, Socket toSocket) {
        try {
            // Set up input stream to read messages from one socket
            BufferedReader in = new BufferedReader(new InputStreamReader(fromSocket.getInputStream()));
            // Set up output stream to send messages to the other socket
            PrintWriter out = new PrintWriter(toSocket.getOutputStream(), true);

            String message;
            // Continuously read messages from one socket and forward them to the other
            while ((message = in.readLine()) != null) {
                out.println(message);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // Close both sockets when the forwarding is done
            closeSocket(fromSocket);
            closeSocket(toSocket);
        }
    }

    // Closes the proxy's ServerSocket
    private void closeProxyServerSocket() {
        try {
            if (proxyServerSocket != null && !proxyServerSocket.isClosed()) {
                proxyServerSocket.close();
                System.out.println("[PROXY] Proxy server socket closed.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Closes a given socket
    private void closeSocket(Socket socket) {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Main method to start the proxy
    public static void main(String[] args) {
        new Proxy();
    }
}
