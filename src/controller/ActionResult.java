package controller;

public final class ActionResult {
    private final boolean successful;
    private final String message;

    private ActionResult(boolean successful, String message) {
        this.successful = successful;
        this.message = message;
    }

    public static ActionResult success(String message) {
        return new ActionResult(true, message);
    }

    public static ActionResult failure(String message) {
        return new ActionResult(false, message);
    }

    public boolean isSuccessful() {
        return successful;
    }

    public String getMessage() {
        return message;
    }
}
