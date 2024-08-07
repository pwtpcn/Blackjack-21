import java.net.*;
import java.io.*;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    private ServerSocket server = null;
    private static String ADDRESS = "192.168.133.187";
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
                new ClientHandler(socket, players, blackjack).start();
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

    public static void main(String args[]) {
        Server server = new Server(5000);
    }
}

class ClientHandler extends Thread {
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private ConcurrentHashMap<String, Player> players;
    private Blackjack blackjack;

    public ClientHandler(Socket socket, ConcurrentHashMap<String, Player> players, Blackjack blackjack) {
        this.socket = socket;
        this.players = players;
        this.blackjack = blackjack;
        try {
            in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            out = new DataOutputStream(socket.getOutputStream());
        } catch (IOException i) {
            System.out.println(i);
        }
    }

    public void run() {
        String line = "";
        try {
            // Read player's name
            String playerName = in.readUTF();
            Player player = new Player(playerName, socket);
            players.put(playerName, player);
            blackjack.addPlayer(player);
            System.out.println("Player " + playerName + " registered with IP: " + socket.getInetAddress());

            // Continue reading messages from client until "Over" is received
            while (!line.equals("Over")) {
                line = in.readUTF();
                System.out.println("Received from " + playerName + ": " + line);

                if(line.equals("start")) {
                    broadcastMessage("start");
                }
                if(line.equals("draw")) {
                    Card card1 = blackjack.dealCard(player);
                    Card card2 = blackjack.dealCard(player);
                    out.writeUTF("Card: " + card1.toString());
                    out.writeUTF("Card: " + card2.toString());
                    out.writeUTF("Overall Score: " + player.getScore());
                    out.flush();
                    System.out.println("Dealt card to " + playerName + ": " + card1);
                    System.out.println("Dealt card to " + playerName + ": " + card2);
                }
                if (line.equals("see")) {
                    for (Player p : players.values()) {
                        broadcastMessage(p.getName()+" : "+p.getHand().get(0));
                    }
                }
                if (line.equals("hit")) {
                    Card card = blackjack.dealCard(player);
                    out.writeUTF("Card: " + card.toString());
                    out.writeUTF("Overall Score: " + player.getScore());
                    broadcastMessage(playerName +" hit a " + card.toString());
                    out.flush();
                }
                if (line.equals("pass")) {
                    Server.addEND();
                    broadcastMessage(playerName +" has passed!");
                    if (Server.getEND() >= players.size()) {
                        Player playerWin = null;
                        for (Player p : players.values()) {
                            if(playerWin == null && p.getScore() <= 21){
                                playerWin = p;
                            }
                            else if(p.getScore() > playerWin.getScore() && p.getScore() <= 21){
                                playerWin = p;
                            }
                        }
                        broadcastMessage("Winner is " + playerWin.getName());
                        broadcastMessage("Winner is " + playerWin.getScore());
                        broadcastMessage("Game over");
                    }
                }
                if (line.equals("reset")) {
                    blackjack.reset();
                }
            }
        } catch (IOException i) {
            System.out.println(i);
        } finally {
            try {
                System.out.println("Closing connection for player");
                socket.close();
                in.close();
                out.close();
            } catch (IOException i) {
                System.out.println(i);
            }
        }
    }

    private void broadcastMessage(String message) {
        for (Player player : players.values()) {
            try {
                DataOutputStream playerOut = new DataOutputStream(player.getSocket().getOutputStream());
                playerOut.writeUTF(message);
                playerOut.flush();
            } catch (IOException e) {
                System.out.println("Error broadcasting message to player: " + player.getName());
            }
        }
    }
}
