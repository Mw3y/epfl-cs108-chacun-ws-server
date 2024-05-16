package ch.epfl.chacun.game;

import java.util.Comparator;
import java.util.List;

/**
 * A utility class that offers methods
 * to encode and decode actions in Base32 for the game of ChaCuN
 *
 * @author Valerio De Santis (373247)
 * @author Simon Lefort (371918)
 */
public final class ActionEncoder {
    /**
     * An exception to be thrown when an action is invalid for the given game state.
     *
     * @author Valerio De Santis (373247)
     * @author Simon Lefort (371918)
     */
    public static class IllegalActionException extends Exception {
        /**
         * Construct an IllegalActionException, with no message.
         */
        public IllegalActionException() {
        }
    }

    /**
     * The Base32-encoded action signaling that no occupant is to be placed.
     */
    private static final int WITH_NO_OCCUPANT = 0b11111;
    // format: K-O-O-O
    /**
     * The length of a Base32-encoded action where an occupant is placed.
     */
    private static final int WITH_NEW_OCCUPANT_ACTION_LENGTH = 1;
    /**
     * The number of bits used to encode the id of the zone where the occupant is placed, used to shift its kind.
     */
    private static final int WITH_NEW_OCCUPANT_KIND_SHIFT = 4;
    /**
     * The mask to extract the zone id from the encoded action where an occupant is placed.
     */
    private static final int WITH_NEW_OCCUPANT_ZONE_MASK = (1 << WITH_NEW_OCCUPANT_KIND_SHIFT) - 1;
    // format: P-P-P-P-P-P-P-P-R-R
    /**
     * The length of a Base32-encoded action where a tile is placed.
     */
    private static final int WITH_PLACED_TILE_ACTION_LENGTH = 2;
    /**
     * The number of bits of the rotation to encode, used to shift the index in the fringe of the tile to place.
     */
    private static final int WITH_PLACED_TILE_IDX_SHIFT = 2;
    /**
     * The mask to extract the rotation from the encoded action where a tile is placed.
     */
    private static final int WITH_PLACED_TILE_ROTATION_MASK = (1 << WITH_PLACED_TILE_ACTION_LENGTH) - 1;
    // format: O-O-O-O
    /**
     * The length of a Base32-encoded action where an occupant is removed.
     */
    private static final int WITH_OCCUPANT_REMOVED_ACTION_LENGTH = 1;

    /**
     * This class can not be instantiated.
     */
    private ActionEncoder() {
    }

    /**
     * Get the fringe indexes of the given game state's board, in a list sorted by x and then y.
     *
     * @param gameState the game state to get the fringe indexes from
     * @return the list of ordered (x-precedence) fringe indexes of the game state
     */
    private static List<Pos> fringeIndexes(GameState gameState) {
        Board board = gameState.board();
        return board.insertionPositions().stream()
                .sorted(Comparator.comparing(Pos::x).thenComparing(Pos::y))
                .toList();
    }

    /**
     * Encode an action where the given tile is placed on the board of the given game state.
     *
     * @param gameState the initial game state
     * @param tile      the tile to place
     * @return the new game state resulting from the action and the encoded action
     */
    public static StateAction withPlacedTile(GameState gameState, PlacedTile tile) {
        List<Pos> fringeIndexes = fringeIndexes(gameState);
        int indexToEncode = fringeIndexes.indexOf(tile.pos()); // a number between 0 and 189
        int rotationToEncode = tile.rotation().ordinal(); // a number between 0 and 3
        int toEncode = indexToEncode << WITH_PLACED_TILE_IDX_SHIFT | rotationToEncode;
        // our action will have 7 bits at most
        return new StateAction(gameState.withPlacedTile(tile), Base32.encodeBits10(toEncode));
    }

    /**
     * Encode an action where the given occupant is placed on the board of the given game state.
     *
     * @param gameState the initial game state
     * @param occupant  the occupant to place
     * @return the new game state resulting from the action and the encoded action
     */
    public static StateAction withNewOccupant(GameState gameState, Occupant occupant) {
        // if the occupant is null, we encode 11111, which means that there is no occupant to place
        if (occupant == null)
            return new StateAction(gameState.withNewOccupant(null), Base32.encodeBits5(WITH_NO_OCCUPANT));
        int kindToEncode = occupant.kind().ordinal(); // a number between 0 and 1
        int zoneToEncode = Zone.localId(occupant.zoneId());
        int toEncode = kindToEncode << WITH_NEW_OCCUPANT_KIND_SHIFT | zoneToEncode;
        return new StateAction(gameState.withNewOccupant(occupant), Base32.encodeBits5(toEncode));
    }

    /**
     * Encode an action where the given occupant is removed from the board of the given game state.
     *
     * @param gameState the initial game state
     * @param occupant  the occupant to remove
     * @return the new game state resulting from the action and the encoded action
     */
    public static StateAction withOccupantRemoved(GameState gameState, Occupant occupant) {
        if (occupant == null)
            return new StateAction(gameState.withOccupantRemoved(null), Base32.encodeBits5(WITH_NO_OCCUPANT));
        List<Occupant> occupants = gameState.board().occupants().stream()
                .sorted(Comparator.comparingInt(Occupant::zoneId)).toList();
        int indexToEncode = occupants.indexOf(occupant); // a number between 0 and 24
        return new StateAction(gameState.withOccupantRemoved(occupant), Base32.encodeBits5(indexToEncode));
    }

    private static <T> T getIndexOrThrows(List<T> list, int index) throws IllegalActionException {
        if (list.size() < index) throw new IllegalActionException();
        return list.get(index);
    }

    private static void validateActionLength(String action, int length) throws IllegalActionException {
        if (action.length() != length) throw new IllegalActionException();
    }

    /**
     * Decode and apply the given action encoded in Base32 to the given game state,
     * throwing an IllegalActionException if the action is invalid.
     * This method lets the caller handle the exception, to choose what to do in case of an invalid action.
     *
     * @param gameState the initial game state
     * @param action    the Base32-code for the action to decode and apply
     * @return the new game state resulting from the action and the decoded action
     * @throws IllegalActionException if the action is invalid for the given game state
     */
    private static StateAction decodeAndApplyWithException(GameState gameState, String action)
            throws IllegalActionException {
        if (!Base32.isValid(action)) throw new IllegalActionException();
        // we decode the parameters of the given action differently
        // depending on the next action of the initial game state
        return switch (gameState.nextAction()) {
            case PLACE_TILE -> { //
                validateActionLength(action, WITH_PLACED_TILE_ACTION_LENGTH);
                int decoded = Base32.decode(action);
                int tileInFringeIdx = decoded >> WITH_PLACED_TILE_IDX_SHIFT;
                int rotationIdx = decoded & WITH_PLACED_TILE_ROTATION_MASK;
                Pos pos = getIndexOrThrows(fringeIndexes(gameState), tileInFringeIdx);
                Tile tile = gameState.tileToPlace();
                PlacedTile placedTile = new PlacedTile(
                    tile, gameState.currentPlayer(), Rotation.ALL.get(rotationIdx), pos
                );
                if (!gameState.board().canAddTile(placedTile)) throw new IllegalActionException();
                yield new StateAction(gameState.withPlacedTile(placedTile), action);
            }
            case OCCUPY_TILE -> {
                validateActionLength(action, WITH_NEW_OCCUPANT_ACTION_LENGTH);
                int decoded = Base32.decode(action);
                if (decoded == WITH_NO_OCCUPANT) yield new StateAction(gameState.withNewOccupant(null), action);
                int kindIdx = decoded >> WITH_NEW_OCCUPANT_KIND_SHIFT;
                int localId = decoded & WITH_NEW_OCCUPANT_ZONE_MASK;
                Occupant.Kind kind = Occupant.Kind.ALL.get(kindIdx);
                Occupant occupant = gameState.lastTilePotentialOccupants().stream()
                    .filter(occ -> occ.kind() == kind && Zone.localId(occ.zoneId()) == localId)
                    .findFirst()
                    .orElseThrow(IllegalActionException::new);
                yield new StateAction(gameState.withNewOccupant(occupant), action);
            }
            case RETAKE_PAWN -> {
                validateActionLength(action, WITH_OCCUPANT_REMOVED_ACTION_LENGTH);
                int decoded = Base32.decode(action);
                if (decoded == WITH_NO_OCCUPANT) yield new StateAction(gameState.withOccupantRemoved(null), action);
                List<Occupant> occupants = gameState.board().occupants().stream()
                    .sorted(Comparator.comparingInt(Occupant::zoneId)).toList();
                Occupant occupant = getIndexOrThrows(occupants, decoded);
                if (occupant.kind() != Occupant.Kind.PAWN) throw new IllegalActionException();
                PlayerColor currentPlayer = gameState.currentPlayer();
                PlayerColor occupantPlacer = gameState.board().tileWithId(Zone.tileId(occupant.zoneId())).placer();
                if (currentPlayer != occupantPlacer) throw new IllegalActionException();
                yield new StateAction(gameState.withOccupantRemoved(occupant), action);
            }
            default -> throw new IllegalActionException();
        };
    }

    /**
     * Decode and apply the given action encoded in Base32 to the given game state.
     *
     * @param gameState the initial game state
     * @param action    the Base32-code for the action to decode and apply
     * @return the new game state resulting from the action and the decoded action,
     * or null if the action is invalid
     */
    public static StateAction decodeAndApply(GameState gameState, String action) {
        // we catch the exception and return null if the action is invalid
        try {
            return decodeAndApplyWithException(gameState, action);
        } catch (IllegalActionException e) {
            return null;
        }
    }

    /**
     * A record to represent a pair of a game state and an action, used in this program
     * to return a game state resulting from an action and the Base32 encoded action itself.
     *
     * @param gameState the game state
     * @param action    the action
     */
    public record StateAction(GameState gameState, String action) {
    }
}
