package ch.epfl.chacun.server.rfc6455;

import java.nio.ByteBuffer;

public final class TextEncoder {

    /**
     * Encodes a message into a WebSocket frame.
     * @param message The message to encode.
     * @return The ByteBuffer containing the WebSocket frame.
     */
    public static ByteBuffer encodeToPayload(String message) {
        ByteBuffer buffer = ByteBuffer.allocate(4096);
        // Set the FIN bit to 1 and the opcode to 0x1 (TEXT)
        byte firstByte = (byte) (1 << 7 | PayloadData.OpCode.TEXT.value());
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
}
