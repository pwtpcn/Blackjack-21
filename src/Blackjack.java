import java.util.ArrayList;

public class Blackjack {
    private CardDeck deck;
    private ArrayList<Player> players;

    public Blackjack() {
        this.deck = new CardDeck();
        this.players = new ArrayList<>();
    }

    public void addPlayer(Player player) {
        this.players.add(player);
    }

    public void reset(){
        deck = new CardDeck();
        for (Player player : players) {
            player.reset();
        }
    }

    public Card dealCard(Player player) {
        Card card = deck.randomCard();
        player.addCard(card);
        return card;
    }

    public void play() {
        deck.shuffle();
        deck.shuffle();
        for (Player player : players) {
            player.addCard(deck.randomCard());
            player.addCard(deck.randomCard());
        }
        for (Player player : players) {
            System.out.println(player.getName());
            System.out.println("-------------------");
            player.printHand();
            player.printScore();
            System.out.println("-------------------");
        }
    }
}
