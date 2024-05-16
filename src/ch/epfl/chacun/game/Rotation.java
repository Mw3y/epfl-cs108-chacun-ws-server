package ch.epfl.chacun.game;

import java.util.List;

/**
 * Represents the rotation of a tile.
 *
 * @author Valerio De Santis (373247)
 * @author Simon Lefort (371918)
 */
public enum Rotation {
    NONE,
    RIGHT,
    HALF_TURN,
    LEFT;

    /**
     * A list containing all the possible rotations
     */
    public static final List<Rotation> ALL = List.of(Rotation.values());
    /**
     * The number of possible rotations
     */
    public static final int COUNT = ALL.size();
    /**
     * The number of degrees in a quarter turn
     */
    private static final int QUARTER_TURN_DEGREES = 90;

    /**
     * Returns the number of degrees of the rotation.
     *
     * @return the number of degrees of the rotation
     */
    public int degreesCW() {
        return this.quarterTurnsCW() * QUARTER_TURN_DEGREES;
    }

    /**
     * Returns the rotation obtained by adding the given rotation to this one.
     *
     * @param that the rotation to add
     * @return the rotation obtained by adding the given rotation to this one
     */
    public Rotation add(Rotation that) {
        int sum = this.ordinal() + that.ordinal();
        return ALL.get(sum % COUNT);
    }

    /**
     * Returns the rotation obtained by negating this one.
     *
     * @return the rotation obtained by negating this one
     */
    public Rotation negated() {
        //The mod 4 operation is used to ensure that this.ordinal being 0
        //works as expected
        int negated = COUNT - this.ordinal();
        return ALL.get(negated % ALL.size());
    }

    /**
     * Returns the number of quarter turns of the rotation.
     *
     * @return the number of quarter turns of the rotation
     */
    public int quarterTurnsCW() {
        return this.ordinal();
    }

}
