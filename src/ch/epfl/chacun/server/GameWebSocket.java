package ch.epfl.chacun.server;

import ch.epfl.chacun.server.rfc6455.RFC6455;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class GameWebSocket {

    private final SocketChannel channel;

    public GameWebSocket(SocketChannel channel) {
        this.channel = channel;
    }

    public boolean sendText(String message) {
        return send(RFC6455.encodeText(message));
    }

    public boolean sendPing() {
        return send(RFC6455.PING);
    }

    public boolean sendPong() {
        return send(RFC6455.PONG);
    }

    public boolean send(ByteBuffer buffer) {
        try {
            channel.write(buffer);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

}
