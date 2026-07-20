package menu;

import controller.ActionResult;
import controller.ShopController;
import view.ShopView;

import java.util.regex.Matcher;

public class ShopMenu extends Menu {
    private final ShopController controller;
    private final ShopView view;

    public ShopMenu(MenuManager menuManager, ShopController controller, ShopView view) {
        super("Shop Menu", menuManager);
        this.controller = controller;
        this.view = view;
    }

    @Override
    public void showCommands() {
        System.out.println("shop list");
        System.out.println("shop daily");
        System.out.println("shop buy -i <item_id> -n <count> [-t <plant_type>]");
    }

    @Override
    protected void processSpecificCommand(String command) {
        Matcher buy = getMatcher(command,
            "shop buy -i (?<itemId>\\d+) -n (?<count>\\d+)(?: -t (?<plantType>.+))?");
        if (command.equals("shop list")) {
            view.showItems(controller.listItems());
        } else if (command.equals("shop daily")) {
            view.showMessage(controller.dailyOffer());
        } else if (buy != null) {
            String plant = buy.group("plantType");
            ActionResult result = controller.buyItem(Integer.parseInt(buy.group("itemId")),
                Integer.parseInt(buy.group("count")), plant == null ? null : plant.trim());
            view.showMessage(result.getMessage());
        } else if (command.equals("show commands")) {
            showCommands();
        } else {
            view.showMessage("invalid command");
        }
    }
}
