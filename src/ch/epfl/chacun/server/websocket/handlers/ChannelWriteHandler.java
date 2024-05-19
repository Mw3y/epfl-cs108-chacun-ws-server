package ch.epfl.chacun.server.websocket.handlers;

import ch.epfl.chacun.server.websocket.AbstractAsyncWebSocketServer;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

public class ChannelWriteHandler<T> implements CompletionHandler<Integer, AsynchronousSocketChannel> {

    final ByteBuffer buf = ByteBuffer.allocate(256);

    private final AbstractAsyncWebSocketServer<T> server;

    public ChannelWriteHandler(AbstractAsyncWebSocketServer<T> server) {
        this.server = server;
    }

    @Override
    public void completed(Integer result, AsynchronousSocketChannel channel) {
        // finish to write message to client, nothing to do
        System.out.println("Message written to client");
    }

    @Override
    public void failed(Throwable exc, AsynchronousSocketChannel channel) {
        //fail to write message to client
        System.out.println("Fail to write message to client");
    }
}
