package pvz.screen;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.Align;
import model.LeaderboardEntry;
import pvz.PvzApplication;

import java.util.List;

public final class LeaderboardScreen extends AuthenticatedUiScreen {
    private static final float RANK_WIDTH = 72f;
    private static final float USERNAME_WIDTH = 210f;
    private static final float CHAPTER_WIDTH = 245f;
    private static final float MINI_WIDTH = 150f;
    private static final float DAILY_WIDTH = 130f;
    private static final float QUEST_WIDTH = 130f;
    private static final float SCORE_WIDTH = 155f;
    private static final float TABLE_WIDTH = 1120f;

    private String sortColumn = "score";
    private String sortOrder = "desc";

    public LeaderboardScreen(PvzApplication app) {
        super(app);
        buildUi();
    }

    private void buildUi() {
        Table screen = new Table();
        screen.top();
        screen.pad(38f, 42f, 20f, 42f);

        screen.add(titleBar("LEADERBOARD"))
            .width(1190f)
            .height(58f)
            .padBottom(12f);
        screen.row();

        screen.add(buildLeaderboardCard())
            .width(1160f)
            .height(535f)
            .center();
        screen.row();

        TextButton back = theme.secondaryButton("Back");
        UiActions.onClick(back, app::showMainMenu);
        screen.add(back)
            .width(170f)
            .height(48f)
            .right()
            .padTop(8f);

        root.add(screen).grow();
    }

    private Table buildLeaderboardCard() {
        Table card = theme.settingsCardPanel(12f);
        card.top();

        Label subtitle = theme.heading("PLAYER RANKINGS");
        subtitle.setAlignment(Align.center);
        card.add(subtitle)
            .width(TABLE_WIDTH)
            .height(38f)
            .padBottom(6f);
        card.row();

        card.add(buildHeader())
            .width(TABLE_WIDTH)
            .height(50f)
            .padBottom(7f);
        card.row();

        Table rows = new Table();
        rows.top();
        rows.defaults().padBottom(5f);

        List<LeaderboardEntry> entries = app.services().leaderboard()
            .getLeaderboard(sortColumn, sortOrder);
        int rank = 1;
        for (LeaderboardEntry entry : entries) {
            rows.add(buildRow(entry, rank))
                .width(TABLE_WIDTH)
                .height(58f);
            rows.row();
            rank++;
        }

        if (entries.isEmpty()) {
            Label empty = theme.settingsLabel("No leaderboard entries yet.");
            empty.setAlignment(Align.center);
            rows.add(empty).width(TABLE_WIDTH).height(70f);
            rows.row();
        }

        ScrollPane scroll = new ScrollPane(rows, theme.skin());
        scroll.setFadeScrollBars(false);
        scroll.setOverscroll(false, false);
        scroll.setScrollingDisabled(true, false);
        card.add(scroll).width(TABLE_WIDTH).growY();
        return card;
    }

    private Table buildHeader() {
        Table header = theme.settingsBadgePanel(5f);
        header.add(headerLabel("#")).width(RANK_WIDTH).height(40f);
        header.add(sortButton("User", "username")).width(USERNAME_WIDTH).height(40f);
        header.add(sortButton("Adventure", "chapter")).width(CHAPTER_WIDTH).height(40f);
        header.add(sortButton("Mini Games", "minigames")).width(MINI_WIDTH).height(40f);
        header.add(sortButton("Daily", "dailyquests")).width(DAILY_WIDTH).height(40f);
        header.add(sortButton("Quests", "otherquests")).width(QUEST_WIDTH).height(40f);
        header.add(sortButton("Best Score", "score")).width(SCORE_WIDTH).height(40f);
        return header;
    }

    private Label headerLabel(String text) {
        Label label = theme.heading(text);
        label.setAlignment(Align.center);
        label.setFontScale(0.82f);
        return label;
    }

    private TextButton sortButton(String text, String column) {
        String suffix = sortColumn.equals(column)
            ? ("desc".equals(sortOrder) ? " ▼" : " ▲")
            : "";
        TextButton button = theme.secondaryButton(text + suffix);
        button.getLabel().setFontScale(0.56f);
        UiActions.onClick(button, () -> changeSort(column));
        return button;
    }

    private Table buildRow(LeaderboardEntry entry, int rank) {
        boolean currentPlayer = entry.username().equals(user.getUsername());
        Table row = currentPlayer
            ? theme.settingsCardPanel(6f)
            : theme.settingsBadgePanel(6f);

        Label rankLabel = cell(Integer.toString(rank), Align.center);
        Label username = cell(entry.username() + (currentPlayer ? "  • YOU" : ""), Align.left);
        Label adventure = cell(adventureText(entry), Align.center);
        Label miniGames = cell(Integer.toString(entry.miniGames()), Align.center);
        Label daily = cell(Integer.toString(entry.dailyQuests()), Align.center);
        Label quests = cell(Integer.toString(entry.otherQuests()), Align.center);
        Label score = cell(Integer.toString(entry.bestScore()), Align.center);

        row.add(rankLabel).width(RANK_WIDTH);
        row.add(username).width(USERNAME_WIDTH).left();
        row.add(adventure).width(CHAPTER_WIDTH);
        row.add(miniGames).width(MINI_WIDTH);
        row.add(daily).width(DAILY_WIDTH);
        row.add(quests).width(QUEST_WIDTH);
        row.add(score).width(SCORE_WIDTH);
        return row;
    }

    private String adventureText(LeaderboardEntry entry) {
        return "Chapter " + entry.chapter() + "  •  Level " + entry.level();
    }

    private Label cell(String text, int alignment) {
        Label label = theme.settingsLabel(text);
        label.setAlignment(alignment);
        label.setEllipsis(true);
        label.setFontScale(0.72f);
        return label;
    }

    private void changeSort(String column) {
        if (sortColumn.equals(column)) {
            sortOrder = "desc".equals(sortOrder) ? "asc" : "desc";
        } else {
            sortColumn = column;
            sortOrder = "desc";
        }
        rebuild();
    }

    private void rebuild() {
        root.clearChildren();
        buildUi();
    }
}
