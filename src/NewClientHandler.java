import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

class NewClientHandler extends Thread {
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private ConcurrentHashMap<String, Player> players;
    private Blackjack blackjack;

    public NewClientHandler(Socket socket, ConcurrentHashMap<String, Player> players, Blackjack blackjack) {
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
            while (!line.equals("OVER")) {
                line = in.readUTF();
                String[] tokens = line.split(" ", 2);
                String command = tokens[0];
                String argument = tokens.length > 1 ? tokens[1] : "";

                switch (command) {
                    case "REGISTER":
                        handleRegister(argument);
                        break;
                    case "PLAY":
                        handlePlay();
                        break;
                    case "HIT":
                        handleHit();
                        break;
                    case "PASS":
                        handlePass();
                        break;
                    case "OVER":
                        handleOver();
                        break;
                    case "SEE":
                        handlerSee();
                    case "RESET":
                        handleReset();
                    default:
                        sendResponse(400, "BAD_REQUEST");
                        break;
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
        broadcastMessage("START");
        for (Player player : players.values()) {
            Card card1 = blackjack.dealCard(player);
            Card card2 = blackjack.dealCard(player);
            out.writeUTF("Card: " + card1.toString());
            out.writeUTF("Card: " + card2.toString());
            out.writeUTF("Overall Score: " + player.getScore());
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
            out.writeUTF("Card: " + card.toString());
            out.writeUTF("Overall Score: " + player.getScore());
            broadcastMessage(player.getName() + " hit a " + card.toString());
            out.flush();
            sendResponse(200, "CARD " + card.toString() + " " + player.getScore());
        } else {
            sendResponse(404, "NOT_FOUND");
        }
    }

    private void handlePass() throws IOException {
        Player player = players.get(socket.getInetAddress().toString());
        if (player != null) {
            if(!player.hasPassed()){
                sendResponse(200, player.getName() + " PASS");
                player.setPassed(true);
                Server.addEND();
            }
            handleGameOver();
        } else {
            sendResponse(404, "NOT_FOUND");
        }
    }

    private void handleGameOver() {
        if (Server.getEND() == players.size()) {
            Server.resetEND();
            broadcastMessage("**---------------------**");
            broadcastMessage("        GAME OVER        ");
            findWinner();
            showScoreBoard();
        }
    }

    private void findWinner(){
        Player playerWin = null;
        for (Player p : players.values()) {
            if(playerWin == null && p.getScore() <= 21){
                playerWin = p;
            }
            else if(playerWin != null && p.getScore() > playerWin.getScore() && p.getScore() <= 21){
                playerWin = p;
            }
        }
        if(playerWin != null){
            broadcastMessage("**---------------------**");
            broadcastMessage(playerWin.getName() + " WON with " + playerWin.getScore() + " points");
        } else{
            broadcastMessage("No winner!");
        }
    }

    private void showScoreBoard(){
        broadcastMessage("**---------------------**");
        for (Player p : players.values()) {
            broadcastMessage(p.getName() + " score is " + p.getScore());
        }
    }

    private void handleOver() {
        System.out.println("Received OVER command, closing connection.");
    }

    private void handlerSee() throws IOException {
        Player player = players.get(socket.getInetAddress().toString());
        if(player != null) {
            for (Player p : players.values()) {
                System.out.println(p.getName()+" : "+p.getHand().get(0));
            }
            sendResponse(200, "Card has show");
        }
        else{
            sendResponse(404, "NOT_FOUND");
        }
    }

    private void handleReset() {
        blackjack.reset();
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