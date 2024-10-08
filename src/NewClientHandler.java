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
    private String playerName;

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
                String arg = tokens.length > 1 ? tokens[1] : "";

                switch (command) {
                    case "REGISTER":
                        handleRegister(arg);
                        break;
                    case "START":
                        startBroadcast();
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
                    case "EXIT":
                        handleExit();
                        break;
                    case "SEE":
                        handlerSee();
                        break;
                    default:
                        sendResponse(400, "Bad request - Wrong command (START, HIT, PASS, SEE, EXIT)");
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
        while (playerName.trim().isEmpty()){
            sendResponse(400, "Bad request - Player name cannot be empty.");
            playerName = in.readUTF().split(" ", 2)[1];
        }
        this.playerName = playerName;
        Player player = new Player(playerName, socket);
        players.put(playerName, player);
        blackjack.addPlayer(player);

        sendResponse(200, "OK - Registration successful. Welcome  " + playerName + "!");
        System.out.println("Player " + playerName + " registered with IP: " + socket.getInetAddress());
    }

    private void startBroadcast() throws IOException {
        System.out.println(playerName + " use START command");

        if(!blackjack.isGameStarted()){
            blackjack.startGame();
            System.out.println("Game start");
            broadcastMessage("START");
            broadcastMessage("**--------START--------**");
            sendResponse(200, "OK - Game start.");
        } else {
            sendResponse(400, "Bad request - Game is already started.");
        }

    }

    private void handlePlay() throws IOException {
        if(blackjack.isGameStarted()){
            Player player = players.get(playerName);
            Card card1 = blackjack.dealCard(player);
            Card card2 = blackjack.dealCard(player);

            DataOutputStream playerOut = new DataOutputStream(player.getSocket().getOutputStream());
            playerOut.writeUTF("Card: " + card1.toString());
            playerOut.writeUTF("Card: " + card2.toString());
            playerOut.writeUTF("Overall Score: " + player.getScore());
            playerOut.flush();

            System.out.println("Dealt cards to " + player.getName() + ": " + card1 + ", " + card2);

            sendResponse(200, "OK - Dealt cards to each player.");
        } else {
            sendResponse(400, "Bad request - Game is not started yet.");
        }
    }

    private void handleHit() throws IOException {
        if(blackjack.isGameStarted()){
            System.out.println(playerName + " use HIT command");
            Player player = players.get(playerName);
            if (player != null) {
                Card card = blackjack.dealCard(player);
                out.writeUTF("You hit a " + card.toString());
                out.writeUTF("Your overall Score: " + player.getScore());
                broadcastMessage(player.getName() + " hit a " + card.toString());
                out.flush();
                sendResponse(200, "OK - You hit " + card.toString());
            } else {
                sendResponse(404, "Not found - Player not found.");
            }
        } else {
            sendResponse(400, "Bad request - Game has not started yet.");
        }
    }

    private void handlerSee() throws IOException {
        if(blackjack.isGameStarted()){
            System.out.println(playerName + " use SEE command");
            Player player = players.get(playerName);
            if(player != null) {
                for (Player p : players.values()) {
                    out.writeUTF(p.getName()+" : "+p.getHand().get(0));
                }
                sendResponse(200, "OK - Card has show");
            }
            else{
                sendResponse(404, "Not found - Player not found.");
            }
        } else {
            sendResponse(400, "Bad request - Game has not started yet.");
        }
    }

    private void handlePass() throws IOException {
        if(blackjack.isGameStarted()){
            System.out.println(playerName + " use PASS command");
            Player player = players.get(playerName);
            if (player != null) {
                if(!player.hasPassed()){
                    sendResponse(200, "OK - " + player.getName() + " PASS");
                    player.setPassed(true);
                    Server.addEND();
                }
                handleGameOver();
            } else {
                sendResponse(404, "Not found - Player not found.");
            }
        } else {
            sendResponse(400, "Bad request - Game has not started yet");
        }
    }

    private void handleGameOver() {
        if (Server.getEND() == players.size()) {
            Server.resetEND();
            broadcastMessage("**---------------------**");
            broadcastMessage("        GAME OVER        ");
            findWinner();
            showScoreBoard();
            blackjack.reset();
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
        broadcastMessage("      *Score Board*      ");
        for (Player p : players.values()) {
            broadcastMessage(p.getName() + " score: " + p.getScore());
        }
        broadcastMessage("-------------------------");
    }

    private void handleExit() {
        players.remove(playerName);
        System.out.println("Player " + playerName + " has left the game.");
        System.out.println("Player in game :");
        for (Player p : players.values()) {
            System.out.println(p.getName());
        }
        System.out.println("Received OVER command, closing connection from player " + playerName + ".");
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

    private void broadcastCommand(String command) {
        for (Player player : players.values()) {
            try {
                DataOutputStream playerOut = new DataOutputStream(player.getSocket().getOutputStream());
                playerOut.writeUTF(command);
                playerOut.flush();
            } catch (IOException e) {
                System.out.println("Error broadcasting command to player: " + player.getName());
            }
        }
    }
}