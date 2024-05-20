package ch.epfl.chacun.server.websocket;

import ch.epfl.chacun.server.rfc6455.CloseStatusCode;
import ch.epfl.chacun.server.rfc6455.RFC6455;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;

public class WebSocketChannel<T> {

    private final AsynchronousSocketChannel channel;
    private final AsyncWebSocketServer<T> server;
    private T context;

    public WebSocketChannel(AsynchronousSocketChannel channel, AsyncWebSocketServer<T> server) {
        this.channel = channel;
        this.server = server;
    }

    public void attachContext(T context) {
        this.context = context;
    }

    /**
     * Returns the underlying SocketChannel.
     * @return The underlying SocketChannel.
     */
    public AsynchronousSocketChannel getChannel() {
        return channel;
    }

    public T getContext() {
        return context;
    }

    public void sendText(String message) {
        sendBytes(RFC6455.encodeTextFrame(message));
    }

    public void sendPing() {
        sendBytes(RFC6455.PING);
    }

    public void sendPong() {
        sendBytes(RFC6455.PONG);
    }

    public void close(CloseStatusCode code, String reason) {
        sendBytes(RFC6455.encodeCloseFrame(code, reason));
    }

    public boolean terminate() {
        try {
            server.onClose(this);
            getChannel().close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public void sendBytes(ByteBuffer buffer) {
        server.startWrite(this, buffer);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof WebSocketChannel ws) {
            return ws.channel.equals(channel);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return channel.hashCode();
    }
}
