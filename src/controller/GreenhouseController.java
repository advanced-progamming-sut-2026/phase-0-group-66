package controller;

import model.Greenhouse;
import model.GreenhouseSlot;
import model.PlantDefinition;
import model.PlantFactory;
import model.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GreenhouseController {
    private static final int MARIGOLD_REWARD = 500;

    private final AuthController authController;
    private final PlantFactory plantFactory;
    private final Random random = new Random();

    public GreenhouseController(AuthController authController, PlantFactory plantFactory) {
        this.authController = authController;
        this.plantFactory = plantFactory;
    }

    public String showGreenhouse() {
        User user = requireUser();
        return user == null ? "Login is required."
            : user.getGreenhouse().render(System.currentTimeMillis());
    }

    public ActionResult plantPot(int x, int y) {
        User user = requireUser();
        if (user == null) {
            return ActionResult.failure("Login is required.");
        }
        User snapshot = user.copyForRollback();
        try {
            GreenhouseSlot slot = user.getGreenhouse().getSlot(x, y);
            if (!slot.isUnlocked()) {
                return ActionResult.failure("Greenhouse slot is locked.");
            }
            if (!slot.isEmpty()) {
                return ActionResult.failure("Greenhouse slot is occupied.");
            }
            if (!user.getInventory().consumePot()) {
                return ActionResult.failure("No planting pot is available in inventory.");
            }
            boolean marigold = random.nextBoolean();
            String plantName = marigold ? "Marigold" : randomBoostablePlant(user);
            if (plantName == null) {
                plantName = "Marigold";
                marigold = true;
            }
            long growth = marigold ? Greenhouse.MARIGOLD_GROWTH_MILLIS
                : Greenhouse.PLANT_GROWTH_MILLIS;
            slot.plant(plantName, marigold, System.currentTimeMillis(), growth);
            return save(user, snapshot, "Planted " + plantName + " at (" + x + ", " + y
                + "); one inventory pot was consumed. Remaining pots: "
                + user.getInventory().getPots() + ".");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            user.restoreFrom(snapshot);
            return ActionResult.failure(exception.getMessage());
        }
    }

    public ActionResult collect(int x, int y) {
        User user = requireUser();
        if (user == null) {
            return ActionResult.failure("Login is required.");
        }
        User snapshot = user.copyForRollback();
        try {
            GreenhouseSlot slot = user.getGreenhouse().getSlot(x, y);
            if (slot.isEmpty()) {
                return ActionResult.failure("Greenhouse slot is empty.");
            }
            if (!slot.isReady(System.currentTimeMillis())) {
                return ActionResult.failure("Plant is not ready.");
            }
            String result;
            if (slot.isMarigold()) {
                user.getWallet().addCoins(MARIGOLD_REWARD);
                result = "Collected Marigold and received 500 coins.";
            } else {
                boolean added = user.getInventory().addStoredBoost(slot.getPlantName());
                result = added ? "Stored one boost for " + slot.getPlantName() + "."
                    : "Boost already exists; the slot was cleared without another boost.";
            }
            slot.clear();
            return save(user, snapshot, result);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            user.restoreFrom(snapshot);
            return ActionResult.failure(exception.getMessage());
        }
    }

    public ActionResult grow(int x, int y) {
        User user = requireUser();
        if (user == null) {
            return ActionResult.failure("Login is required.");
        }
        User snapshot = user.copyForRollback();
        try {
            GreenhouseSlot slot = user.getGreenhouse().getSlot(x, y);
            long now = System.currentTimeMillis();
            if (slot.isEmpty()) {
                return ActionResult.failure("Greenhouse slot is empty.");
            }
            if (slot.isReady(now)) {
                return ActionResult.failure("Plant is already ready.");
            }
            int cost = (int) Math.ceil(slot.remainingMillis(now) / 3_600_000.0);
            if (!user.getWallet().spendGems(cost)) {
                return ActionResult.failure("Not enough gems. Required: " + cost + ".");
            }
            slot.makeReady(now);
            return save(user, snapshot, "Growth completed for " + cost + " gem(s).");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            user.restoreFrom(snapshot);
            return ActionResult.failure(exception.getMessage());
        }
    }

    private String randomBoostablePlant(User user) {
        ArrayList<String> choices = new ArrayList<>();
        for (String name : user.getCollectionBook().getOwnedPlants()) {
            PlantDefinition definition = plantFactory.findDefinition(name).orElse(null);
            if (definition != null && !definition.getPlantFoodEffect().isBlank()) {
                choices.add(definition.getName());
            }
        }
        return choices.isEmpty() ? null : choices.get(random.nextInt(choices.size()));
    }

    private User requireUser() {
        return authController.getCurrentUser();
    }

    private ActionResult save(User user, User snapshot, String message) {
        ActionResult save = authController.saveCurrentState();
        if (!save.isSuccessful()) {
            user.restoreFrom(snapshot);
            return save;
        }
        return ActionResult.success(message);
    }
}
