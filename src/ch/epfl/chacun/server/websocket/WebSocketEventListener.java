package ch.epfl.chacun.server.websocket;

import ch.epfl.chacun.server.rfc6455.PayloadData;

/**
 * Represents a listener for WebSocket server events.
 */
public abstract class WebSocketEventListener {

    /**
     * Called when a new connection is opened.
     *
     * @param ws The channel of the new connection.
     */
    abstract protected void onOpen(WebSocketChannel ws);

    /**
     * Called when a message is received.
     *
     * @param ws      The channel of the connection.
     * @param message The message received.
     */
    abstract protected void onMessage(WebSocketChannel ws, String message);

    /**
     * Called when a ping is received.
     *
     * @param ws The channel of the connection.
     */
    abstract protected void onPing(WebSocketChannel ws);

    /**
     * Called when a pong is received.
     *
     * @param ws The channel of the connection.
     */
    abstract protected void onPong(WebSocketChannel ws);

    /**
     * Called when a connection will close.
     *
     * @param ws The channel of the connection.
     */
    abstract protected void onClose(WebSocketChannel ws);

    /**
     * Dispatches the payload data to the appropriate event handler.
     *
     * @param payload The payload data.
     * @param ws      The channel of the connection.
     */
    abstract protected void dispatch(PayloadData payload, WebSocketChannel ws);
}
