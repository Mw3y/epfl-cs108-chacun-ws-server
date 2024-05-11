package ch.epfl.chacun.server;

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

public class WebSocketServer {

    /**
     * The port number on which the server listens.
     */
    public static final int PORT = 3000;

    /**
     * Globally Unique Identifier (GUID, [RFC4122]) in string form.
     */
    private static final String GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

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

    public static void main(String[] args) {
        try (ServerSocketChannel serverSocketChannel = ServerSocketChannel.open()) {
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.bind(new InetSocketAddress(PORT));

            Selector selector = Selector.open();
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
            while (true) {
                selector.select(1);
                Iterator<SelectionKey> it = selector.selectedKeys().iterator();
                while (it.hasNext()) {
                    SelectionKey key = it.next();
                    if (key.isAcceptable()) {
                        SocketChannel channel = serverSocketChannel.accept();
                        if (channel != null) {
                            channel.configureBlocking(false);
                            channel.register(selector, SelectionKey.OP_READ);
                        }
                    }
                    if (key.isReadable()) {
                        SocketChannel socketChannel = (SocketChannel) key.channel();
                        ByteBuffer buffer = ByteBuffer.allocate(256);
                        socketChannel.read(buffer);
                        buffer.flip();

                        String content = new String(buffer.array());
                        if (content.startsWith("GET / HTTP/1.1")) {
                            openingHandshake(content, socketChannel, key);
                        } else {
                            if (buffer.limit() <= 0) continue;

                            /*
                             *  0                   1                   2                   3
                             *  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
                             * +-+-+-+-+-------+-+-------------+-------------------------------+
                             * |F|R|R|R| opcode|M| Payload len |    Extended payload length    |
                             * |I|S|S|S|  (4)  |A|     (7)     |             (16/64)           |
                             * |N|V|V|V|       |S|             |   (if payload len==126/127)   |
                             * | |1|2|3|       |K|             |                               |
                             * +-+-+-+-+-------+-+-------------+ - - - - - - - - - - - - - - - +
                             * |     Extended payload length continued, if payload len == 127  |
                             * + - - - - - - - - - - - - - - - +-------------------------------+
                             * |                               |Masking-key, if MASK set to 1  |
                             * +-------------------------------+-------------------------------+
                             * | Masking-key (continued)       |          Payload Data         |
                             * +-------------------------------- - - - - - - - - - - - - - - - +
                             * :                     Payload Data continued ...                :
                             * + - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - +
                             * |                     Payload Data continued ...                |
                             * +---------------------------------------------------------------+
                             */
                            byte firstByte = buffer.get();
                            byte secondByte = buffer.get();

                            System.out.println("opcode: " + getOpCodeBits(firstByte));
                            int opCode = getOpCodeBits(firstByte);

                            // If the mask bit is not set, close the connection
                            if (!isMasked(secondByte)) {
                                closeSocketChannel(socketChannel, key);
                                continue;
                            }

                            int length = getLength(secondByte, buffer);
                            byte[] dataBytes = new byte[length];
                            byte[] maskValue = new byte[4];
                            buffer.get(maskValue);
                            // Apply the mask to the data
                            for (int i = 0; i < length; ++i) {
                                byte data = buffer.get();
                                dataBytes[i] = (byte) (data ^ maskValue[i % 4]);
                            }
                            System.out.println(new String(dataBytes));
                        }

                    }
                    it.remove();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get the length of the payload data from the second byte of a WebSocket frame.
     * <p>
     * Payload length:  7 bits, 7+16 bits, or 7+64 bits
     * <p>
     * The length of the "Payload data", in bytes: if 0-125, that is the
     * payload length.
     * <p>
     * If 126, the following 2 bytes interpreted as a
     * 16-bit unsigned integer are the payload length.  If 127, the
     * following 8 bytes interpreted as a 64-bit unsigned integer (the
     * most significant bit MUST be 0) are the payload length.  Multibyte
     * length quantities are expressed in network byte order.  Note that
     * in all cases, the minimal number of bytes MUST be used to encode
     * the length, for example, the length of a 124-byte-long string
     * can't be encoded as the sequence 126, 0, 124.  The payload length
     * is the length of the "Extension data" + the length of the
     * "Application data".  The length of the "Extension data" may be
     * zero, in which case the payload length is the length of the
     * "Application data".
     *
     * @param secondByte The second byte of a WebSocket frame.
     * @param buffer     The buffer containing the WebSocket frame.
     * @return The length of the payload data.
     */
    private static int getLength(byte secondByte, ByteBuffer buffer) {
        int LENGTH_MASK = (1 << 7) - 1;
        int length = secondByte & LENGTH_MASK;
        switch (length) {
            case 127 -> length = (int) buffer.getLong();
            case 126 -> length = buffer.getShort();
        }
        return length;
    }

    /**
     * Whether the mask bit from the second byte of a WebSocket frame is set.
     * <p>
     * Mask:  1 bit
     * <p>
     * Defines whether the "Payload data" is masked.
     * If set to 1, a masking key is present in masking-key, and this
     * is used to unmask the "Payload data" as per Section 5.3.
     *
     * @param secondByte The second byte of a WebSocket frame.
     * @return The value of the mask bit.
     */
    private static boolean isMasked(byte secondByte) {
        int MASK_MASK = 1 << (Byte.SIZE - 1);
        return ((secondByte & MASK_MASK) >> (Byte.SIZE - 1)) == 1;
    }


    /**
     * Get the opcode bits from the first byte of a WebSocket frame.
     * <p>
     * Opcode: 4 bits
     * <p>
     * Defines the interpretation of the "Payload data".
     * If an unknown opcode is received, the receiving endpoint MUST Fail the
     * WebSocket Connection. The following values are defined.
     * <p>
     * %x0 denotes a continuation frame<br>
     * %x1 denotes a text frame<br>
     * %x2 denotes a binary frame<br>
     * %x3-7 are reserved for further non-control frames<br>
     * %x8 denotes a connection close<br>
     * %x9 denotes a ping<br>
     * %xA denotes a pong<br>
     * %xB-F are reserved for further control frames<br>
     *
     * @param firstByte The first byte of a WebSocket frame.
     * @return The value of the opcode bits.
     */
    private static int getOpCodeBits(byte firstByte) {
        int OPCODE_MASK = (1 << 4) - 1;
        return firstByte & OPCODE_MASK;
    }

    /**
     * Get the opcode from the first byte of a WebSocket frame.
     * <p>
     * Defines the interpretation of the "Payload data".
     *
     * @param firstByte The first byte of a WebSocket frame.
     * @return The value of the opcode bits.
     */
    private static OpCode getOpCode(byte firstByte) {
        return OpCode.fromValue(getOpCodeBits(firstByte));
    }

    /**
     * Get the RSV bits from the first byte of a WebSocket frame.
     * <p>
     * RSV1, RSV2, RSV3:  1 bit each
     * <p>
     * MUST be 0 unless an extension is negotiated that defines meanings for non-zero values.
     * If a nonzero value is received and none of the negotiated extensions defines the meaning
     * of such a nonzero value, the receiving endpoint MUST Fail the WebSocket Connection.
     *
     * @param firstByte The first byte of a WebSocket frame.
     */
    private static int[] getRSVBits(byte firstByte) {
        int[] rsv = new int[3];
        for (int i = 0; i < 3; i++) {
            int shift = Byte.SIZE - 2 - i;
            int mask = 1 << shift;
            rsv[i] = (firstByte & mask) >> shift;
        }
        return rsv;
    }

    /**
     * Get the FIN bit from the first byte of a WebSocket frame.
     * <p>
     * FIN:  1 bit
     * Indicates that this is the final fragment in a message.
     * The first fragment MAY also be the final fragment.
     *
     * @param firstByte The first byte of a WebSocket frame.
     * @return The value of the FIN bit.
     */
    public static int getFinBit(byte firstByte) {
        int FIN_MASK = 1 << (Byte.SIZE - 1);
        return (firstByte & FIN_MASK) >> (Byte.SIZE - 1);
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
     * @param key           The selection key of the socket channel.
     * @throws IOException If the handshake response could not be written to the socket channel.
     */
    private static void openingHandshake(String content, SocketChannel socketChannel, SelectionKey key) throws IOException {
        Pattern secWSKeyPattern = Pattern.compile("Sec-WebSocket-Key:\\s*(.*?)\r\n");
        Matcher secWSKeyMatcher = secWSKeyPattern.matcher(content);

        // If the Sec-WebSocket-Key header is not present, close the connection
        if (!secWSKeyMatcher.find()) {
            closeSocketChannel(socketChannel, key);
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
     * @param key           The selection key to cancel.
     * @throws IOException If the socket channel could not be closed.
     */
    private static void closeSocketChannel(SocketChannel socketChannel, SelectionKey key) throws IOException {
        socketChannel.socket().close();
        socketChannel.close();
        key.cancel();
    }

    private enum OpCode {
        CONTINUATION,
        TEXT,
        BINARY,
        CLOSE,
        PING,
        PONG,
        RESERVED;

        public static OpCode fromValue(int value) {
            return switch (value) {
                case 0x0 -> CONTINUATION;
                case 0x1 -> TEXT;
                case 0x2 -> BINARY;
                case 0x8 -> CLOSE;
                case 0x9 -> PING;
                case 0xA -> PONG;
                default -> RESERVED;
            };
        }

    }
}

