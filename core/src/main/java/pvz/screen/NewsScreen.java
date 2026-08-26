package pvz.screen;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import controller.ActionResult;
import controller.NewsController;
import model.News;
import pvz.PvzApplication;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class NewsScreen extends AuthenticatedUiScreen {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter
        .ofPattern("yyyy-MM-dd  HH:mm")
        .withZone(ZoneId.systemDefault());

    private final NewsController controller;
    private final Label status;

    public NewsScreen(PvzApplication app) {
        super(app);
        controller = app.services().news();
        status = statusLabel();
        status.setWrap(false);
        buildUi();
    }

    private void buildUi() {
        Table panel = theme.dialogPanel();
        panel.top();
        panel.add(theme.settingsTitle("News")).width(850f).height(58f).center().padBottom(10f);
        panel.row();

        ScrollPane scroll = new ScrollPane(buildNewsList(), theme.skin());
        scroll.setFadeScrollBars(false);
        scroll.setScrollingDisabled(true, false);
        panel.add(scroll).width(890f).height(455f).grow();
        panel.row();
        panel.add(status).width(850f).height(22f).padTop(2f);
        panel.row();

        Table actions = new Table();
        TextButton markAll = theme.primaryButton("Mark All as Read");
        TextButton back = theme.secondaryButton("Back");
        UiActions.onClick(markAll, this::markAllAsRead);
        UiActions.onClick(back, app::showMainMenu);
        actions.add(markAll).width(240f).height(50f).padRight(10f);
        actions.add(back).width(180f).height(50f);
        panel.add(actions).padTop(0f);
        root.add(panel).width(1010f).height(650f).center();
    }

    private Table buildNewsList() {
        Table list = new Table();
        list.top();
        List<News> newsItems = controller.showAllNews();
        if (newsItems.isEmpty()) {
            list.add(theme.heading("No news yet.")).expand().center();
            return list;
        }

        for (int index = newsItems.size() - 1; index >= 0; index--) {
            News news = newsItems.get(index);
            list.add(buildNewsCard(news)).growX().pad(6f, 8f, 8f, 8f);
            list.row();
        }
        return list;
    }

    private Table buildNewsCard(News news) {
        Table card = theme.settingsCardPanel(14f);
        Table header = new Table();
        header.add(theme.heading(news.getTitle())).left();
        header.add().expandX();
        if (news.isUnread()) {
            Label newLabel = theme.heading("NEW");
            header.add(newLabel).padRight(12f);
        }
        header.add(theme.fieldLabel(formatDate(news.getCreatedAt()))).right();
        card.add(header).growX();
        card.row();

        Label content = theme.bodyLabel(news.getContent());
        content.setWrap(true);
        card.add(content).growX().left().padTop(8f);
        card.row();

        Table footer = new Table();
        footer.left();
        if (news.isUnread()) {
            TextButton markRead = theme.primaryButton("Mark as Read");
            UiActions.onClick(markRead, () -> markAsRead(news));
            footer.add(markRead).width(175f).height(42f).padTop(8f);
        } else {
            Label read = theme.fieldLabel("READ");
            footer.add(read).padTop(8f);
        }
        card.add(footer).growX().left();
        return card;
    }

    private void markAsRead(News news) {
        ActionResult result = controller.markAsRead(news);
        if (!result.isSuccessful()) {
            theme.showError(status, result.getMessage());
            return;
        }
        app.showNews();
    }

    private void markAllAsRead() {
        ActionResult result = controller.markAllAsRead();
        if (!result.isSuccessful()) {
            theme.showError(status, result.getMessage());
            return;
        }
        app.showNews();
    }

    private String formatDate(String value) {
        try {
            return DATE_FORMAT.format(Instant.parse(value));
        } catch (RuntimeException exception) {
            return value == null ? "" : value;
        }
    }
}
