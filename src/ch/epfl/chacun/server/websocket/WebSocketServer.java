package ch.epfl.chacun.server.websocket;

import ch.epfl.chacun.server.rfc6455.CloseStatusCode;
import ch.epfl.chacun.server.rfc6455.RFC6455;

public class WebSocketServer extends AbstractWebSocketServer {

    public WebSocketServer(int port) {
        super(port);
    }

    @Override
    protected void onOpen(WebSocketChannel ws) {
        System.out.println("Connection opened");
    }

    @Override
    protected void onMessage(WebSocketChannel ws, String message) {
        System.out.println("message.received: " + message);
        ws.sendText("hello.world.from.server");
    }

    @Override
    protected void onPing(WebSocketChannel ws) {
        System.out.println("Ping received");
        ws.sendPong();
    }

    @Override
    protected void onPong(WebSocketChannel ws) {
        System.out.println("Pong received");
    }

    @Override
    protected void onClose(WebSocketChannel ws) {
        System.out.println("Connection closed");
    }
}

