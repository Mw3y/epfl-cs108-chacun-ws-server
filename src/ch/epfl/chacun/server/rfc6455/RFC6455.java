package ch.epfl.chacun.server.rfc6455;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

public class RFC6455 {

    /**
     * The size of the mask value in bytes.
     */
    public static final int DATA_MASK_SIZE = 4;

    // Constants for bit manipulation
    public static final int LENGTH_BITS = 7;
    public static final int LENGTH_MASK = (1 << LENGTH_BITS) - 1;
    public static final int IS_MASKED_POS = 7;
    public static final int IS_MASKED_MASK = 1 << IS_MASKED_POS;
    public static final int OPCODE_BITS = 4;
    public static final int OPCODE_MASK = (1 << OPCODE_BITS) - 1;
    public static final int RSV_BITS = 3;
    public static final int FIRST_RSV_POS = 6;
    public static final int FIN_BIT_POS = 7;
    public static final int FIN_MASK = 1 << FIN_BIT_POS;

    /**
     * PING control frame.
     */
    public static final ByteBuffer PING = encodeControlFrame(OpCode.PING);

    /**
     * PONG control frame.
     */
    public static final ByteBuffer PONG = encodeControlFrame(OpCode.PONG);

    /**
     * Encodes a control frame with the provided opcode.
     * @param opCode The opcode of the control frame.
     * @return The ByteBuffer containing the control frame.
     */
    private static ByteBuffer encodeControlFrame(OpCode opCode) {
        ByteBuffer buffer = ByteBuffer.allocate(2);
        buffer.put((byte) (1 << FIN_BIT_POS | opCode.value()));
        buffer.put((byte) 0); // No mask and no payload data length
        buffer.flip();
        return buffer.asReadOnlyBuffer();
    }

    /**
     * Decodes a WebSocket frame into a string.
     * @param payloadData The payload data of the WebSocket frame.
     * @return The decoded string.
     */
    public static String decodeText(PayloadData payloadData) {
        ByteBuffer data = payloadData.data();
        if (payloadData.isMasked()) {
            byte[] dataBytes = new byte[payloadData.length()];
            // Apply the mask to retrieve the data
            byte[] mask = payloadData.mask();
            for (int i = 0; i < dataBytes.length; ++i) {
                dataBytes[i] = (byte) (data.get() ^ mask[i % DATA_MASK_SIZE]);
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
     * Extracts the data of a WebSocket frame.
     * @param payload The payload data of the WebSocket frame.
     * @return The parsed payload data.
     */
    public static PayloadData parsePayload(ByteBuffer payload) {
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
        ByteBuffer buffer = payload.duplicate().asReadOnlyBuffer();
        buffer.flip();
        // Read from the buffer until the data is available
        boolean isFinal = readIsFinal(buffer);
        int[] rsv = readRSVBits(buffer);
        OpCode opcode = readOpCode(buffer);
        boolean isMasked = readIsMasked(buffer);
        // Read length and keep track of the buffer position
        // The new buffer position will be the start of the data mask
        int length = readLength(buffer, false);
        byte[] mask = isMasked ? new byte[DATA_MASK_SIZE] : null;
        if (isMasked) {
            buffer.get(mask);
        }
        // The data is the remaining bytes in the buffer
        ByteBuffer data = buffer.slice(buffer.position(), length);
        return new PayloadData(buffer, isFinal, rsv, opcode, isMasked, length, mask, data);
    }


    /**
     * Encodes a message into a WebSocket frame.
     * @param message The message to encode.
     * @return The ByteBuffer containing the WebSocket frame.
     */
    public static ByteBuffer encodeText(String message) {
        ByteBuffer buffer = ByteBuffer.allocate(4096);
        // Set the FIN bit to 1 and the opcode to 0x1 (TEXT)
        byte firstByte = (byte) (1 << FIN_BIT_POS | OpCode.TEXT.value());
        buffer.put(firstByte);
        // Set the MASK bit to 0 and the payload length
        long unsignedLength = Integer.toUnsignedLong(message.length());
        buffer.put(encodeLength(unsignedLength));
        // Add the payload data
        buffer.put(message.getBytes());
        buffer.flip();
        return buffer.asReadOnlyBuffer();
    }

    /**
     * Encodes the length of the payload data.
     * @param length The length of the payload data.
     * @return The byte array containing the length data.
     */
    private static byte[] encodeLength(long length) {
        if (length <= Byte.MAX_VALUE - 2)
            return new byte[]{(byte) length};

        ByteBuffer buffer;
        if (length <= Short.MAX_VALUE) {
            buffer = ByteBuffer.allocate(Short.BYTES + 1);
            buffer.put((byte) 126);
            buffer.putShort((short) length);
        } else {
            buffer = ByteBuffer.allocate(Long.BYTES + 1);
            buffer.put((byte) 127);
            buffer.putLong(length);
        }

        return buffer.array();
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
     * @param buffer The buffer containing the WebSocket frame.
     * @return The value of the opcode bits.
     */
    private static int readOpCodeBits(ByteBuffer buffer) {
        return buffer.get(0) & OPCODE_MASK;
    }

    /**
     * Get the opcode from the first byte of a WebSocket frame.
     * <p>
     * Defines the interpretation of the "Payload data".
     *
     * @param buffer The buffer containing the WebSocket frame.
     * @return The value of the opcode bits.
     */
    public static OpCode readOpCode(ByteBuffer buffer) {
        return OpCode.fromValue(readOpCodeBits(buffer));
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
     * @param buffer The buffer containing the WebSocket frame.
     * @return The value of the RSV bits.
     */
    public static int[] readRSVBits(ByteBuffer buffer) {
        int[] rsv = new int[RSV_BITS];
        int firstByte = buffer.get(0);
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
     * @param buffer The buffer containing the WebSocket frame.
     * @return The value of the FIN bit.
     */
    public static boolean readIsFinal(ByteBuffer buffer) {
        return (buffer.get(0) & FIN_MASK) >> FIN_BIT_POS == 1;
    }

    /**
     * Get the length of the payload data from the second byte of a WebSocket frame.
     * @param buffer The buffer containing the WebSocket frame.
     * @param immutableOperation Whether the buffer position should change or not.
     * @return The length of the payload data.
     */
    private static int readLength(ByteBuffer buffer, boolean immutableOperation) {
        int length = buffer.get(1) & LENGTH_MASK;
        int bufferPos = buffer.position();
        buffer.position(2);
        switch (length) {
            case 127 -> length = (int) buffer.getLong();
            case 126 -> length = Short.toUnsignedInt(buffer.getShort());
        }
        if (immutableOperation)
            buffer.position(bufferPos);
        return length;
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
     * @param buffer The buffer containing the WebSocket frame.
     * @return The length of the payload data.
     */
    public int readLength(ByteBuffer buffer) {
        return readLength(buffer, true);
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
     * @param buffer The buffer containing the WebSocket frame.
     * @return The value of the mask bit.
     */
    public static boolean readIsMasked(ByteBuffer buffer) {
        return ((buffer.get(1) & IS_MASKED_MASK) >> IS_MASKED_POS) == 1;
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
         * Get the OpCode matching the provided value or RESERVED by default.
         * @param value The value of the OpCode.
         * @return The OpCode matching the provided value or RESERVED by default.
         */
        public static OpCode fromValue(int value) {
            return ALL.stream().filter(code -> code.value == value).findFirst().orElse(RESERVED);
        }

        /**
         * Get the value of the OpCode.
         * @return The value of the OpCode.
         */
        public int value() {
            return value;
        }

    }
}
