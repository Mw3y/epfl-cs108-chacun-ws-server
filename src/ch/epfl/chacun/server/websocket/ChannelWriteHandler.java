package ch.epfl.chacun.server.websocket;

import java.nio.channels.CompletionHandler;

/**
 * Handles the asynchronous writing of a message to a WebSocket channel.
 * @param <T> The type of the context attached to the WebSocket channel.
 * @author Maxence Espagnet (sciper: 372808)
 */
public class ChannelWriteHandler<T> implements CompletionHandler<Integer, WebSocketChannel<T>> {

    private final AsyncWebSocketServer<T> server;

    /**
     * Create a new write handler for the server.
     * @param server the server to handle writes for
     */
    public ChannelWriteHandler(AsyncWebSocketServer<T> server) {
        this.server = server;
    }

    @Override
    public void completed(Integer result, WebSocketChannel<T> ws) {
        // Finish to write message to client, nothing to do
        System.out.println("Message written to client");
    }

    @Override
    public void failed(Throwable exc, WebSocketChannel<T> ws) {
        System.out.println("Failed to write message to client... closing channel");
        ws.terminate(); // Close the channel on the server side
    }
}
