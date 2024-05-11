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
        String string = secWebSocketKey + GUID;
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

                        String string = new String(buffer.array());
                        if (string.startsWith("GET / HTTP/1.1")) {
                            Pattern secWSKeyPattern = Pattern.compile("Sec-WebSocket-Key:\\s*(.*?)\r\n");
                            Matcher matcher = secWSKeyPattern.matcher(string);

                            // If the Sec-WebSocket-Key header is not present, close the connection
                            if (!matcher.find()) {
                                closeSocketChannel(socketChannel, key);
                                continue;
                            }

                            // Generate the Sec-WebSocket-Accept header value
                            String secWebSocketKey = matcher.group(1);
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
                        } else {
                            if (buffer.limit() <= 0) continue;

                            byte firstByte = buffer.get();
                            byte secondByte = buffer.get();

                            /*
                             * FIN:  1 bit
                             *
                             * Indicates that this is the final fragment in a message.
                             * The first fragment MAY also be the final fragment.
                             */
                            byte fin = (byte) ((firstByte & 128) >> 7);

                            /*
                             * RSV1, RSV2, RSV3:  1 bit each
                             *
                             * MUST be 0 unless an extension is negotiated that defines meanings for non-zero values.
                             * If a nonzero value is received and none of the negotiated extensions defines the meaning
                             * of such a nonzero value, the receiving endpoint MUST Fail the WebSocket Connection.
                             */
                            byte rsv1 = (byte) ((firstByte & 64) >> 6);
                            byte rsv2 = (byte) ((firstByte & 32) >> 5);
                            byte rsv3 = (byte) ((firstByte & 16) >> 4);

                            /*
                             * Opcode: 4 bits
                             *
                             * Defines the interpretation of the "Payload data".
                             * If an unknown opcode is received, the receiving endpoint MUST Fail the
                             * WebSocket Connection. The following values are defined.
                             *
                             * %x0 denotes a continuation frame
                             * %x1 denotes a text frame
                             * %x2 denotes a binary frame
                             * %x3-7 are reserved for further non-control frames
                             * %x8 denotes a connection close
                             * %x9 denotes a ping
                             * %xA denotes a pong
                             * %xB-F are reserved for further control frames
                             */
                            byte opCode = (byte) ((firstByte & 8) | (firstByte & 4) | (firstByte & 2) | (firstByte & 1));

                            /*
                             * Mask:  1 bit
                             *
                             * Defines whether the "Payload data" is masked.  If set to 1, a
                             * masking key is present in masking-key, and this is used to unmask
                             * the "Payload data" as per Section 5.3.  All frames sent from
                             * client to server have this bit set to 1.
                             */
                            byte mask = (byte) ((secondByte & 128) >> 7);

                            /*
                             * Payload length:  7 bits, 7+16 bits, or 7+64 bits
                             *
                             * The length of the "Payload data", in bytes: if 0-125, that is the
                             * payload length.
                             *
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
                             */
                            int length = secondByte & 0b01111111;
                            switch (length) {
                                case 127 -> length = (int) buffer.getLong();
                                case 126 -> length = buffer.getShort();
                            }

                            // If the mask bit is not set, close the connection
                            if (mask != 1) {
                                closeSocketChannel(socketChannel, key);
                                continue;
                            }

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

    private static void closeSocketChannel(SocketChannel socketChannel, SelectionKey key) throws IOException {
        socketChannel.socket().close();
        socketChannel.close();
        key.cancel();
    }
}

