package network.client;

import network.protocol.NetworkOperation;
import network.protocol.NetworkRequest;
import network.protocol.NetworkResponse;
import network.protocol.Phase3Protocol;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Lightweight request/response TCP client. A fresh connection is used per request so account
 * persistence is independent from multiplayer session lifecycle added in later Phase 3 stages.
 */
public final class PvzNetworkClient {
    private static final int DEFAULT_CONNECT_TIMEOUT_MS = 2500;
    private static final int DEFAULT_READ_TIMEOUT_MS = 5000;

    private final String host;
    private final int port;
    private final int connectTimeoutMs;
    private final int readTimeoutMs;

    public PvzNetworkClient(String host, int port) {
        this(host, port, DEFAULT_CONNECT_TIMEOUT_MS, DEFAULT_READ_TIMEOUT_MS);
    }

    public PvzNetworkClient(String host, int port, int connectTimeoutMs, int readTimeoutMs) {
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("Server host cannot be empty.");
        }
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("Server port must be between 1 and 65535.");
        }
        this.host = host.trim();
        this.port = port;
        this.connectTimeoutMs = Math.max(100, connectTimeoutMs);
        this.readTimeoutMs = Math.max(100, readTimeoutMs);
    }

    public NetworkResponse request(NetworkOperation operation, Object... arguments) throws IOException {
        NetworkRequest request = new NetworkRequest(Phase3Protocol.VERSION, operation, arguments);
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), connectTimeoutMs);
            socket.setSoTimeout(readTimeoutMs);

            try (ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
                 ObjectInputStream input = new ObjectInputStream(socket.getInputStream())) {
                output.writeObject(request);
                output.flush();
                Object received = input.readObject();
                if (!(received instanceof NetworkResponse response)) {
                    throw new IOException("Server returned an invalid response.");
                }
                return response;
            } catch (ClassNotFoundException exception) {
                throw new IOException("Could not decode server response.", exception);
            }
        }
    }

    public boolean ping() {
        try {
            return request(NetworkOperation.PING).isSuccessful();
        } catch (IOException exception) {
            return false;
        }
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }
}
