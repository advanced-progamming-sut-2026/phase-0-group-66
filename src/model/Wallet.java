package model;

import java.io.Serializable;

public class Wallet implements Serializable {
    private static final long serialVersionUID = 1L;

    private int coins;
    private int gems;

    public Wallet() {
        this(0, 0);
    }

    public Wallet(int coins, int gems) {
        this.coins = Math.max(0, coins);
        this.gems = Math.max(0, gems);
    }

    public int getCoins() {
        return coins;
    }

    public int getGems() {
        return gems;
    }

    public void addCoins(int amount) {
        requireNonNegative(amount);
        coins += amount;
    }

    public boolean spendCoins(int amount) {
        requireNonNegative(amount);
        if (coins < amount) {
            return false;
        }
        coins -= amount;
        return true;
    }

    public void addGems(int amount) {
        requireNonNegative(amount);
        gems += amount;
    }

    public boolean spendGems(int amount) {
        requireNonNegative(amount);
        if (gems < amount) {
            return false;
        }
        gems -= amount;
        return true;
    }

    private void requireNonNegative(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Amount cannot be negative.");
        }
    }
}
