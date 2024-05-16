package ch.epfl.chacun.game;

import java.util.List;
import java.util.Objects;


/**
 * Represents an occupant in the game
 * An occupant is defined by its kind and the id of the zone where it is
 *
 * @param kind   non null
 * @param zoneId non negative
 * @author Valerio De Santis (373247)
 * @author Simon Lefort (371918)
 */
public record Occupant(Kind kind, int zoneId) {

    /**
     * Defines the number of pawns and huts
     * a player has during the game
     */
    private static final int PAWN_COUNT = 5;
    private static final int HUT_COUNT = 3;

    /**
     * Constructor for Occupant
     *
     * @param kind   non null
     * @param zoneId non negative
     * @throws NullPointerException     if kind is null
     * @throws IllegalArgumentException if zoneId is negative
     */
    public Occupant {
        Objects.requireNonNull(kind);
        Preconditions.checkArgument(zoneId >= 0);
    }

    /**
     * Returns the number of occupants of the given kind
     *
     * @param kind non-null, the kind of occupant
     * @return the number of occupants of the given kind
     */
    public static int occupantsCount(Kind kind) {
        return switch (kind) {
            case Kind.PAWN -> PAWN_COUNT;
            case Kind.HUT -> HUT_COUNT;
        };
    }

    /**
     * Represents the different kinds of occupant
     *
     * @author Valerio De Santis (373247)
     * @author Simon Lefort (371918)
     */
    public enum Kind {
        PAWN, HUT;

        /**
         * A list containing all the possible kinds
         */
        public static final List<Kind> ALL = List.of(Kind.values());
    }

}
