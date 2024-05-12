package ch.epfl.chacun.server;

import java.nio.ByteBuffer;

/**
 * Represents a RFC 6455 WebSocket data frame.
 * <p>
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
public final class WebSocketData {

    /**
     * The ByteBuffer containing the WebSocket frame data.
     */
    private final ByteBuffer buffer;

    /**
     * The first and second byte of a WebSocket frame.
     */
    private final byte firstByte, secondByte;

    /**
     * The index of the first data byte in a WebSocket frame.
     */
    public static final int FIRST_DATA_BYTE_INDEX = 2;

    /**
     * The size of the mask value in bytes.
     */
    public static final int DATA_MASK_SIZE = 4;

    /**
     * Constructs a WebSocketData object from a ByteBuffer.
     * @param buffer The ByteBuffer containing the WebSocket frame data.
     */
    public WebSocketData(ByteBuffer buffer) {
        // Duplicate the buffer to prevent modifying the original buffer
        this.buffer = buffer.duplicate();
        // Flip the buffer to read from the beginning
        this.buffer.flip();
        // Read the first and second byte of the WebSocket frame which contains utility data
        firstByte = this.buffer.get();
        secondByte = this.buffer.get();
    }

    /**
     * Returns a copy of the ByteBuffer containing the WebSocket frame data.
     * @return A copy of the ByteBuffer containing the WebSocket frame data.
     */
    public ByteBuffer buffer() {
        return buffer.duplicate();
    }

    /**
     * Decodes the TEXT payload data from a WebSocket frame.
     * @return The payload data as a string.
     */
    public String decodeText() {
        if (getOpCode() != OpCode.TEXT) {
            throw new IllegalStateException("Not a TEXT frame");
        }

        if (!isMasked()) {
            throw new IllegalStateException("Mask bit not set");
        }

        if (buffer.limit()  <= 0) {
            return "";
        }

        byte[] dataBytes = new byte[getLength()];
        byte[] maskValue = new byte[DATA_MASK_SIZE];
        buffer.get(maskValue);
        // Apply the mask to retrieve the data
        for (int i = 0; i < dataBytes.length; ++i) {
            byte data = buffer.get();
            dataBytes[i] = (byte) (data ^ maskValue[i % DATA_MASK_SIZE]);
        }
        // Reset the buffer position to the first data byte
        // Allow the data to be read again
        buffer.position(FIRST_DATA_BYTE_INDEX);
        // Convert the data to a string
        return new String(dataBytes);
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
     * @return The length of the payload data.
     */
    public int getLength() {
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
     * @return The value of the mask bit.
     */
    public boolean isMasked() {
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
     * @return The value of the opcode bits.
     */
    public int getOpCodeBits() {
        int OPCODE_MASK = (1 << 4) - 1;
        return firstByte & OPCODE_MASK;
    }

    /**
     * Get the opcode from the first byte of a WebSocket frame.
     * <p>
     * Defines the interpretation of the "Payload data".
     *
     * @return The value of the opcode bits.
     */
    public OpCode getOpCode() {
        return OpCode.fromValue(getOpCodeBits());
    }

    /**
     * Get the RSV bits from the first byte of a WebSocket frame.
     * <p>
     * RSV1, RSV2, RSV3:  1 bit each
     * <p>
     * MUST be 0 unless an extension is negotiated that defines meanings for non-zero values.
     * If a nonzero value is received and none of the negotiated extensions defines the meaning
     * of such a nonzero value, the receiving endpoint MUST Fail the WebSocket Connection.
     * @return The value of the RSV bits.
     */
    public int[] getRSVBits() {
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
     * @return The value of the FIN bit.
     */
    public int getFinBit() {
        int FIN_MASK = 1 << (Byte.SIZE - 1);
        return (firstByte & FIN_MASK) >> (Byte.SIZE - 1);
    }

    /**
     * Represents the opcode of a WebSocket frame.
     */
    public enum OpCode {
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
