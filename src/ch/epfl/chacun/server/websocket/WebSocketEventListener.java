package ch.epfl.chacun.server.websocket;

import ch.epfl.chacun.server.rfc6455.PayloadData;

import java.nio.channels.SocketChannel;

/**
 * Represents a listener for WebSocket server events.
 */
public abstract class WebSocketEventListener {

    /**
     * Called when a new connection is opened.
     *
     * @param channel The channel of the new connection.
     */
    abstract protected void onOpen(SocketChannel channel);

    /**
     * Called when a message is received.
     *
     * @param channel The channel of the connection.
     * @param message The message received.
     */
    abstract protected void onMessage(SocketChannel channel, String message);

    /**
     * Called when a ping is received.
     *
     * @param channel The channel of the connection.
     */
    abstract protected void onPing(SocketChannel channel);

    /**
     * Called when a pong is received.
     *
     * @param channel The channel of the connection.
     */
    abstract protected void onPong(SocketChannel channel);

    /**
     * Called when a connection is closed.
     *
     * @param channel The channel of the connection.
     */
    abstract protected void onClose(SocketChannel channel);

    /**
     * Dispatches the payload data to the appropriate event handler.
     *
     * @param payload The payload data.
     * @param channel The channel of the connection.
     */
    abstract protected void dispatch(PayloadData payload, SocketChannel channel);
}
