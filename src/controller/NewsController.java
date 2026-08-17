package controller;

import model.News;
import model.User;

import java.util.ArrayList;
import java.util.List;

public class NewsController {
    private final AuthController authController;

    public NewsController(AuthController authController) {
        this.authController = authController;
    }

    public List<News> showUnreadNews() {
        User user = authController.getCurrentUser();
        if (user == null) {
            return List.of();
        }
        ArrayList<News> result = new ArrayList<>();
        for (News news : user.getNews()) {
            if (news.isUnread()) {
                result.add(news);
                news.markAsRead();
            }
        }
        authController.saveCurrentState();
        return List.copyOf(result);
    }

    public List<News> showAllNews() {
        User user = authController.getCurrentUser();
        return user == null ? List.of() : user.getNews();
    }

    public boolean hasUnreadNews() {
        User user = authController.getCurrentUser();
        return user != null && user.getNews().stream().anyMatch(News::isUnread);
    }

    public ActionResult markAsRead(News item) {
        User user = authController.getCurrentUser();
        if (user == null) {
            return ActionResult.failure("Login is required.");
        }
        if (item == null || !user.getNews().contains(item)) {
            return ActionResult.failure("News item does not exist.");
        }
        if (!item.isUnread()) {
            return ActionResult.success("News item is already read.");
        }
        item.markAsRead();
        ActionResult saveResult = authController.saveCurrentState();
        return saveResult.isSuccessful()
            ? ActionResult.success("Marked as read.") : saveResult;
    }

    public ActionResult markAllAsRead() {
        User user = authController.getCurrentUser();
        if (user == null) {
            return ActionResult.failure("Login is required.");
        }
        int marked = 0;
        for (News news : user.getNews()) {
            if (news.isUnread()) {
                news.markAsRead();
                marked++;
            }
        }
        ActionResult saveResult = authController.saveCurrentState();
        if (!saveResult.isSuccessful()) {
            return saveResult;
        }
        return ActionResult.success(marked == 0
            ? "No unread news." : "Marked " + marked + " item(s) as read.");
    }
}
