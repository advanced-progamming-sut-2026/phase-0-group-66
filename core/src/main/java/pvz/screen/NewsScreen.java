package pvz.screen;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import controller.NewsController;
import model.News;
import pvz.PvzApplication;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class NewsScreen extends AuthenticatedUiScreen {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter
        .ofPattern("yyyy-MM-dd  HH:mm")
        .withZone(ZoneId.systemDefault());

    private final NewsController controller;
    private final Set<News> unreadOnOpen;

    public NewsScreen(PvzApplication app) {
        super(app);
        controller = app.services().news();
        unreadOnOpen = captureUnread();
        controller.showUnreadNews();
        buildUi();
    }

    private Set<News> captureUnread() {
        Set<News> unread = new HashSet<>();
        for (News news : controller.showAllNews()) {
            if (news.isUnread()) {
                unread.add(news);
            }
        }
        return unread;
    }

    private void buildUi() {
        Table panel = theme.dialogPanel();
        panel.add(titleBar("NEWS")).growX().padBottom(12f);
        panel.row();

        ScrollPane scroll = new ScrollPane(buildNewsList(), theme.skin());
        scroll.setFadeScrollBars(false);
        scroll.setScrollingDisabled(true, false);
        panel.add(scroll).width(890f).height(455f).grow();
        panel.row();

        TextButton back = theme.secondaryButton("Back");
        UiActions.onClick(back, app::showMainMenu);
        panel.add(back).width(190f).height(50f).padTop(12f);
        root.add(panel).width(1010f).height(620f).center();
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
        Table card = theme.insetPanel(14f);
        Table header = new Table();
        header.add(theme.heading(news.getTitle())).left();
        header.add().expandX();
        if (unreadOnOpen.contains(news)) {
            Label newLabel = theme.heading("NEW");
            header.add(newLabel).padRight(14f);
        }
        header.add(theme.fieldLabel(formatDate(news.getCreatedAt()))).right();
        card.add(header).growX();
        card.row();

        Label content = theme.bodyLabel(news.getContent());
        content.setWrap(true);
        card.add(content).growX().left().padTop(8f);
        return card;
    }

    private String formatDate(String value) {
        try {
            return DATE_FORMAT.format(Instant.parse(value));
        } catch (RuntimeException exception) {
            return value == null ? "" : value;
        }
    }
}
