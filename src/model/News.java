package model;

import java.io.Serializable;
import java.time.Instant;

public class News implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String title;
    private final String content;
    private boolean read;
    private final String createdAt;

    public News(String title, String content) {
        this.title = title;
        this.content = content;
        this.createdAt = Instant.now().toString();
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void markAsRead() {
        read = true;
    }

    public boolean isUnread() {
        return !read;
    }
}
