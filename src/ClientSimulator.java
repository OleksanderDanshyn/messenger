import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.Socket;

public class ClientSimulator {
    public static void runClient(String host, int port, String clientName) {
        try {
            Socket socket = new Socket(host, port);
            System.out.println(clientName + " connected to the server.");

            JFrame frame = new JFrame(clientName);
            frame.setSize(700, 700);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new BorderLayout());

            JTextPane messageArea = new JTextPane();
            messageArea.setContentType("text/html");
            messageArea.setEditable(false);
            frame.add(new JScrollPane(messageArea), BorderLayout.CENTER);

            JTextField inputField = new JTextField();
            frame.add(inputField, BorderLayout.SOUTH);
            frame.setVisible(true);

            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            String serverMessage = reader.readLine();
            if (serverMessage != null && serverMessage.startsWith("Enter your name:")) {
                writer.println(clientName);
                appendMessage(messageArea, "Connected to the server. You are " + clientName + ".<br>");
            } else {
                appendMessage(messageArea, "Error during handshake with the server.<br>");
                socket.close();
                return;
            }

            new Thread(() -> receiveMessages(reader, messageArea)).start();

            inputField.addActionListener(e -> {
                String message = inputField.getText();
                if (!message.isEmpty()) {
                    writer.println(message);
                    inputField.setText("");

                    appendMessage(messageArea, "<span style='color: green;'><b>You:</b> " + message + "</span><br>");

                    if (message.equalsIgnoreCase("bye")) {
                        try {
                            socket.close();
                            appendMessage(messageArea, "Disconnected from the server.<br>");
                        } catch (IOException ex) {
                            appendMessage(messageArea, "Error disconnecting: " + ex.getMessage() + "<br>");
                        }
                    }
                }
            });

        } catch (IOException e) {
            System.out.println(clientName + " connection error: " + e.getMessage());
        }
    }

    private static void receiveMessages(BufferedReader reader, JTextPane messageArea) {
        try {
            String serverMessage;
            while ((serverMessage = reader.readLine()) != null) {
                appendMessage(messageArea, serverMessage + "<br>");
            }
        } catch (IOException e) {
            appendMessage(messageArea, "Disconnected from server.<br>");
        }
    }

    private static void appendMessage(JTextPane textPane, String message) {
        try {
            String existingText = textPane.getText();
            String newText = existingText.replace("</body>", message + "</body>");
            textPane.setText(newText);
        } catch (Exception e) {
            textPane.setText(message);
        }
    }
}