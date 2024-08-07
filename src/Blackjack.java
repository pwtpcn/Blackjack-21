import java.util.ArrayList;

public class Blackjack {
    private CardDeck deck;
    private ArrayList<Player> players;
    private boolean gameStarted;

    public Blackjack() {
        this.deck = new CardDeck();
        this.players = new ArrayList<>();
        this.gameStarted = false;
    }

    public void addPlayer(Player player) {
        this.players.add(player);
    }

    public void reset(){
        deck = new CardDeck();
        gameStarted = false;
        for (Player player : players) {
            player.reset();
        }
    }

    public Card dealCard(Player player) {
        Card card = deck.randomCard();
        player.addCard(card);
        return card;
    }

    public void startGame() {
        this.gameStarted = true;
        deck.shuffle();
    }

    public boolean isGameStarted() {
        return gameStarted;
    }

    public void playersList(){
        for (Player player : players) {
            System.out.println(player.getName());
        }
    }

    public CardDeck getDeck() {
        return deck;
    }

    //    public void play() {
//        deck.shuffle();
//        deck.shuffle();
//        for (Player player : players) {
//            player.addCard(deck.randomCard());
//            player.addCard(deck.randomCard());
//        }
//        for (Player player : players) {
//            System.out.println(player.getName());
//            System.out.println("-------------------");
//            player.printHand();
//            player.printScore();
//            System.out.println("-------------------");
//        }
//    }
}
