package ch.epfl.chacun.server.websocket;

import java.nio.channels.SocketChannel;

public class WebSocketServer extends AbstractWebSocketServer {

    public WebSocketServer(int port) {
        super(port);
    }

    @Override
    protected void onOpen(SocketChannel channel) {
        System.out.println("Connection opened");
    }

    @Override
    protected void onMessage(SocketChannel channel, String message) {
        System.out.println(message);
    }

    @Override
    protected void onPing(SocketChannel channel) {
        System.out.println("Ping received");
    }

    @Override
    protected void onPong(SocketChannel channel) {
        System.out.println("Pong received");
    }

    @Override
    protected void onClose(SocketChannel channel) {
        System.out.println("Connection closed");
    }
}

