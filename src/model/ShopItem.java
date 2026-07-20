package model;

public record ShopItem(int id, ShopItemType type, int coinCost, int gemCost,
                       int quantity, String description) {
    @Override
    public String toString() {
        String price = coinCost > 0 ? coinCost + " coins" : gemCost + " gems";
        return id + ". " + type + " - " + price + " - " + description;
    }
}
