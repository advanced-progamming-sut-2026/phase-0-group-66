package pvz.screen;

import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import controller.ActionResult;
import model.Chapter;
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
    private final Label selectedChapterValue;
    private final Label status;
    private final TextField amountField;
    private String selectedChapter;

    public CheatScreen(PvzApplication app) {
        super(app);
        selectedChapter = CHAPTERS[0];
        coinValue = theme.settingsLabel("");
        gemValue = theme.settingsLabel("");
        selectedChapterValue = theme.settingsLabel(selectedChapter);
        status = statusLabel();
        status.setWrap(false);
        amountField = textField("Amount");
        amountField.setTextFieldFilter(new TextField.TextFieldFilter.DigitsOnlyFilter());
        buildUi();
        refreshWallet();
    }

    private void buildUi() {
        Table panel = theme.dialogPanel();
        panel.top();
        panel.add(theme.settingsTitle("Wallet & Cheat Codes"))
            .width(850f).height(58f).center().padBottom(10f);
        panel.row();
        panel.add(buildWalletCard()).width(850f).height(170f).padBottom(12f);
        panel.row();
        panel.add(buildUnlockCard()).width(850f).height(300f).padBottom(8f);
        panel.row();
        panel.add(status).width(800f).height(30f);
        panel.row();

        TextButton back = theme.secondaryButton("Back to Settings");
        UiActions.onClick(back, app::showSettings);
        panel.add(back).width(230f).height(50f).padTop(5f);
        root.add(panel).width(980f).height(675f).center();
    }

    private Table buildWalletCard() {
        Table card = theme.settingsCardPanel(14f);
        card.add(theme.settingsLabel("WALLET")).left().colspan(5).padBottom(6f);
        card.row();

        addWalletBadge(card, UiTheme.COIN_ICON, "Coin Wallet", coinValue);
        addWalletBadge(card, UiTheme.GEM_ICON, "Gem Wallet", gemValue);
        card.row().padTop(10f);

        card.add(theme.settingsLabel("Cheat amount")).left().padRight(8f);
        card.add(amountField).width(180f).height(46f).padRight(12f);

        TextButton addCoins = theme.primaryButton("Add Coins");
        TextButton addGems = theme.tertiaryButton("Add Gems");
        UiActions.onClick(addCoins, () -> addCurrency("coin"));
        UiActions.onClick(addGems, () -> addCurrency("gem"));
        card.add(addCoins).width(180f).height(48f).padRight(8f);
        card.add(addGems).width(180f).height(48f);
        return card;
    }

    private void addWalletBadge(Table card, String iconId, String title, Label value) {
        Table badge = theme.settingsBadgePanel(8f);
        Image icon = theme.image(iconId);
        if (icon != null) {
            badge.add(icon).size(34f).padRight(7f);
        }
        badge.add(theme.settingsLabel(title)).padRight(10f);
        badge.add(value);
        card.add(badge).width(390f).height(54f).padRight(10f);
    }

    private Table buildUnlockCard() {
        Table card = theme.settingsCardPanel(14f);
        card.top().left();
        card.add(theme.settingsLabel("LEVEL UNLOCK CHEATS"))
            .left().colspan(4).padBottom(5f);
        card.row();
        card.add(theme.bodyLabel("Unlock any level directly. Previous levels do not need to be completed."))
            .left().colspan(4).padBottom(8f);
        card.row();

        card.add(theme.settingsLabel("Chapter:")).left().padRight(8f);
        card.add(selectedChapterValue).left().colspan(3);
        card.row().padTop(5f);

        Table chapterButtons = new Table();
        for (String chapter : CHAPTERS) {
            TextButton button = theme.secondaryButton(shortChapterName(chapter));
            UiActions.onClick(button, () -> selectChapter(chapter));
            chapterButtons.add(button).width(190f).height(44f).padRight(6f);
        }
        card.add(chapterButtons).left().colspan(4).padBottom(10f);
        card.row();

        card.add(theme.settingsLabel("Unlock level:")).left().padRight(8f);
        Table levelButtons = new Table();
        for (int level = 1; level <= 4; level++) {
            int selectedLevel = level;
            TextButton button = theme.primaryButton("Level " + level);
            UiActions.onClick(button, () -> unlockLevel(selectedLevel));
            levelButtons.add(button).width(135f).height(46f).padRight(7f);
        }
        card.add(levelButtons).left().colspan(3);
        card.row().padTop(12f);

        TextButton unlockAll = theme.tertiaryButton("Unlock All Adventure + Mini Games");
        UiActions.onClick(unlockAll, this::unlockAll);
        card.add(unlockAll).width(410f).height(50f).left().colspan(4);
        return card;
    }

    private String shortChapterName(String chapter) {
        if ("Ancient Egypt".equals(chapter)) {
            return "Egypt";
        }
        if ("Frostbite Caves".equals(chapter)) {
            return "Frostbite";
        }
        if ("Big Wave Beach".equals(chapter)) {
            return "Beach";
        }
        return "Dark Ages";
    }

    private void selectChapter(String chapter) {
        selectedChapter = chapter;
        selectedChapterValue.setText(chapter);
        theme.showSuccess(status, "Selected " + chapter + ".");
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

    private void unlockLevel(int level) {
        ActionResult result = app.services().game().unlockLevelCheat(selectedChapter, level);
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
