package ch.epfl.chacun.game;

/**
 * Represents a position in the game.
 *
 * @param x the x coordinate
 * @param y the y coordinate
 * @author Valerio De Santis (373247)
 * @author Simon Lefort (371918)
 */
public record Pos(int x, int y) {

    /**
     * The origin of the game's board.
     */
    public final static Pos ORIGIN = new Pos(0, 0);

    /**
     * Returns the position obtained by translating this one by the given amount.
     *
     * @param dX the amount to translate the x coordinate
     * @param dY the amount to translate the y coordinate
     * @return the position obtained by translating this one by the given amount
     */
    public Pos translated(int dX, int dY) {
        return new Pos(this.x + dX, this.y + dY);
    }

    /**
     * Returns the next position obtained by moving in the specified direction.
     *
     * @param direction the direction to move to
     * @return the next position obtained by moving in the specified direction
     */
    public Pos neighbor(Direction direction) {
        return switch (direction) {
            case N -> translated(0, -1);
            case E -> translated(1, 0);
            case S -> translated(0, 1);
            case W -> translated(-1, 0);
        };
    }

}
