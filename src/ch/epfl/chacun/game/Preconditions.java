package ch.epfl.chacun.game;

/**
 * Utility class to check if arguments are correct.
 *
 * @author Valerio De Santis (373247)
 * @author Simon Lefort (371918)
 */
public final class Preconditions {
    private Preconditions() {
    }

    /**
     * Throws an IllegalArgumentException if the given boolean is false.
     *
     * @param shouldBeTrue the boolean condition to check
     * @throws IllegalArgumentException if the given boolean is false
     */
    public static void checkArgument(boolean shouldBeTrue) {
        if (!shouldBeTrue) throw new IllegalArgumentException();
    }

}

