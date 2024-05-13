package ch.epfl.chacun.server.websocket;

import ch.epfl.chacun.server.rfc6455.TextEncoder;
import org.w3c.dom.Text;

import java.io.IOException;
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
        try {
            channel.write(TextEncoder.encodeToPayload("Hello World"));
        } catch (IOException e) {
            e.printStackTrace();
        }
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

