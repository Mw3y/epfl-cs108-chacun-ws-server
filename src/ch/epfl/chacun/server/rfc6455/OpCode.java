package ch.epfl.chacun.server.rfc6455;

import java.util.List;

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
    public int asNumber() {
        return value;
    }

}
