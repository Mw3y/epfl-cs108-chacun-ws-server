package ch.epfl.chacun.server.websocket;

import ch.epfl.chacun.server.rfc6455.PayloadData;
import ch.epfl.chacun.server.rfc6455.RFC6455;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;

/**
 * @author Steven Ou
 */
public abstract class AsyncWebSocketServer<T> extends WebSocketBroadcaster<T> {

    public static final int MAX_MESSAGE_SIZE = 256;

    TimeoutWatcher<T> timeoutWatcher = new TimeoutWatcher<>();

    public AsyncWebSocketServer(String bindAddr, int bindPort) throws IOException {
        InetSocketAddress sockAddr = new InetSocketAddress(InetAddress.getByName(bindAddr), bindPort);
        // Create a socket channel and bind to local bind address
        AsynchronousServerSocketChannel serverSock = AsynchronousServerSocketChannel.open().bind(sockAddr);
        // Start to accept the connection from client
        serverSock.accept(serverSock, new ChannelConnectionHandler<T>(this));
        System.out.println(STR."Server started on \{bindAddr}:\{bindPort}");
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
     * @param buffer the buffer containing the message to write
     */
    public void startWrite(WebSocketChannel<T> ws, ByteBuffer buffer) {
        buffer.rewind();
        ws.getChannel().write(buffer, ws, new ChannelWriteHandler<T>(this));
    }

    @Override
    protected void onOpen(WebSocketChannel<T> ws) {
        System.out.println("New connection opened");
        timeoutWatcher.watch(ws);
    }

    @Override
    protected void onPing(WebSocketChannel<T> ws) {
        ws.sendPong();
    }

    @Override
    protected void onPong(WebSocketChannel<T> ws) {
        timeoutWatcher.registerPong(ws);
    }

    @Override
    protected void onClose(WebSocketChannel<T> ws) {
        System.out.println("Connection closed");
        timeoutWatcher.unwatch(ws);
    }

    @Override
    public void dispatch(PayloadData payload, WebSocketChannel<T> ws) {
        switch (payload.opCode()) {
            case TEXT -> onMessage(ws, RFC6455.decodeTextFrame(payload));
            case PING -> onPing(ws);
            case PONG -> onPong(ws);
            case CLOSE -> ws.terminate();
        }
    }
}
