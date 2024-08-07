import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

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
                        System.out.println(p.getName()+" : "+p.getHand().get(0));
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
                            else if(playerWin != null && p.getScore() > playerWin.getScore() && p.getScore() <= 21){
                                playerWin = p;
                            }
                        }
                        broadcastMessage("**--------------------**");
                        if(playerWin != null){
                            broadcastMessage("Winner is " + playerWin.getName());
                            broadcastMessage("Winner score is " + playerWin.getScore());
                        } else{
                            broadcastMessage("No winner!");
                        }
                        broadcastMessage("**--------------------**");
                        for (Player p : players.values()) {
                            broadcastMessage(p.getName() + " score is " + p.getScore());
                        }
                        broadcastMessage("**--------------------**");
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

    private void sendResponse(int statusCode, String statusPhrase) throws IOException {
        out.writeUTF(statusCode + " " + statusPhrase);
        out.flush();
    }

    private void handleRegister(String playerName) throws IOException {
        if (playerName == null || playerName.isEmpty()) {
            sendResponse(400, "BAD_REQUEST");
            return;
        }
        Player player = new Player(playerName, socket);
        players.put(playerName, player);
        blackjack.addPlayer(player);
        sendResponse(200, "REGISTERED " + playerName);
        System.out.println("Player " + playerName + " registered with IP: " + socket.getInetAddress());
    }

    private void handlePlay() throws IOException {
        for (Player player : players.values()) {
            Card card1 = blackjack.dealCard(player);
            Card card2 = blackjack.dealCard(player);
            DataOutputStream playerOut = new DataOutputStream(player.getSocket().getOutputStream());
            playerOut.writeUTF("200 INITIAL_CARDS " + card1.toString() + " " + card2.toString() + " " + player.getScore());
            playerOut.flush();
            System.out.println("Dealt cards to " + player.getName() + ": " + card1 + ", " + card2);
        }
    }

    private void handleHit() throws IOException {
        Player player = players.get(socket.getInetAddress().toString());
        if (player != null) {
            Card card = blackjack.dealCard(player);
            sendResponse(200, "CARD " + card.toString() + " " + player.getScore());
        } else {
            sendResponse(404, "NOT_FOUND");
        }
    }

    private void handlePass() throws IOException {
        Player player = players.get(socket.getInetAddress().toString());
        if (player != null) {
            Server.addEND();
            if (Server.getEND() >= players.size()) {
                broadcastMessage("200 GAME_OVER");
            }
        } else {
            sendResponse(404, "NOT_FOUND");
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