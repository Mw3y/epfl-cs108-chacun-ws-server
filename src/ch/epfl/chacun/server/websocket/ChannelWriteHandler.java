package ch.epfl.chacun.server.websocket;

import java.nio.channels.CompletionHandler;

public class ChannelWriteHandler<T> implements CompletionHandler<Integer, WebSocketChannel<T>> {

    private final AsyncWebSocketServer<T> server;

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
        ws.terminate();
    }
}
