package model;

import java.util.List;

public class Shop {
    private final List<ShopItem> permanentItems = List.of(
        new ShopItem(1, ShopItemType.POT, 2000, 0, 1, "Unlock one greenhouse slot."),
        new ShopItem(2, ShopItemType.PLANT_FOOD, 0, 3, 1, "Add one plant food, maximum 3."),
        new ShopItem(3, ShopItemType.RANDOM_SEED_PACKET, 1000, 0, 5,
            "Add 5 seed packets for a random owned plant."),
        new ShopItem(4, ShopItemType.SELECTED_SEED_PACKET, 0, 5, 10,
            "Add 10 seed packets for a selected owned plant."),
        new ShopItem(5, ShopItemType.CURRENCY_EXCHANGE, 0, 5, 500,
            "Exchange 5 gems for 500 coins.")
    );

    public List<ShopItem> getPermanentItems() { return permanentItems; }

    public ShopItem findItem(int id) {
        return permanentItems.stream().filter(item -> item.id() == id).findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Shop item does not exist."));
    }
}
