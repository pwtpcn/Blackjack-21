import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CardDeck {
    private List<Card> deck;

    public CardDeck() {
        deck = new ArrayList<>();
        createDeck();
    }

    private void createDeck() {
        String[] suits = {"Hearts", "Diamonds", "Clubs", "Spades"};
        String[] ranks = {"Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten", "Jack", "Queen", "King", "Ace"};
        int[] scores = {2, 3, 4, 5, 6, 7, 8, 9, 10, 10, 10, 10, 11}; // Assuming Ace is worth 11 points

        for (String suit : suits) {
            for (int i = 0; i < ranks.length; i++) {
                deck.add(new Card(scores[i], ranks[i] + " of " + suit));
            }
        }
    }

    public List<Card> getDeck() {
        return deck;
    }

    public Card randomCard(){
        Random rand = new Random();
        Card card = deck.get(rand.nextInt(deck.size()));
        deck.remove(card);
        return card;
    }

    public void shuffle() {
        Random rand = new Random();
        for (int i = 0; i < deck.size(); i++) {
            int randomIndex = rand.nextInt(deck.size());
            Card temp = deck.get(i);
            deck.set(i, deck.get(randomIndex));
            deck.set(randomIndex, temp);
        }
    }

    public void printDeck() {
        for (Card card : getDeck()) {
            System.out.println(card);
        }
    }
}