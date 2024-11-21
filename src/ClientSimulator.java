import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Socket;

public class ClientSimulator {
    public static void runClient(String host, int port, String clientName) {
        try {
            Socket socket = new Socket(host, port);
            System.out.println(clientName + " connected to the server.");

            // GUI setup
            JFrame frame = new JFrame(clientName);
            frame.setSize(400, 400);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new BorderLayout());

            JTextArea messageArea = new JTextArea();
            messageArea.setEditable(false);
            frame.add(new JScrollPane(messageArea), BorderLayout.CENTER);

            JTextField inputField = new JTextField();
            frame.add(inputField, BorderLayout.SOUTH);
            frame.setVisible(true);

            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Send client name
            String serverMessage = reader.readLine();
            if (serverMessage != null && serverMessage.startsWith("Enter your name:")) {
                writer.println(clientName);
                messageArea.append("Connected to the server. You are " + clientName + ".\n");
            } else {
                messageArea.append("Error during handshake with the server.\n");
                socket.close();
                return;
            }

            // Start a thread to receive messages
            new Thread(() -> receiveMessages(reader, messageArea)).start();

            inputField.addActionListener(e -> {
                String message = inputField.getText();
                if (!message.isEmpty()) {
                    writer.println(message);
                    inputField.setText("");
                    if (message.equalsIgnoreCase("bye")) {
                        try {
                            socket.close();
                            messageArea.append("Disconnected from the server.\n");
                        } catch (IOException ex) {
                            messageArea.append("Error disconnecting: " + ex.getMessage() + "\n");
                        }
                    }
                }
            });

        } catch (IOException e) {
            System.out.println(clientName + " connection error: " + e.getMessage());
        }
    }

    private static void receiveMessages(BufferedReader reader, JTextArea messageArea) {
        try {
            String serverMessage;
            while ((serverMessage = reader.readLine()) != null) {
                messageArea.append(serverMessage + "\n");
            }
        } catch (IOException e) {
            messageArea.append("Disconnected from server.\n");
        }
    }
}
