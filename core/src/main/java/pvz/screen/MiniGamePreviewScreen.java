package pvz.screen;

import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.Align;
import model.MiniGameSession;
import model.MiniGameType;
import pvz.PvzApplication;

public final class MiniGamePreviewScreen extends AuthenticatedUiScreen {
    private static final String VASE_ICON =
        "IMAGE_EGGBREAKER_VASE_EGG_BROWN_VASE_EGG_BROWN_151X198";
    private static final String BOWLING_ICON =
        "IMAGE_PLANT_WALLNUT_WALLNUT_169X187";
    private static final String BRAIN_ICON =
        "IMAGE_UI_GAMEOVER_FAIL_SCREEN_BRAIN_ONLY";

    private final MiniGameType type;
    private final MiniGameSession session;
    private final Label status;

    public MiniGamePreviewScreen(PvzApplication app, MiniGameType type) {
        super(app);
        this.type = type;
        session = app.services().miniGames().getCurrentSession();
        if (session == null || session.getDefinition().type() != type) {
            throw new IllegalStateException("A mini-game preview requires a prepared level.");
        }
        status = theme.statusLabel();
        buildUi();
    }

    @Override
    protected void handleEscape() {
        app.returnToMiniGames();
    }

    private void buildUi() {
        Table screen = new Table();
        screen.top().pad(26f, 46f, 18f, 46f);
        screen.add(titleBar(displayName() + " - LEVEL " + session.getLevel()))
            .width(1188f).height(54f).padBottom(12f);
        screen.row();
        screen.add(buildPreviewCard()).width(920f).height(550f).center();
        screen.row().padTop(8f);
        status.setAlignment(Align.center);
        screen.add(status).width(900f).height(30f);
        addScrollable(screen);
    }

    private Table buildPreviewCard() {
        Table card = theme.settingsCardPanel(24f);
        card.top();
        card.defaults().left();

        Image icon = theme.image(iconId());
        if (icon != null) {
            card.add(icon).size(92f).padBottom(4f).center();
            card.row();
        }

        card.add(theme.title(displayName())).width(820f).height(42f).padBottom(3f).center();
        card.row();
        Label level = theme.heading("LEVEL " + session.getLevel() + " PREVIEW");
        card.add(level).width(820f).height(28f).padBottom(6f).center();
        card.row();

        addInfo(card, "OBJECTIVE", session.getDefinition().objective());
        addInfo(card, "TARGET", targetText());

        card.add(theme.fieldLabel("HOW TO PLAY")).left().padTop(12f);
        card.row().padTop(5f);
        Label rules = theme.bodyLabel(rulesText());
        rules.setAlignment(Align.left);
        rules.setWrap(true);
        rules.setFontScale(0.72f);
        card.add(rules).width(820f).height(86f).left();
        card.row().padTop(10f);

        TextButton play = theme.tertiaryButton("PLAY");
        UiActions.onClick(play, () -> app.playMiniGame(type));
        card.add(play).width(300f).height(50f).padBottom(5f);
        card.row();

        TextButton back = theme.secondaryButton("BACK TO MINI GAMES");
        UiActions.onClick(back, app::returnToMiniGames);
        card.add(back).width(300f).height(46f);
        return card;
    }

    private void addInfo(Table card, String name, String value) {
        Table row = new Table();
        row.left();
        row.add(theme.fieldLabel(name)).width(160f).left();
        Label label = theme.bodyLabel(value);
        label.setAlignment(Align.left);
        label.setWrap(true);
        label.setFontScale(0.72f);
        row.add(label).width(660f).height(42f).left();
        card.add(row).width(820f).height(46f).left();
        card.row().padTop(4f);
    }

    private String targetText() {
        return switch (type) {
            case VASEBREAKER -> session.getTarget() + " vases broken";
            case WALLNUT_BOWLING -> session.getTarget() + " zombies defeated";
            case I_ZOMBIE -> "Eat all 5 brains";
            case BEGHOULD -> session.getTarget() + " matches made";
            case ZOMBOTANY -> session.getTarget() + " zombies defeated";
        };
    }

    private String rulesText() {
        return switch (type) {
            case VASEBREAKER ->
                "Break a vase to reveal an empty tile, a zombie, or a plant packet. "
                    + "Use the revealed plants to clear the board. No falling sun.";
            case WALLNUT_BOWLING ->
                "Choose a Wall-nut from the conveyor and launch it down a row. Normal nuts turn "
                    + "after impact, while Explode-o-nuts clear a 3x3 area.";
            case I_ZOMBIE ->
                "Plants are already placed. Spend sun to deploy the available zombie cards and "
                    + "eat all five brains before the plants stop you.";
            case BEGHOULD ->
                "Swap adjacent plants only when the swap creates a line of three or more. "
                    + "Matched plants refill the board, grant sun, and help defend against zombies.";
            case ZOMBOTANY ->
                "Choose your plants, press Play, then plant and collect sun during the battle. "
                    + "Each enemy carries a Peashooter, Wall-nut, Jalapeno, or Squash power.";
        };
    }

    private String displayName() {
        return switch (type) {
            case VASEBREAKER -> "VASEBREAKER";
            case WALLNUT_BOWLING -> "WALL-NUT BOWLING";
            case I_ZOMBIE -> "I, ZOMBIE";
            case BEGHOULD -> "BEGHOULed";
            case ZOMBOTANY -> "ZOMBOTANY";
        };
    }

    private String iconId() {
        return switch (type) {
            case VASEBREAKER -> VASE_ICON;
            case WALLNUT_BOWLING -> BOWLING_ICON;
            case I_ZOMBIE -> BRAIN_ICON;
            case BEGHOULD -> "IMAGE_UI_PENNY_PURSUITS_LEVEL_ICONS_TYPE_ICON_MINIGAME";
            case ZOMBOTANY -> "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_MUMMY";
        };
    }

}
