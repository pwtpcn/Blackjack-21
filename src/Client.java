import java.io.*;
import java.net.*;

public class Client {
    private Socket socket = null;
    private DataInputStream input = null;
    private DataInputStream in = null;
    private DataOutputStream out = null;
    private static String ADDRESS = "0.0.0.0";
//    private static String ADDRESS = "192.168.133.187";

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
//            System.out.print(playerName + " has join game");
//            System.out.println("");
            out.writeUTF("REGISTER " + playerName);
            out.flush();

            // Read response for registration
            String response = in.readUTF();
            System.out.println(response);

            if(response.startsWith("200")) {
                System.out.println("Registration successful");
                System.out.print(playerName + " has join game");
                System.out.println("");

                // Thread to read messages from the server
                Thread readThread = new Thread(() -> {
                    try {
                        while (!socket.isClosed()) {
                            String serverMessage = in.readUTF();
//                            if(serverMessage.equals("start")) {
//                                out.writeUTF("draw");
//                            }
                            System.out.println(serverMessage);
                        }
                    } catch (IOException e) {
                        System.out.println(e);
                    }
                });
                readThread.start();

                String line = "";

                // Main loop to send messages to the server
                while (!line.equals("Over")) {
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

    private void sendCommand(String command) throws IOException {
        out.writeUTF(command);
        out.flush();
    }

    public static void main(String args[]) {
        Client client = new Client(ADDRESS, 5000);
    }
}
