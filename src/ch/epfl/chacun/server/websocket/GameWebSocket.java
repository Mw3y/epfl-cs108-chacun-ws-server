package ch.epfl.chacun.server.websocket;

import ch.epfl.chacun.server.rfc6455.RFC6455;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class GameWebSocket {

    private final SocketChannel channel;
    private final WebSocketEventListener listener;

    public GameWebSocket(SocketChannel channel, WebSocketEventListener listener) {
        this.channel = channel;
        this.listener = listener;
    }

    public boolean sendText(String message) {
        return sendBytes(RFC6455.encodeText(message));
    }

    public boolean sendPing() {
        return sendBytes(RFC6455.PING);
    }

    public boolean sendPong() {
        return sendBytes(RFC6455.PONG);
    }

    public boolean close() {
        try {
            // listener.onClose(this);
            channel.socket().close();
            channel.close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public boolean sendBytes(ByteBuffer buffer) {
        try {
            channel.write(buffer);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

}
