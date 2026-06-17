import java.util.regex.Matcher;

public class CollectionMenu extends Menu {
    public CollectionMenu(MenuManager menuManager) {
        super("Collection Menu", menuManager);
    }

    @Override
    public void showCommands() { }

    @Override
    protected void processSpecificCommand(String command) {
        Matcher showPlantMatcher = getMatcher(command, "menu collection show-plant -p (?<plantName>\\S+)");
        Matcher showZombieMatcher = getMatcher(command, "menu collection show-zombie -z (?<zombieName>\\S+)");
        Matcher upgradeMatcher = getMatcher(command, "menu collection upgrade-plant -p (?<plantName>\\S+)");
        Matcher purchaseMatcher = getMatcher(command, "menu collection purchase-plant -p (?<plantName>\\S+)");

        if (command.equals("menu collection show-plants")) {
        } else if (command.equals("menu collection show-all-plants")) {
        } else if (command.equals("menu collection show-zombies")) {
        } else if (command.equals("menu collection show-all-zombies")) {
        } else if (showPlantMatcher != null) {
            String plantName = showPlantMatcher.group("plantName");
        } else if (showZombieMatcher != null) {
            String zombieName = showZombieMatcher.group("zombieName");
        } else if (upgradeMatcher != null) {
            String plantName = upgradeMatcher.group("plantName");
        } else if (purchaseMatcher != null) {
            String plantName = purchaseMatcher.group("plantName");
        } else {
            System.out.println("invalid command");
        }
    }
}