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

                if (containsBannedWords(message)) {
                    writer.println("Your message contains banned words and was not sent.");
                    continue;
                }

                Main.broadcastMessage(clientName + ": " + message, this);
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

