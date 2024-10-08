import java.io.*;
import java.net.*;

public class Client {
    private Socket socket = null;
    private DataInputStream input = null;
    private DataInputStream in = null;
    private DataOutputStream out = null;
    private static String ADDRESS = "0.0.0.0";

    public Client(String address, int port) {
        try {
            socket = new Socket(address, port);
            System.out.println("Connected");

            // Input from console
            input = new DataInputStream(System.in);

            // Input from server
            in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));

            // Output to server
            out = new DataOutputStream(socket.getOutputStream());

            // Register player name
            System.out.print("Enter your name: ");
            String playerName = input.readLine();
            out.writeUTF("REGISTER " + playerName);
            out.flush();

            // Read response for registration
            String response = in.readUTF();
            System.out.println(response);

            if(response.startsWith("200")) {
                System.out.println("Registration successful");
                System.out.println(playerName + " has join game");
                System.out.println("-------------------------");
                System.out.println("Available Command");
                System.out.println("  - START : start the game.");
                System.out.println("  - HIT : ask for one more card.");
                System.out.println("  - PASS : pass your turn, if all player already passed game will end.");
                System.out.println("  - SEE : show first card of all players.");
                System.out.println("  - EXIT : exit the game.");
                System.out.println("-------------------------");

                // Thread to read messages from the server
                Thread readThread = new Thread(() -> {
                    try {
                        while (!socket.isClosed()) {
                            String serverMessage = in.readUTF();
                            if(serverMessage.equals("START")) {
                                out.writeUTF("PLAY");
                            }
                            System.out.println(serverMessage);
                        }
                    } catch (IOException e) {
                        System.out.println(e);
                    }
                });
                readThread.start();

                String line = "";

                // Main loop to send messages to the server
                while (!line.equals("EXIT")) {
                    try {
                        line = input.readLine();
                        out.writeUTF(line);
                        out.flush();
                    } catch (IOException i) {
                        System.out.println(i);
                    }
                }
            }
        } catch (UnknownHostException u) {
            System.out.println(u);
        } catch (IOException i) {
            System.out.println(i);
        } finally {
            // Close resources
            try {
                input.close();
                out.close();
                socket.close();
            } catch (IOException i) {
                System.out.println(i);
            }
        }
    }

    public static void main(String args[]) {
        Client client = new Client(ADDRESS, 5000);
    }
}
