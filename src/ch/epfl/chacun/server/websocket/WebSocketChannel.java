package ch.epfl.chacun.server.websocket;

import ch.epfl.chacun.server.rfc6455.CloseStatusCode;
import ch.epfl.chacun.server.rfc6455.RFC6455;
import ch.epfl.chacun.server.websocket.handlers.ChannelWriteHandler;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class WebSocketChannel<T> {

    private final AsynchronousSocketChannel channel;
    private final WebSocketEventListener<T> listener;
    private T context;

    public WebSocketChannel(AsynchronousSocketChannel channel, WebSocketEventListener<T> listener) {
        this.channel = channel;
        this.listener = listener;
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

    public boolean sendText(String message) {
        return sendBytes(RFC6455.encodeTextFrame(message));
    }

    public boolean sendPing() {
        return sendBytes(RFC6455.PING);
    }

    public boolean sendPong() {
        return sendBytes(RFC6455.PONG);
    }

    public boolean close(CloseStatusCode code, String reason) {
        return sendBytes(RFC6455.encodeCloseFrame(code, reason));
    }

    public boolean terminate() {
        try {
            listener.onClose(this);
            getChannel().close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public boolean sendBytes(ByteBuffer buffer) {
        buffer.rewind();
        channel.write(buffer);
        return true;
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
