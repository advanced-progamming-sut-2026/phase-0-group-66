import java.util.regex.Matcher;

public class ShopMenu extends Menu {
    public ShopMenu(MenuManager menuManager) {
        super("Shop Menu", menuManager);
    }

    @Override
    public void showCommands() { }

    @Override
    protected void processSpecificCommand(String command) {
        Matcher buyMatcher = getMatcher(command, "shop buy -i (?<itemId>\\d+) -n (?<count>\\d+)( -t (?<plantType>\\S+))?");

        if (command.equals("shop list")) {
        } else if (command.equals("shop daily")) {
        } else if (buyMatcher != null) {
            String itemId = buyMatcher.group("itemId");
            int count = Integer.parseInt(buyMatcher.group("count"));
            String plantType = buyMatcher.group("plantType");

        } else {
            System.out.println("invalid command");
        }
    }
}