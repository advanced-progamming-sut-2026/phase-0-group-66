package pvz.screen;

import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import controller.ActionResult;
import pvz.PvzApplication;
import pvz.ui.UiTheme;

public final class CheatScreen extends AuthenticatedUiScreen {
    private static final String[] CHAPTERS = {
        "Ancient Egypt",
        "Frostbite Caves",
        "Big Wave Beach",
        "Dark Ages"
    };

    private final Label coinValue;
    private final Label gemValue;
    private final Label status;
    private final TextField amountField;
    private final SelectBox<String> chapterBox;
    private final SelectBox<String> levelBox;

    public CheatScreen(PvzApplication app) {
        super(app);
        coinValue = theme.settingsLabel("");
        gemValue = theme.settingsLabel("");
        status = statusLabel();
        status.setWrap(false);

        amountField = textField("Amount");
        amountField.setTextFieldFilter(new TextField.TextFieldFilter.DigitsOnlyFilter());

        chapterBox = new SelectBox<>(theme.skin());
        chapterBox.setItems(CHAPTERS);
        chapterBox.setSelected(CHAPTERS[0]);

        levelBox = new SelectBox<>(theme.skin());
        levelBox.setItems("Level 1", "Level 2", "Level 3", "Level 4");
        levelBox.setSelectedIndex(0);

        buildUi();
        refreshWallet();
    }

    private void buildUi() {
        Table screen = new Table();
        screen.top();
        screen.pad(28f, 70f, 14f, 70f);

        Table panel = theme.dialogPanel();
        panel.top();
        panel.add(theme.settingsTitle("Wallet & Cheat Codes"))
            .width(900f)
            .height(58f)
            .center()
            .padBottom(10f);
        panel.row();
        panel.add(buildWalletCard()).width(900f).height(170f).padBottom(12f);
        panel.row();
        panel.add(buildUnlockCard()).width(900f).height(300f).padBottom(8f);
        panel.row();
        panel.add(status).width(820f).height(30f).padTop(2f);
        panel.row();

        TextButton back = theme.secondaryButton("Back to Settings");
        UiActions.onClick(back, app::showSettings);
        panel.add(back).width(240f).height(50f).padTop(6f);

        screen.add(panel).width(1040f).height(620f);
        root.add(screen).grow();
    }

    private Table buildWalletCard() {
        Table card = theme.settingsCardPanel(14f);
        card.top();
        card.add(theme.settingsLabel("WALLET CHEATS"))
            .left()
            .colspan(4)
            .padBottom(8f);
        card.row();

        Table balances = new Table();
        balances.add(walletBadge(UiTheme.COIN_ICON, "Coins", coinValue))
            .width(390f)
            .height(54f)
            .padRight(12f);
        balances.add(walletBadge(UiTheme.GEM_ICON, "Gems", gemValue))
            .width(390f)
            .height(54f);
        card.add(balances).colspan(4).center().padBottom(10f);
        card.row();

        card.add(theme.settingsLabel("Amount")).width(100f).left().padRight(8f);
        card.add(amountField).width(210f).height(46f).padRight(14f);

        TextButton addCoins = theme.primaryButton("Add Coins");
        TextButton addGems = theme.tertiaryButton("Add Gems");
        UiActions.onClick(addCoins, () -> addCurrency("coin"));
        UiActions.onClick(addGems, () -> addCurrency("gem"));
        card.add(addCoins).width(190f).height(48f).padRight(10f);
        card.add(addGems).width(190f).height(48f);
        return card;
    }

    private Table walletBadge(String iconId, String title, Label value) {
        Table badge = theme.settingsBadgePanel(8f);
        Image icon = theme.image(iconId);
        if (icon != null) {
            badge.add(icon).size(34f).padRight(7f);
        }
        badge.add(theme.settingsLabel(title)).padRight(12f);
        badge.add(value).expandX().right();
        return badge;
    }

    private Table buildUnlockCard() {
        Table card = theme.settingsCardPanel(14f);
        card.top();

        card.add(theme.settingsLabel("LEVEL UNLOCK CHEATS"))
            .left()
            .colspan(4)
            .padBottom(5f);
        card.row();

        Label help = theme.settingsLabel(
            "Choose any chapter and level, or unlock every level with one click."
        );
        help.setWrap(false);
        card.add(help).left().colspan(4).width(820f).padBottom(14f);
        card.row();

        card.add(theme.settingsLabel("Chapter")).width(100f).left().padRight(8f);
        card.add(chapterBox).width(280f).height(46f).padRight(24f);
        card.add(theme.settingsLabel("Level")).width(80f).left().padRight(8f);
        card.add(levelBox).width(190f).height(46f);
        card.row().padTop(14f);

        TextButton unlockSelected = theme.primaryButton("Unlock Selected Level");
        UiActions.onClick(unlockSelected, this::unlockSelectedLevel);
        card.add(unlockSelected)
            .width(320f)
            .height(52f)
            .colspan(4)
            .center()
            .padBottom(12f);
        card.row();

        TextButton unlockAll = theme.tertiaryButton("UNLOCK ALL LEVELS");
        unlockAll.getLabel().setFontScale(0.88f);
        UiActions.onClick(unlockAll, this::unlockAll);
        card.add(unlockAll)
            .width(430f)
            .height(58f)
            .colspan(4)
            .center();
        return card;
    }

    private void addCurrency(String currency) {
        int amount = parseAmount();
        if (amount <= 0) {
            theme.showError(status, "Enter an amount greater than zero.");
            return;
        }
        ActionResult result = app.services().game().addWalletCurrency(amount, currency);
        handleResult(result);
        if (result.isSuccessful()) {
            refreshWallet();
            amountField.setText("");
        }
    }

    private int parseAmount() {
        String text = amountField.getText().trim();
        if (text.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private void unlockSelectedLevel() {
        int level = levelBox.getSelectedIndex() + 1;
        ActionResult result = app.services().game().unlockLevelCheat(chapterBox.getSelected(), level);
        handleResult(result);
    }

    private void unlockAll() {
        ActionResult result = app.services().game().unlockAllLevels();
        handleResult(result);
    }

    private void refreshWallet() {
        coinValue.setText(Integer.toString(user.getWallet().getCoins()));
        gemValue.setText(Integer.toString(user.getWallet().getGems()));
    }

    private void handleResult(ActionResult result) {
        if (result.isSuccessful()) {
            theme.showSuccess(status, result.getMessage());
        } else {
            theme.showError(status, result.getMessage());
        }
    }
}
