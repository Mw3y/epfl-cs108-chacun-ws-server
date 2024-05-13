package ch.epfl.chacun.server;

import java.nio.channels.SocketChannel;

public class WebSocketServer extends AbstractWebSocketServer {

    public WebSocketServer(int port) {
        super(port);
    }

    @Override
    public void onOpen(SocketChannel channel) {
        System.out.println("Connection opened");
    }

    @Override
    public void onMessage(SocketChannel channel, String message) {
        System.out.println(message);
    }

    @Override
    public void onPing(SocketChannel channel) {
        System.out.println("Ping received");
    }

    @Override
    public void onPong(SocketChannel channel) {
        System.out.println("Pong received");
    }

    @Override
    public void onClose(SocketChannel channel) {
        System.out.println("Connection closed");
    }
}

