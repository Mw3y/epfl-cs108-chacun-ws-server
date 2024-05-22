package ch.epfl.chacun.server.websocket;

import ch.epfl.chacun.server.rfc6455.PayloadData;

/**
 * Represents a listener for WebSocket server events.
 * @author Maxence Espagnet (sciper: 372808)
 */
public abstract class WebSocketEventListener<T> {

    /**
     * Called when a new connection is opened.
     *
     * @param ws The channel of the new connection.
     */
    abstract protected void onOpen(WebSocketChannel<T> ws);

    /**
     * Called when a text message is received.
     *
     * @param ws      The channel of the connection.
     * @param message The message received.
     */
    abstract protected void onMessage(WebSocketChannel<T> ws, String message);

    /**
     * Called when a binary message is received.
     *
     * @param ws      The channel of the connection.
     * @param message The message received.
     */
    abstract protected void onBinary(WebSocketChannel<T> ws, byte[] message);

    /**
     * Called when a ping is received.
     *
     * @param ws The channel of the connection.
     */
    abstract protected void onPing(WebSocketChannel<T> ws);

    /**
     * Called when a pong is received.
     *
     * @param ws The channel of the connection.
     */
    abstract protected void onPong(WebSocketChannel<T> ws);

    /**
     * Called when a connection will close.
     *
     * @param ws The channel of the connection.
     */
    abstract protected void onClose(WebSocketChannel<T> ws);

    /**
     * Dispatches the payload data to the appropriate event handler.
     *
     * @param payload The payload data.
     * @param ws      The channel of the connection.
     */
    abstract public void dispatch(PayloadData payload, WebSocketChannel<T> ws);
}
