# Chat Application

This repository contains a simple chat application implemented using Java. The application consists of a server, a proxy, and multiple clients that can connect to the server through the proxy to exchange messages. Additionally, clients can send files to the server, and the server can broadcast messages to all connected clients.

## Features

- Allows multiple clients to connect to a server simultaneously.
- Clients can send text messages to the server, which will be broadcasted to all other connected clients.
- Clients can send files to the server, which will be saved on the server's file system.
- Server announcements are prefixed with `[SERVER]` to distinguish them from client messages.
- Basic encryption/decryption of messages to ensure privacy.

## Load Order

1. **Server:** Run the `Server` class to start the server on the specified port (default port is 5000).
2. **Proxy:** Run the `Proxy` class to start the proxy server on the specified port (default port is 6000).
3. **Client:** Run the `Client` class to start a client.

## Usage

1. **Server Setup:**
   - Run the `Server` class to start the server on the specified port (default port is 5000).

2. **Proxy Setup:**
   - Run the `Proxy` class to start the proxy server on the specified port (default port is 6000).

3. **Client Setup:**
   - Run the `Client` class to start a client.
   - Enter a unique nickname when prompted.
   - Start sending messages to communicate with other connected clients.

## Commands

- **Text Message:** Simply type your message in the chat input and press Enter or click the Send button.
- **Private Message:** Use the `/private` command followed by the recipient's nickname and the message (e.g., `/private John Hello John!`).
- **File Transfer:** Use the `/file` command followed by the file path to send a file to the server (e.g., `/file C:\example.txt`).
- **Exit:** Type `exit` to disconnect from the server and close the client application.

## Requirements

- Java Development Kit (JDK) 8 or higher
