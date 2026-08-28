package network.protocol;

import java.io.Serial;
import java.io.Serializable;

/** Response envelope for every Phase 3 request. */
public final class NetworkResponse implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final boolean successful;
    private final String message;
    private final Object payload;

    private NetworkResponse(boolean successful, String message, Object payload) {
        this.successful = successful;
        this.message = message == null ? "" : message;
        this.payload = payload;
    }

    public static NetworkResponse success(String message) {
        return new NetworkResponse(true, message, null);
    }

    public static NetworkResponse success(String message, Object payload) {
        return new NetworkResponse(true, message, payload);
    }

    public static NetworkResponse failure(String message) {
        return new NetworkResponse(false, message, null);
    }

    public boolean isSuccessful() {
        return successful;
    }

    public String getMessage() {
        return message;
    }

    public Object getPayload() {
        return payload;
    }
}
