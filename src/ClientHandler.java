import java.io.*;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;

class ClientHandler extends Thread {
    private Socket socket;
    private String clientName;
    private BufferedReader reader;
    private PrintWriter writer;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            InputStream input = socket.getInputStream();
            reader = new BufferedReader(new InputStreamReader(input));
            OutputStream output = socket.getOutputStream();
            writer = new PrintWriter(output, true);

            // Request and validate client name
            writer.println("Enter your name:");
            clientName = reader.readLine();

            if (clientName == null || clientName.trim().isEmpty()) {
                System.out.println("Client failed to provide a valid name. Disconnecting.");
                socket.close();
                return;
            }

            System.out.println(clientName + " has joined");
            Main.broadcastMessage(clientName + " has joined the chat", this);

            // Handle client messages
            while (true) {
                String message = reader.readLine();
                if (message == null || message.equalsIgnoreCase("bye")) {
                    System.out.println(clientName + " has disconnected");
                    Main.broadcastMessage(clientName + " has left the chat", this);
                    break;
                }

                if (message.equalsIgnoreCase("!banned")) {
                    writer.println("Banned words: " + String.join(", ", Main.getBannedWords()));
                    continue;
                }

                if (message.equalsIgnoreCase("!info")) {
                    sendInformation();
                    continue;
                }

                if (containsBannedWords(message)) {
                    writer.println("Your message contains banned words and was not sent.");
                    continue;
                }

                // Handle specific commands
                if (message.startsWith("@")) {
                    handlePrivateMessage(message);
                } else if (message.startsWith("!exclude")) {
                    handleExclusionMessage(message);
                } else {
                    Main.broadcastMessage(clientName + ": " + message, this);
                }
            }

        } catch (IOException ex) {
            System.out.println("Error handling client: " + ex.getMessage());
        } finally {
            try {
                socket.close();
                Main.removeClient(this);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void sendInformation() {
        String clientList = String.join(", ", Main.getConnectedClients());
        String instructions = "Send a message to all: <message>\n" +
                "Send a private message to a specific client: @username: <message>\n" +
                "Send to multiple clients: @username1,username2: <message>\n" +
                "Exclude certain clients from a message: !exclude username1,username2: <message>\n" +
                "Query banned words: !banned";

        writer.println("Connected clients: " + clientList);
        writer.println("Usage instructions:\n" + instructions);
    }


    // Handle private messages
    private void handlePrivateMessage(String message) {
        String[] parts = message.split(":", 2);
        if (parts.length == 2) {
            String recipients = parts[0].substring(1).trim(); // Extract usernames
            String actualMessage = parts[1].trim();
            List<String> recipientNames = Arrays.asList(recipients.split(","));
            Main.sendMessageToClients(clientName + " (private): " + actualMessage, recipientNames, this);
        } else {
            writer.println("Invalid format. Use @username: message or @username1,username2: message");
        }
    }

    // Handle exclusion messages
    private void handleExclusionMessage(String message) {
        String[] parts = message.split(":", 2);
        if (parts.length == 2) {
            String excluded = parts[0].substring(8).trim(); // Extract excluded usernames
            String actualMessage = parts[1].trim();
            List<String> excludedNames = Arrays.asList(excluded.split(","));
            Main.broadcastMessageExcluding(clientName + ": " + actualMessage, excludedNames, this);
        } else {
            writer.println("Invalid format. Use !exclude username1,username2: message");
        }
    }


    private boolean containsBannedWords(String message) {
        for (String bannedWord : Main.getBannedWords()) {
            if (message.toLowerCase().contains(bannedWord.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    public void sendMessage(String message) {
        writer.println(message);
    }

    public String getClientName() {
        return clientName;
    }
}

