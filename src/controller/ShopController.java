package controller;

import model.PlantFactory;
import model.Shop;
import model.ShopItem;
import model.ShopItemType;
import model.User;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ShopController {
    private final AuthController authController;
    private final PlantFactory plantFactory;
    private final Shop shop = new Shop();
    private final Random random = new Random();

    public ShopController(AuthController authController, PlantFactory plantFactory) {
        this.authController = authController;
        this.plantFactory = plantFactory;
    }

    public List<String> listItems() {
        return shop.getPermanentItems().stream().map(ShopItem::toString).toList();
    }

    public String dailyOffer() {
        User user = authController.getCurrentUser();
        if (user == null) {
            return "Login is required.";
        }
        if (!user.getShopState().isCurrent(LocalDate.now())) {
            User snapshot = user.copyForRollback();
            refreshDaily(user);
            ActionResult saved = authController.saveCurrentState();
            if (!saved.isSuccessful()) {
                user.restoreFrom(snapshot);
                return "Could not save daily offer: " + saved.getMessage();
            }
        }
        return "6. DAILY_SEED_PACKET - 1600 coins - 10 seed packets for "
            + user.getShopState().getDailyPlant() + " - "
            + (user.getShopState().isDailyPurchased() ? "PURCHASED" : "AVAILABLE");
    }

    public ActionResult buyItem(int itemId, int count, String plantType) {
        User user = authController.getCurrentUser();
        if (user == null) {
            return ActionResult.failure("Login is required.");
        }
        if (count <= 0) {
            return ActionResult.failure("Count must be positive.");
        }
        User snapshot = user.copyForRollback();
        try {
            ActionResult result;
            if (itemId == 6) {
                result = buyDaily(user, count);
            } else {
                ShopItem item = shop.findItem(itemId);
                result = buyPermanent(user, item, count, plantType);
            }
            if (!result.isSuccessful()) {
                user.restoreFrom(snapshot);
            }
            return result;
        } catch (IllegalArgumentException exception) {
            user.restoreFrom(snapshot);
            return ActionResult.failure(exception.getMessage());
        }
    }

    private ActionResult buyPermanent(User user, ShopItem item, int count, String plantType) {
        if (item.type() == ShopItemType.POT
            && user.getGreenhouse().getUnlockedSlotCount() + count > model.Greenhouse.MAX_SLOTS) {
            return ActionResult.failure("Greenhouse already has the requested capacity.");
        }
        if (item.type() == ShopItemType.PLANT_FOOD
            && user.getInventory().getPlantFoodCapacityLeft() < count) {
            return ActionResult.failure("Plant food capacity is 3.");
        }
        if (item.type() == ShopItemType.SELECTED_SEED_PACKET) {
            String canonical = ownedPlant(user, plantType);
            if (canonical == null) {
                return ActionResult.failure("A valid owned plant is required with -t.");
            }
            if (!spend(user, item, count)) {
                return ActionResult.failure("Not enough currency.");
            }
            user.getInventory().addSeedPacket(canonical, item.quantity() * count);
        } else {
            if (!spend(user, item, count)) {
                return ActionResult.failure("Not enough currency.");
            }
            applyItem(user, item, count);
        }
        return save("Purchase completed: " + item.type() + " x" + count + ".");
    }

    private void applyItem(User user, ShopItem item, int count) {
        if (item.type() == ShopItemType.POT) {
            for (int index = 0; index < count; index++) {
                user.getGreenhouse().unlockNextSlot();
            }
        } else if (item.type() == ShopItemType.PLANT_FOOD) {
            user.getInventory().addPlantFood(count);
        } else if (item.type() == ShopItemType.RANDOM_SEED_PACKET) {
            for (int index = 0; index < count; index++) {
                String plant = randomOwnedPlant(user);
                if (plant != null) {
                    user.getInventory().addSeedPacket(plant, item.quantity());
                }
            }
        } else if (item.type() == ShopItemType.CURRENCY_EXCHANGE) {
            user.getWallet().addCoins(item.quantity() * count);
        }
    }

    private ActionResult buyDaily(User user, int count) {
        refreshDaily(user);
        if (count != 1) {
            return ActionResult.failure("Daily offer can only be bought once.");
        }
        if (user.getShopState().isDailyPurchased()) {
            return ActionResult.failure("Daily offer was already purchased.");
        }
        if (!user.getWallet().spendCoins(1600)) {
            return ActionResult.failure("Not enough coins.");
        }
        user.getInventory().addSeedPacket(user.getShopState().getDailyPlant(), 10);
        user.getShopState().markDailyPurchased();
        return save("Daily offer purchased.");
    }

    private boolean spend(User user, ShopItem item, int count) {
        int coins = item.coinCost() * count;
        int gems = item.gemCost() * count;
        if (coins > 0) {
            return user.getWallet().spendCoins(coins);
        }
        return user.getWallet().spendGems(gems);
    }

    private void refreshDaily(User user) {
        LocalDate today = LocalDate.now();
        if (!user.getShopState().isCurrent(today)) {
            user.getShopState().setDailyOffer(today, randomOwnedPlant(user));
        }
    }

    private String randomOwnedPlant(User user) {
        List<String> owned = List.copyOf(user.getCollectionBook().getOwnedPlants());
        return owned.isEmpty() ? "Peashooter" : owned.get(random.nextInt(owned.size()));
    }

    private String ownedPlant(User user, String plantType) {
        if (plantType == null) {
            return null;
        }
        return plantFactory.findDefinition(plantType)
            .map(definition -> definition.getName())
            .filter(user.getCollectionBook().getOwnedPlants()::contains)
            .orElse(null);
    }

    private ActionResult save(String message) {
        ActionResult save = authController.saveCurrentState();
        return save.isSuccessful() ? ActionResult.success(message) : save;
    }
}
