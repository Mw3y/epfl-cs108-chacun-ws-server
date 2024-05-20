package ch.epfl.chacun.server.websocket;

import ch.epfl.chacun.server.rfc6455.CloseStatusCode;
import ch.epfl.chacun.server.rfc6455.RFC6455;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;

/**
 * Represents a WebSocket channel between the server and a client.
 * @param <T> The type of the context attached to the WebSocket channel.
 * @author Maxence Espagnet (sciper: 372808)
 */
public class WebSocketChannel<T> {

    /**
     * The underlying AsynchronousSocketChannel.
     */
    private final AsynchronousSocketChannel channel;

    /**
     * The server managing the WebSocket channel.
     */
    private final AsyncWebSocketServer<T> server;

    /**
     * The context attached to the WebSocket channel.
     */
    private T context;

    /**
     * Create a new WebSocket channel with the given AsynchronousSocketChannel and server.
     * @param channel The AsynchronousSocketChannel.
     * @param server The server managing the WebSocket channel.
     */
    public WebSocketChannel(AsynchronousSocketChannel channel, AsyncWebSocketServer<T> server) {
        this.channel = channel;
        this.server = server;
    }

    /**
     * Attach a context to the WebSocket channel.
     * @param context The context to attach.
     */
    public void attachContext(T context) {
        this.context = context;
    }

    /**
     * Returns the context attached to the WebSocket channel.
     */
    public T getContext() {
        return context;
    }

    /**
     * Returns the underlying AsynchronousSocketChannel.
     * @return The underlying AsynchronousSocketChannel.
     */
    public AsynchronousSocketChannel getChannel() {
        return channel;
    }

    /**
     * Send a byte buffer to the client through the WebSocket channel.
     * @param buffer The byte buffer to send
     */
    public void sendBytes(ByteBuffer buffer) {
        server.startWrite(this, buffer);
    }

    /**
     * Send a text message to the client through the WebSocket channel.
     * @param message The text message to send.
     */
    public void sendText(String message) {
        sendBytes(RFC6455.encodeTextFrame(message));
    }

    /**
     * Send a ping control frame to the client through the WebSocket channel.
     */
    public void sendPing() {
        sendBytes(RFC6455.PING);
    }

    /**
     * Send a pong control frame to the client through the WebSocket channel.
     */
    public void sendPong() {
        sendBytes(RFC6455.PONG);
    }

    /**
     * Send a close control frame to the client through the WebSocket channel.
     * <p>
     * This starts the closing handshake with the client.
     * @param code The close status code.
     * @param reason The close reason.
     */
    public void close(CloseStatusCode code, String reason) {
        sendBytes(RFC6455.encodeCloseFrame(code, reason));
    }

    /**
     * Broadcast a text message to all clients subscribed to the given broadcast channel id.
     * @param id The broadcast channel id.
     * @param message The text message to broadcast.
     */
    public void broadcast(String id, String message) {
        server.broadcastTo(id, RFC6455.encodeTextFrame(message));
    }

    /**
     * Broadcast a byte buffer to all clients subscribed to the given broadcast channel id.
     * @param id The broadcast channel id.
     * @param buffer The byte buffer to broadcast.
     */
    public void broadcast(String id, ByteBuffer buffer) {
        server.broadcastTo(id, buffer);
    }

    /**
     * Subscribe to a broadcast channel with the given id.
     * @param id The broadcast channel id.
     */
    public void subscribe(String id) {
        server.subscribeTo(id, this);
    }

    /**
     * Unsubscribe from a broadcast channel with the given id.
     * @param id The broadcast channel id.
     */
    public void unsubscribe(String id) {
        server.unsubscribeFrom(id, this);
    }

    /**
     * Terminate the WebSocket channel on the server side.
     * <p>
     * This is the last part of the closing handshake.
     * @return {@code true} if the channel was successfully terminated, {@code false} otherwise.
     */
    public boolean terminate() {
        try {
            server.onClose(this);
            getChannel().close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof WebSocketChannel<?> ws) {
            return ws.channel.equals(channel);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return channel.hashCode();
    }
}
