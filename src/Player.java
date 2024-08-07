import java.net.Socket;
import java.util.ArrayList;

public class Player {
    private String name;
    private int score;
    private ArrayList<Card> hand;
    private Socket socket;
    private boolean passed;

    public Player(String name, Socket socket) {
        this.name = name;
        this.score = 0;
        this.hand = new ArrayList<>();
        this.socket = socket;
        this.passed = false;
    }

    public String getName() {
        return name;
    }

    public int getScore() {
        return score;
    }

    public ArrayList<Card> getHand() {
        return hand;
    }

    public Socket getSocket() {
        return socket;
    }

    public void addCard(Card card) {
        hand.add(card);
        addScore(card.getScore());
    }

    public void removeCard(Card card) {
        hand.remove(card);
    }
    public void reset(){
        score = 0;
        hand.clear();
    }

    public void printHand() {
        System.out.println(hand);
    }

    public void printScore() {
        System.out.println(score);
    }

    public void addScore(int score) {
        this.score += score;
    }

    public boolean hasPassed() {
        return passed;
    }

    public void setPassed(boolean passed) {
        this.passed = passed;
    }
}
