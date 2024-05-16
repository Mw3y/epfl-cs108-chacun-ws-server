package ch.epfl.chacun.game;

import java.util.List;

/**
 * Represents the four cardinal directions
 *
 * @author Valerio De Santis (373247)
 * @author Simon Lefort (371918)
 */
public enum Direction {

    N,
    E,
    S,
    W;

    /**
     * A list containing the four cardinal directions
     */
    public static final List<Direction> ALL = List.of(Direction.values());

    /**
     * The number of cardinal directions
     */
    public static final int COUNT = ALL.size();


    /**
     * Returns the direction obtained by rotating this direction
     *
     * @param rotation the rotation to apply
     * @return the direction obtained by rotating this direction by the given rotation
     */
    public final Direction rotated(Rotation rotation) {
        int withQuarterTurns = this.ordinal() + rotation.quarterTurnsCW();
        return ALL.get(withQuarterTurns % COUNT);
    }


    /**
     * Returns the direction opposite to this direction
     *
     * @return the direction opposite to this direction
     */
    public final Direction opposite() {
        return this.rotated(Rotation.HALF_TURN);
    }

}
