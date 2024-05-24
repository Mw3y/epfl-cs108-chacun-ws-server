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
 *
 * @author Maxence Espagnet (sciper: 372808)
 */
public abstract class AsyncWebSocketServer<T> extends WebSocketBroadcaster<T> {

    /**
     * The timeout watcher that keeps track of the last time a client sent a pong message.
     */
    private final TimeoutWatcher<T> timeoutWatcher;

    /**
     * The maximum size of a payload that can be received (in bytes).
     */
    private final int maxBufferSize;

    /**
     * Create a new asynchronous WebSocket server that listens on the specified address and port.
     *
     * @param bindAddr      the address to bind to
     * @param bindPort      the port to bind to
     * @param maxBufferSize the maximum size of a payload that can be received (in bytes)
     * @param pingInterval  the interval in milliseconds at which the client should send a pong message
     * @throws IOException if an I/O error occurs
     */
    public AsyncWebSocketServer(String bindAddr, int bindPort, int maxBufferSize, int pingInterval) throws IOException {
        InetSocketAddress sockAddr = new InetSocketAddress(InetAddress.getByName(bindAddr), bindPort);
        // Create a socket channel and bind to local bind address
        AsynchronousServerSocketChannel serverSock = AsynchronousServerSocketChannel.open().bind(sockAddr);
        // Start to accept the connection from client
        serverSock.accept(serverSock, new ChannelConnectionHandler<>(this));
        // Server settings
        this.maxBufferSize = maxBufferSize;
        this.timeoutWatcher = new TimeoutWatcher<>(pingInterval);
        System.out.println(STR."Server started on \{bindAddr}:\{bindPort}");
    }

    /**
     * Start reading asynchronously a message from the client.
     *
     * @param ws the socket channel to read messages from
     */
    public void startRead(WebSocketChannel<T> ws) {
        ByteBuffer buffer = ByteBuffer.allocate(maxBufferSize); // If the data is larger, it will be considered invalid
        ws.getAsyncChannel().read(buffer, ws, new ChannelReadHandler<>(this, buffer));
    }

    /**
     * Start to write asynchronously a message to the client.
     *
     * @param ws     the socket channel to write messages to
     * @param buffer the buffer containing the message to write
     */
    public void startWrite(WebSocketChannel<T> ws, ByteBuffer buffer) {
        buffer.rewind(); // Rewind the buffer to start reading from the beginning
        ws.getAsyncChannel().write(buffer, ws, new ChannelWriteHandler<>(this));
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
            case BINARY -> onBinary(ws, RFC6455.decodeFrame(payload));
            case TEXT -> onMessage(ws, RFC6455.decodeTextFrame(payload));
            case PING -> onPing(ws);
            case PONG -> onPong(ws);
            case CLOSE -> ws.terminate();
        }
    }
}
