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
}
