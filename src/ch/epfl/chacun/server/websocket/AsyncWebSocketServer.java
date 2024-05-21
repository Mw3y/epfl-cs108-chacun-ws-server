package ch.epfl.chacun.server.websocket;

import ch.epfl.chacun.server.rfc6455.PayloadData;
import ch.epfl.chacun.server.rfc6455.RFC6455;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;

/**
 * An asynchronous WebSocket server that listens for incoming connections and messages from clients.
 * @author Maxence Espagnet (sciper: 372808)
 */
public abstract class AsyncWebSocketServer<T> extends WebSocketBroadcaster<T> {

    /**
     * The maximum size of a message that can be sent or received.
     */
    public static final int MAX_MESSAGE_SIZE = 512;

    /**
     * The interval in milliseconds at which the server sends ping messages to clients.
     */
    private static final long PING_INTERVAL = 6 * 1000; // 1 minute

    /**
     * The timeout watcher that keeps track of the last time a client sent a pong message.
     */
    TimeoutWatcher<T> timeoutWatcher = new TimeoutWatcher<>(PING_INTERVAL);

    /**
     * Create a new asynchronous WebSocket server that listens on the specified address and port.
     *
     * @param bindAddr the address to bind to
     * @param bindPort the port to bind to
     * @throws IOException if an I/O error occurs
     */
    public AsyncWebSocketServer(String bindAddr, int bindPort) throws IOException {
        InetSocketAddress sockAddr = new InetSocketAddress(InetAddress.getByName(bindAddr), bindPort);
        // Create a socket channel and bind to local bind address
        AsynchronousServerSocketChannel serverSock = AsynchronousServerSocketChannel.open().bind(sockAddr);
        // Start to accept the connection from client
        serverSock.accept(serverSock, new ChannelConnectionHandler<T>(this));
        System.out.println(STR."Server started on \{bindAddr}:\{bindPort}");
    }

    /**
     * Start reading asynchronously a message from the client.
     *
     * @param ws the socket channel to read messages from
     */
    public void startRead(WebSocketChannel<T> ws) {
        final ByteBuffer buf = ByteBuffer.allocate(MAX_MESSAGE_SIZE);
        ws.getChannel().read(buf, ws, new ChannelReadHandler<T>(this, buf));
    }

    /**
     * Start to write asynchronously a message to the client.
     *
     * @param ws  the socket channel to write messages to
     * @param buffer the buffer containing the message to write
     */
    public void startWrite(WebSocketChannel<T> ws, ByteBuffer buffer) {
        buffer.rewind(); // Rewind the buffer to start reading from the beginning
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
