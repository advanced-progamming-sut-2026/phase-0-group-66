package model;

import java.io.Serializable;

public class Wallet implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final int MAX_BALANCE = 999_999_999;

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
        coins = safeAdd(coins, amount);
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
        gems = safeAdd(gems, amount);
    }

    public boolean spendGems(int amount) {
        requireNonNegative(amount);
        if (gems < amount) {
            return false;
        }
        gems -= amount;
        return true;
    }

    private int safeAdd(int current, int amount) {
        requireNonNegative(amount);
        if (amount > MAX_BALANCE - current) {
            throw new IllegalArgumentException("Wallet balance cannot exceed " + MAX_BALANCE + ".");
        }
        return current + amount;
    }

    private void requireNonNegative(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Amount cannot be negative.");
        }
    }
}
