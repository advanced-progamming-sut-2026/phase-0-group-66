package network.protocol;

import java.io.Serial;
import java.io.Serializable;
import java.util.Arrays;

/** A small extensible request envelope shared by the client and server. */
public final class NetworkRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final int protocolVersion;
    private final NetworkOperation operation;
    private final Object[] arguments;
    private final String authToken;

    public NetworkRequest(int protocolVersion, NetworkOperation operation, Object... arguments) {
        this(protocolVersion, operation, null, arguments);
    }

    public NetworkRequest(int protocolVersion, NetworkOperation operation, String authToken,
                          Object... arguments) {
        this.protocolVersion = protocolVersion;
        this.operation = operation;
        this.authToken = authToken;
        this.arguments = arguments == null ? new Object[0] : Arrays.copyOf(arguments, arguments.length);
    }

    public int getProtocolVersion() {
        return protocolVersion;
    }

    public NetworkOperation getOperation() {
        return operation;
    }

    public String getAuthToken() {
        return authToken;
    }

    public Object[] getArguments() {
        return Arrays.copyOf(arguments, arguments.length);
    }

    public Object argument(int index) {
        return arguments[index];
    }
}
