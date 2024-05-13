package ch.epfl.chacun.server.websocket;

public class WebSocketServer extends AbstractWebSocketServer {

    public WebSocketServer(int port) {
        super(port);
    }

    @Override
    protected void onOpen(GameWebSocket ws) {
        System.out.println("Connection opened");
    }

    @Override
    protected void onMessage(GameWebSocket ws, String message) {
        System.out.println("message.received: " + message);
        ws.sendText("hello.world.from.server");
        ws.sendPing();
    }

    @Override
    protected void onPing(GameWebSocket ws) {
        System.out.println("Ping received");
        ws.sendPong();
    }

    @Override
    protected void onPong(GameWebSocket ws) {
        System.out.println("Pong received");
    }

    @Override
    protected void onClose(GameWebSocket ws) {
        System.out.println("Connection closed");
    }
}

