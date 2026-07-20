package view;

import model.News;

import java.util.List;

public class NewsView {
    public void showNews(List<News> newsList) {
        if (newsList.isEmpty()) {
            System.out.println("No news.");
            return;
        }
        for (News news : newsList) {
            System.out.println("[" + news.getCreatedAt() + "] " + news.getTitle());
            System.out.println(news.getContent());
        }
    }
}
