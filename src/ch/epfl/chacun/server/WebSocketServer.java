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

public class WebSocketServer {

    private static final String MAGIC_NUMBER = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

    private static String encodeSha1AndBase64(String secWebSocketKey) {
        String string = secWebSocketKey + MAGIC_NUMBER;
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
            serverSocketChannel.bind(new InetSocketAddress(3000));

            Selector selector = Selector.open();
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
            while (true) {
                selector.select(1);
                for (Iterator<SelectionKey> it = selector.selectedKeys().iterator(); it.hasNext(); ) {
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
                        ByteBuffer buffer = ByteBuffer.allocate(2048);
                        socketChannel.read(buffer);
                        buffer.flip();

                        // String byteString = toByteString(buffer);
                        String string = new String(buffer.array());
                        if (string.startsWith("GET / HTTP/1.1")) {
                            int swkIndex = string.indexOf("Sec-WebSocket-Key:");
                            int endIndex = string.indexOf("\r\n", swkIndex);
                            String swk = string.substring(swkIndex + 19, endIndex);

                            String s = encodeSha1AndBase64(swk);

                            String response = "HTTP/1.1 101 Switching Protocols\r\n" + "Upgrade: websocket\r\n" + "Connection: Upgrade\r\n" + "Sec-WebSocket-Accept: " + s + "\r\n\r\n";
                            socketChannel.write(ByteBuffer.wrap(response.getBytes()));
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
                            long length = secondByte & 0b01111111;
                            switch ((int) length) {
                                case 126 -> length = buffer.getShort();
                                case 127 -> length = buffer.getLong();
                            }

                            byte[] dataBytes = new byte[(int) length];

                            byte[] maskValue = null;
                            if (mask == 1) {
                                // Unmask the data
                                maskValue = new byte[4];
                                buffer.get(maskValue);
                                for (int i = 0; i < length; i++) {
                                    byte data = buffer.get();
                                    dataBytes[i] = (byte) (data ^ maskValue[i % 4]);
                                }
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
}

