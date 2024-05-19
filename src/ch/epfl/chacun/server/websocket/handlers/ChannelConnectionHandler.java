package ch.epfl.chacun.server.websocket.handlers;

import ch.epfl.chacun.server.websocket.AbstractAsyncWebSocketServer;

import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

public class ChannelConnectionHandler<T> implements CompletionHandler<AsynchronousSocketChannel, AsynchronousServerSocketChannel> {

    private final AbstractAsyncWebSocketServer<T> server;

    public ChannelConnectionHandler(AbstractAsyncWebSocketServer<T> server) {
        this.server = server;
    }

    @Override
    public void completed(AsynchronousSocketChannel channel, AsynchronousServerSocketChannel serverChannel) {
        // A connection is accepted, start to accept next connection
        serverChannel.accept(serverChannel, this);
        // start to read message from the client
        server.startRead(channel);
    }

    @Override
    public void failed(Throwable exc, AsynchronousServerSocketChannel attachment) {
        System.out.println("fail to accept a connection");
    }
}
