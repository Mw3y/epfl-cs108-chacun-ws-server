package ch.epfl.chacun.server.rfc6455;

import java.util.List;

/**
 * Represents the opcode of a WebSocket frame.
 */
public enum OpCode {
    /**
     * Indicates that the frame is a continuation of a previous frame.
     */
    CONTINUATION(0x0),

    /**
     * Indicates that the frame contains a text message.
     */
    TEXT(0x1),

    /**
     * Indicates that the frame contains binary data.
     */
    BINARY(0x2),

    /**
     * Indicates that the frame is a connection close request.
     */
    CLOSE(0x8),

    /**
     * Indicates that the frame is a ping request.
     */
    PING(0x9),
    /**
     * Indicates that the frame is a pong response.
     */
    PONG(0xA);

    /**
     * The list of all OpCode values.
     */
    public static final List<OpCode> ALL = List.of(values());

    /**
     * The value of the OpCode.
     */
    private final int code;

    /**
     * Create a new OpCode with the provided value.
     * @param code The value of the OpCode.
     */
    OpCode(int code) {
        this.code = code;
    }

    /**
     * Get the OpCode matching the provided value or RESERVED by default.
     * @param value The value of the OpCode.
     * @return The OpCode matching the provided value or RESERVED by default.
     */
    public static OpCode fromValue(int value) {
        return ALL.stream().filter(op -> op.code == value).findFirst().orElseThrow();
    }

    /**
     * Get the value of the OpCode.
     * @return The value of the OpCode.
     */
    public int asNumber() {
        return code;
    }

}
