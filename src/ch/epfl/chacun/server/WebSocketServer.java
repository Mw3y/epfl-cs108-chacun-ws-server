package ch.epfl.chacun.server;

public class WebSocketServer extends AbstractWebSocketServer {

    public WebSocketServer(int port) {
        super(port);
    }

    @Override
    public void onOpen() {
        System.out.println("Connection opened");
    }

    @Override
    public void onMessage(String message) {
        System.out.println(message);
    }

    @Override
    public void onPing() {
        System.out.println("Ping received");
    }

    @Override
    public void onPong() {
        System.out.println("Pong received");
    }

    @Override
    public void onClose() {
        System.out.println("Connection closed");
    }
}

