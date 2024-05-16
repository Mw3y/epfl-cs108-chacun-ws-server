package ch.epfl.chacun.game;

/**
 * This is a utility class containing useful methods to work with Base32
 *
 * @author Valerio De Santis (373247)
 * @author Simon Lefort (371918)
 */
public final class Base32 {
    /**
     * This is a utility class. It is not instantiable
     */
    private Base32() {
    }

    /**
     * The set of characters that compose Base32
     */
    private final static String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    /**
     * A mask to get the 5 least significant bits
     */
    private final static int MASK_5_BITS = 0b11111;
    /**
     * The number of bits encoded by a Base32-character
     */
    private final static int BASE_32_SYMBOL_BIT_SIZE = 5;

    /**
     * Checks if all the characters of the given string belong to the charset of Base32
     *
     * @param encoded the string to verify
     * @return if all the characters of the given string belong to the charset of Base32
     */
    public static boolean isValid(String encoded) {
        return encoded.chars().allMatch(c -> ALPHABET.indexOf(c) != -1);
    }

    /**
     * Returns the Base32-encoded string resulting of the 5 least significant bits of a given integer
     *
     * @param toEncode the integer whose 5 least significant bits have to be encoded
     * @return the Base32-encoded string resulting of the 5 least significant bits of a given integer
     */
    public static String encodeBits5(int toEncode) {
        return String.valueOf(ALPHABET.charAt(toEncode & MASK_5_BITS));
    }

    /**
     * Returns the Base32-encoded string resulting of the 10 least significant bits of a given integer
     *
     * @param toEncode the integer whose 10 least significant bits have to be encoded
     * @return the Base32-encoded string resulting of the 10 least significant bits of a given integer
     */
    public static String encodeBits10(int toEncode) {
        return encodeBits5(toEncode >> BASE_32_SYMBOL_BIT_SIZE) + encodeBits5(toEncode);
    }

    /**
     * Returns the number corresponding to the given Base32-encoded message
     *
     * @param encoded the Base32-encoded message
     * @return the number corresponding to the given Base32-encoded message
     */
    public static int decode(String encoded) {
        Preconditions.checkArgument(isValid(encoded));
        int decoded = 0;
        for (int i = 0; i < encoded.length(); i++) {
            decoded = decoded << BASE_32_SYMBOL_BIT_SIZE | ALPHABET.indexOf(encoded.charAt(i));
        }
        return decoded;
    }

}
