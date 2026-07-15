import java.util.regex.Matcher;

public class QuestMenu extends Menu {
    public QuestMenu(MenuManager menuManager) {
        super("Quest Menu", menuManager);
    }

    @Override
    public void showCommands() { }

    @Override
    protected void processSpecificCommand(String command) {
        Matcher pageMatcher = getMatcher(command, "travel log page (?<pageName>\\S+)");

        if (pageMatcher != null) {
            String pageName = pageMatcher.group("pageName");
        } else {
            System.out.println("invalid command");
        }
    }
}