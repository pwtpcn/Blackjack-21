public class Card {
    private int score;
    private String cardName;

    public Card(int score, String cardName) {
        this.score = score;
        this.cardName = cardName;
    }

    public int getScore() {
        return score;
    }

    public String getCardName() {
        return cardName;
    }

    @Override
    public String toString() {
        return cardName;
    }
}
