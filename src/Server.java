import java.net.*;
import java.io.*;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    private ServerSocket server = null;
//    private static String ADDRESS = "192.168.133.187";
    private static String ADDRESS = "0.0.0.0";
    private ConcurrentHashMap<String, Player> players = new ConcurrentHashMap<>();
    private final Blackjack blackjack = new Blackjack();
    private static int END = 0;

    public Server(int port) {
        try {
            server = new ServerSocket(port, 0, InetAddress.getByName(ADDRESS));
            System.out.println("Server started");

            while (true) {
                System.out.println("Waiting for a client ...");
                Socket socket = server.accept();
                System.out.println("Client accepted");
//                new ClientHandler(socket, players, blackjack).start();
                new NewClientHandler(socket, players, blackjack).start();
            }
        } catch (IOException i) {
            System.out.println(i);
        }
    }

    public synchronized static void addEND() {
        END++;
    }

    public synchronized static int getEND() {
        return END;
    }

    public synchronized static void resetEND() {
        END = 0;
    }

    public static void main(String args[]) {
        Server server = new Server(5000);
    }
}