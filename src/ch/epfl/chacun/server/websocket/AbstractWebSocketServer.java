package ch.epfl.chacun.server.websocket;

import ch.epfl.chacun.server.GameWebSocket;
import ch.epfl.chacun.server.rfc6455.PayloadData;
import ch.epfl.chacun.server.rfc6455.RFC6455;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Iterator;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class AbstractWebSocketServer extends WebSocketEventListener {

    /**
     * Globally Unique Identifier (GUID, [RFC4122]) in string form.
     */
    private static final String GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

    private boolean isRunning = true;

    private final int port;

    public AbstractWebSocketServer(int port) {
        this.port = port;
    }

    public void start() {
        try (ServerSocketChannel serverSocketChannel = ServerSocketChannel.open()) {
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.bind(new InetSocketAddress(port));

            Selector selector = Selector.open();
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
            while (isRunning) {
                selector.select(1);
                Iterator<SelectionKey> it = selector.selectedKeys().iterator();
                while (it.hasNext()) {
                    SelectionKey key = it.next();
                    if (key.isAcceptable()) {
                        SocketChannel channel = serverSocketChannel.accept();
                        GameWebSocket gameWebSocket = new GameWebSocket(channel);
                        if (channel != null) {
                            channel.configureBlocking(false);
                            channel.register(selector, SelectionKey.OP_READ);
                            onOpen(gameWebSocket);
                        }
                    }
                    if (key.isReadable()) {
                        SocketChannel socketChannel = (SocketChannel) key.channel();

                        ByteBuffer buffer = ByteBuffer.allocate(4096);
                        socketChannel.read(buffer);

                        // for (byte b : buffer.array()) {
                        //     System.out.print(Integer.toBinaryString(b & 0xFF).replace(' ', '0') + " ");
                        // }

                        PayloadData webSocketData = RFC6455.parsePayload(buffer);

                        String content = new String(buffer.array());
                        if (content.startsWith("GET / HTTP/1.1")) {
                            openingHandshake(content, socketChannel);
                        }
                        dispatch(webSocketData, socketChannel);
                    }
                    it.remove();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected void stop() {
        isRunning = false;
    }

    /**
     * Create the key for the "Sec-WebSocket-Accept" header of the handshake upgrade response.
     * <p>
     * For this header field, the server has to take the value (as present
     * in the header field, e.g., the base64-encoded [RFC4648] version minus
     * any leading and trailing whitespace) and concatenate this with the
     * Globally Unique Identifier (GUID, [RFC4122]) "258EAFA5-E914-47DA-
     * 95CA-C5AB0DC85B11" in string form, which is unlikely to be used by
     * network endpoints that do not understand the WebSocket Protocol.  A
     * SHA-1 hash (160 bits) [FIPS.180-3], base64-encoded (see Section 4 of
     * [RFC4648]), of this concatenation is then returned in the server's
     * handshake.
     *
     * @param secWebSocketKey The value of the "Sec-WebSocket-Key" header field in the handshake request.
     * @return the value of the "Sec-WebSocket-Accept" header field in the handshake response.
     */
    private static String encodeSha1AndBase64(String secWebSocketKey) {
        String string = secWebSocketKey.concat(GUID);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] sha1Result = digest.digest(string.getBytes());
            return Base64.getEncoder().encodeToString(sha1Result);
        } catch (NoSuchAlgorithmException ignored) {
            return null;
        }
    }

    /**
     * Tries to upgrade the HTTP connection to a WebSocket connection.
     * <p>
     * The opening handshake is intended to be compatible with HTTP-based
     * server-side software and intermediaries, so that a single port can be
     * used by both HTTP clients talking to that server and WebSocket
     * clients talking to that server. To this end, the WebSocket client's
     * handshake is an HTTP Upgrade request.
     *
     * @param content       The content of the HTTP request.
     * @param socketChannel The socket channel to write the handshake response to.
     * @throws IOException If the handshake response could not be written to the socket channel.
     */
    private static void openingHandshake(String content, SocketChannel socketChannel) throws IOException {
        Pattern secWSKeyPattern = Pattern.compile("Sec-WebSocket-Key:\\s*(.*?)\r\n");
        Matcher secWSKeyMatcher = secWSKeyPattern.matcher(content);

        // If the Sec-WebSocket-Key header is not present, close the connection
        if (!secWSKeyMatcher.find()) {
            closeSocketChannel(socketChannel);
            return;
        }

        // Generate the Sec-WebSocket-Accept header value
        String secWebSocketKey = secWSKeyMatcher.group(1);
        String secWSAcceptHeader = encodeSha1AndBase64(secWebSocketKey);

        // Create the handshake response
        StringJoiner responseBuilder = new StringJoiner("\r\n");
        responseBuilder.add("HTTP/1.1 101 Switching Protocols");
        responseBuilder.add("Upgrade: websocket");
        responseBuilder.add("Connection: Upgrade");
        responseBuilder.add("Sec-WebSocket-Accept: " + secWSAcceptHeader);
        responseBuilder.add("\r\n");
        // Send the handshake response
        socketChannel.write(ByteBuffer.wrap(responseBuilder.toString().getBytes()));
    }

    /**
     * Close the socket channel and cancel the selection key.
     *
     * @param socketChannel The socket channel to close.
     */
    protected static void closeSocketChannel(SocketChannel socketChannel) {
        try {
            socketChannel.socket().close();
            socketChannel.close();
        } catch (IOException ignored) {
        }
    }

    @Override
    public void dispatch(PayloadData payload, SocketChannel channel) {
        GameWebSocket gameWebSocket = new GameWebSocket(channel);
        switch (payload.opCode()) {
            case TEXT -> onMessage(gameWebSocket, RFC6455.decodeText(payload));
            case PING -> onPing(gameWebSocket);
            case PONG -> onPong(gameWebSocket);
            case CLOSE -> {
                onClose(gameWebSocket);
                closeSocketChannel(channel);
            }
        }
    }
}

