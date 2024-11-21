import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

public class Main {
    private static final List<ClientHandler> clientHandlers = new ArrayList<>();
    private static String serverName;
    private static int port;
    private static List<String> bannedWords;

    public static void main(String[] args) {
        loadConfiguration("C:\\Users\\User\\IdeaProjects\\TCPServer\\src\\server_config.txt");

        System.out.println("Starting " + serverName + " on port " + port);
        new Thread(() -> startServer(port)).start();

        // Start multiple clients for testing (optional)
        int numberOfClients = 3;
        String host = "127.0.0.1";
        for (int i = 0; i < numberOfClients; i++) {
            int clientId = i + 1;
            new Thread(() -> ClientSimulator.runClient(host, port, "Client" + clientId)).start();
        }
    }

    private static void loadConfiguration(String configFilePath) {
        try (InputStream input = new FileInputStream(configFilePath)) {
            Properties properties = new Properties();
            properties.load(input);

            serverName = properties.getProperty("ServerName", "DefaultServer");
            port = Integer.parseInt(properties.getProperty("Port", "12345"));
            String bannedWordsString = properties.getProperty("BannedWords", "");
            bannedWords = Arrays.asList(bannedWordsString.split(","));

        } catch (IOException e) {
            System.out.println("Error loading configuration: " + e.getMessage());
            serverName = "DefaultServer";
            port = 12345;
            bannedWords = new ArrayList<>();
        }
    }

    public static void startServer(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println(serverName + " is listening on port " + port);

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("New client connected");

                ClientHandler clientHandler = new ClientHandler(socket);
                synchronized (clientHandlers) {
                    clientHandlers.add(clientHandler);
                }
                clientHandler.start();
            }
        } catch (IOException ex) {
            System.out.println("Server exception: " + ex.getMessage());
        }
    }

    public static void broadcastMessage(String message, ClientHandler sender) {
        synchronized (clientHandlers) {
            for (ClientHandler client : clientHandlers) {
                if (client != sender) {
                    client.sendMessage(message);
                }
            }
        }
    }

    public static void sendMessageToClients(String message, List<String> recipientNames, ClientHandler sender) {
        synchronized (clientHandlers) {
            for (ClientHandler client : clientHandlers) {
                if (recipientNames.contains(client.getClientName()) && client != sender) {
                    client.sendMessage(message);
                }
            }
        }
    }

    public static void removeClient(ClientHandler clientHandler) {
        synchronized (clientHandlers) {
            clientHandlers.remove(clientHandler);
        }
    }

    public static List<String> getBannedWords() {
        return bannedWords;
    }

    public static List<String> getConnectedClients() {
        synchronized (clientHandlers) {
            return clientHandlers.stream()
                    .map(ClientHandler::getClientName)
                    .collect(Collectors.toList());
        }
    }

    public static void broadcastMessageExcluding(String message, List<String> excludedNames, ClientHandler sender) {
        synchronized (clientHandlers) {
            for (ClientHandler client : clientHandlers) {
                if (!excludedNames.contains(client.getClientName()) && client != sender) {
                    client.sendMessage(message);
                }
            }
        }
    }

    // Send list of clients and instructions to new clients
    public static void sendInitialInformation(ClientHandler clientHandler) {
        String clientList = String.join(", ", getConnectedClients());
        String instructions = "Send a message to all: <message>\n" +
                "Send a private message to a specific client: @username: <message>\n" +
                "Send to multiple clients: @username1,username2: <message>\n" +
                "Exclude certain clients from a message: !exclude username1,username2: <message>\n" +
                "Query banned words: !banned";

        clientHandler.sendMessage("Connected clients: " + clientList);
        clientHandler.sendMessage("Usage instructions:\n" + instructions);
    }
}
