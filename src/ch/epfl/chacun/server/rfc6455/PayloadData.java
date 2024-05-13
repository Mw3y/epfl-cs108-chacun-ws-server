package ch.epfl.chacun.server.rfc6455;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * Represents the payload data of a WebSocket frame.
 */
public class PayloadData {

    /**
     * The size of the mask value in bytes.
     */
    public static final int DATA_MASK_SIZE = 4;

    // Constants for bit manipulation
    private static final int LENGTH_BITS = 7;
    private static final int LENGTH_MASK = (1 << LENGTH_BITS) - 1;
    private static final int IS_MASKED_POS = 7;
    private static final int IS_MASKED_MASK = 1 << IS_MASKED_POS;
    private static final int OPCODE_BITS = 4;
    private static final int OPCODE_MASK = (1 << OPCODE_BITS) - 1;
    private static final int RSV_BITS = 3;
    private static final int FIRST_RSV_POS = 6;
    private static final int FIN_BIT_POS = 7;
    private static final int FIN_MASK = 1 << FIN_BIT_POS;

    private final ByteBuffer buffer;
    private final byte firstByte;
    private final byte secondByte;
    private final boolean isFinal;
    private final int length;
    private final boolean isMasked;
    private final OpCode opcode;
    private final int[] rsv;
    private final byte[] mask;
    private final ByteBuffer data;

    /**
     * Extracts data following RFC 6455 specs and creates a new PayloadData.
     *
     * @param payloadBuffer The buffer containing the payload data.
     */
    public PayloadData(ByteBuffer payloadBuffer) {
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
        this.buffer = payloadBuffer.duplicate().asReadOnlyBuffer();
        this.buffer.flip();
        // Read from the buffer until the data is available
        this.firstByte = buffer.get();
        this.secondByte = buffer.get();
        this.isFinal = readIsFinal();
        this.rsv = readRSVBits();
        this.opcode = readOpCode();
        this.isMasked = readIsMasked();
        this.length = readLength();
        this.mask = isMasked ? new byte[DATA_MASK_SIZE] : null;
        if (isMasked) {
            buffer.get(mask);
        }
        // The data is the remaining bytes in the buffer
        this.data = buffer.slice(buffer.position(), length);
    }

    /**
     * Decodes the payload data as text.
     *
     * @return The payload data as text.
     */
    public String decodeAsText() {
        if (isMasked) {
            byte[] dataBytes = new byte[length];
            // Apply the mask to retrieve the data
            for (int i = 0; i < dataBytes.length; ++i) {
                byte data = buffer.get();
                dataBytes[i] = (byte) (data ^ mask[i % DATA_MASK_SIZE]);
            }
            // Reset the buffer position to the first data byte
            // Allow the data to be read again
            data.position(0);
            // Convert the data to a string
            return new String(dataBytes);
        }
        return new String(data.array());
    }

    /**
     * Get the opcode of the payload data.
     *
     * @return The opcode of the payload data.
     */
    public OpCode opCode() {
        return opcode;
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
    private int readLength() {
        int length = secondByte & LENGTH_MASK;
        switch (length) {
            case 127 -> length = (int) buffer.getLong();
            case 126 -> length = Short.toUnsignedInt(buffer.getShort());
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
    private boolean readIsMasked() {
        return ((secondByte & IS_MASKED_MASK) >> IS_MASKED_POS) == 1;
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
    private int readOpCodeBits() {
        return firstByte & OPCODE_MASK;
    }

    /**
     * Get the opcode from the first byte of a WebSocket frame.
     * <p>
     * Defines the interpretation of the "Payload data".
     *
     * @return The value of the opcode bits.
     */
    private OpCode readOpCode() {
        return OpCode.fromValue(readOpCodeBits());
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
     * @return The value of the RSV bits.
     */
    private int[] readRSVBits() {
        int[] rsv = new int[RSV_BITS];
        for (int i = 0; i < rsv.length; i++) {
            int shift = FIRST_RSV_POS - i;
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
    private boolean readIsFinal() {
        return (firstByte & FIN_MASK) >> FIN_BIT_POS == 1;
    }

    /**
     * Represents the opcode of a WebSocket frame.
     */
    public enum OpCode {
        CONTINUATION(0x0),
        TEXT(0x1),
        BINARY(0x2),
        CLOSE(0x8),
        PING(0x9),
        PONG(0xA),
        // 0xFF is a dummy value to represent reserved opcodes since it doesn't exist
        RESERVED(0xFF);

        private static final List<OpCode> ALL = List.of(values());
        private final int value;

        /**
         * Create a new OpCode with the provided value.
         * @param value The value of the OpCode.
         */
        OpCode(int value) {
            this.value = value;
        }

        /**
         * Get the value of the OpCode.
         * @return The value of the OpCode.
         */
        public int value() {
            return value;
        }

        /**
         * Get the OpCode matching the provided value or RESERVED by default.
         * @param value The value of the OpCode.
         * @return The OpCode matching the provided value or RESERVED by default.
         */
        public static OpCode fromValue(int value) {
            return ALL.stream().filter(code -> code.value == value).findFirst().orElse(RESERVED);
        }

    }

}
