package fr.aumgn.dac2.game.colonnisation;

import fr.aumgn.dac2.game.GamePlayer;
import fr.aumgn.dac2.stage.StagePlayer;

public class ColonnPlayer extends GamePlayer
        implements Comparable<ColonnPlayer> {

    private int multiplier;
    private int score;

    public ColonnPlayer(StagePlayer player) {
        super(player);
        this.multiplier = 1;
        this.score = 0;
    }

    @Override
    public int compareTo(ColonnPlayer other) {
        return new Integer(score).compareTo(other.score);
    }

    public int getScore() {
        return score;
    }

    public int getMultiplier() {
        return multiplier;
    }

    public void addPoints(int value) {
        score += value;
    }

    public void incrementMultiplier() {
        multiplier++;
    }

    public void resetMultiplier() {
        multiplier = 1;
    }
}
