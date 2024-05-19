package ch.epfl.chacun.server.websocket;

import ch.epfl.chacun.server.rfc6455.PayloadData;
import ch.epfl.chacun.server.rfc6455.RFC6455;
import ch.epfl.chacun.server.websocket.handlers.ChannelConnectionHandler;
import ch.epfl.chacun.server.websocket.handlers.ChannelReadHandler;
import ch.epfl.chacun.server.websocket.handlers.ChannelWriteHandler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;

/**
 * @author Steven Ou
 */
public abstract class AbstractAsyncWebSocketServer<T> extends WebSocketBroadcaster<T> {

    public static final int MAX_MESSAGE_SIZE = 256;

    public AbstractAsyncWebSocketServer(String bindAddr, int bindPort) throws IOException {
        InetSocketAddress sockAddr = new InetSocketAddress(bindAddr, bindPort);
        // Create a socket channel and bind to local bind address
        AsynchronousServerSocketChannel serverSock = AsynchronousServerSocketChannel.open().bind(sockAddr);
        // Start to accept the connection from client
        serverSock.accept(serverSock, new ChannelConnectionHandler<T>(this));
    }

    /**
     * Start to read message from the client
     *
     * @param ws the socket channel to read messages from
     */
    public void startRead(WebSocketChannel<T> ws) {
        final ByteBuffer buf = ByteBuffer.allocate(MAX_MESSAGE_SIZE);
        ws.getChannel().read(buf, ws, new ChannelReadHandler<T>(this, buf));
    }

    /**
     * Start to write message to the client
     *
     * @param ws  the socket channel to write messages to
     * @param buf the buffer containing the message to write
     */
    public void startWrite(WebSocketChannel<T> ws, final ByteBuffer buf) {
        ws.getChannel().write(buf, ws, new ChannelWriteHandler<T>(this));
    }

    @Override
    public void dispatch(PayloadData payload, WebSocketChannel<T> ws) {
        switch (payload.opCode()) {
            case TEXT -> onMessage(ws, RFC6455.decodeTextFrame(payload));
            case PING -> onPing(ws);
            case PONG -> onPong(ws);
            case CLOSE -> onClose(ws);
        }
    }
}
