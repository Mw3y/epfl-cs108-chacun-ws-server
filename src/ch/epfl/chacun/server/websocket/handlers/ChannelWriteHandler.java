package ch.epfl.chacun.server.websocket.handlers;

import ch.epfl.chacun.server.websocket.AbstractAsyncWebSocketServer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

public class ChannelWriteHandler<T> implements CompletionHandler<Integer, AsynchronousSocketChannel> {

    private final AbstractAsyncWebSocketServer<T> server;

    public ChannelWriteHandler(AbstractAsyncWebSocketServer<T> server) {
        this.server = server;
    }

    @Override
    public void completed(Integer result, AsynchronousSocketChannel channel) {
        // Finish to write message to client, nothing to do
        System.out.println("Message written to client");
    }

    @Override
    public void failed(Throwable exc, AsynchronousSocketChannel channel) {
        System.out.println("Failed to write message to client... closing channel");
        try {
            channel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
