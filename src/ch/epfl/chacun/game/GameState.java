package ch.epfl.chacun.game;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Represents the state of a game
 *
 * @param players      the ordered list of the game players
 * @param tileDecks    the tile decks of the game, containing the cards to place
 * @param tileToPlace  the tile to place on the board, may be null if no tile is to be placed
 * @param board        the board of the game, where the tiles can be placed
 * @param nextAction   the next action to be performed
 * @param messageBoard the message board of the game, containing the messages generated during the game
 * @author Valerio De Santis (373247)
 * @author Simon Lefort (371918)
 */
public record GameState(
        List<PlayerColor> players,
        TileDecks tileDecks,
        Tile tileToPlace,
        Board board,
        Action nextAction,
        MessageBoard messageBoard
) {

    /**
     * The minimum number of players in a game
     */
    private final static int MIN_PLAYER_COUNT = 2;

    /**
     * Represents the possible actions that can be performed in a game
     */
    public enum Action {
        START_GAME,
        PLACE_TILE,
        RETAKE_PAWN,
        OCCUPY_TILE,
        END_GAME
    }

    /**
     * Constructs a new game state, validating the arguments.
     * It is important that there isn't a tile to place if the next action is not to place a tile.
     *
     * @param players      the ordered list of the game players, must contain at least 2 players and not be null
     * @param tileDecks    the tile decks of the game, containing the cards to place, must not be null
     * @param tileToPlace  the tile to place on the board, may be null if no tile is to be placed
     * @param board        the board of the game, where the tiles can be placed, must not be null
     * @param nextAction   the next action to be performed, must not be null
     * @param messageBoard the message board of the game,
     *                     containing the messages generated during the game, must not be null
     * @throws NullPointerException     if any of the arguments is null, except tileToPlace
     * @throws IllegalArgumentException if there are not enough players or if the tileToPlace is null and
     *                                  the next action is to place a tile, or if the tileToPlace is not null and the next action is not to place a tile.
     */
    public GameState {

        Objects.requireNonNull(tileDecks);
        Objects.requireNonNull(board);
        Objects.requireNonNull(nextAction);
        Objects.requireNonNull(messageBoard);

        Objects.requireNonNull(players);
        players = List.copyOf(players);
        Preconditions.checkArgument(players.size() >= MIN_PLAYER_COUNT);

        Preconditions.checkArgument(tileToPlace == null ^ nextAction == Action.PLACE_TILE);
    }

    /**
     * Constructs the initial game state, with no tile to place, an empty board, and the start game action.
     *
     * @param players   the ordered list of the game players
     * @param tileDecks the tile decks of the game, containing the cards to place
     * @param textMaker the text maker to generate the messages
     * @return the initial game state, with no tile to place, an empty board, and the start game action
     */
    public static GameState initial(List<PlayerColor> players, TileDecks tileDecks, TextMaker textMaker) {
        return new GameState(
                players,
                tileDecks,
                null,
                Board.EMPTY,
                Action.START_GAME,
                new MessageBoard(textMaker, List.of())
        );
    }

    /**
     * Returns the color of the player who is currently playing
     *
     * @return the color of the player who is currently playing, or null if the game is not started or ended
     */
    public PlayerColor currentPlayer() {
        boolean isGameRunning = nextAction != Action.START_GAME && nextAction != Action.END_GAME;
        return isGameRunning ? players.getFirst() : null;
    }

    /**
     * Returns the number of free occupants of a given kind that a player can still place on the board
     *
     * @param player the player for whom to count the free occupants
     * @param kind   the kind of the occupants to count
     * @return the number of free occupants of the given kind that the player can place on the board
     */
    public int freeOccupantsCount(PlayerColor player, Occupant.Kind kind) {
        return Occupant.occupantsCount(kind) - board.occupantCount(player, kind);
    }

    /**
     * Returns the set of occupants that the placer may use to occupy
     * the last placed tile, the latter having to be non-null
     *
     * @return the set of occupants that may be placed on the last placed tile by the player
     * @throws IllegalArgumentException if the board is empty
     */
    public Set<Occupant> lastTilePotentialOccupants() {
        PlacedTile tile = board.lastPlacedTile();
        // if the board is empty, lastPlacedTile will return null
        Preconditions.checkArgument(tile != null);
        return tile.potentialOccupants()
                .stream()
                .filter(occupant ->
                        freeOccupantsCount(currentPlayer(), occupant.kind()) > 0
                                && !switch (tile.zoneWithId(occupant.zoneId())) {
                            case Zone.Forest forestZone -> board.forestArea(forestZone).isOccupied();
                            case Zone.Meadow meadowZone -> board.meadowArea(meadowZone).isOccupied();
                            case Zone.River riverZone when occupant.kind() == Occupant.Kind.PAWN ->
                                    board.riverArea(riverZone).isOccupied();
                            // here we handle the case when the occupant is a hut
                            case Zone.Water waterZone -> board.riverSystemArea(waterZone).isOccupied();
                        }
                )
                .collect(Collectors.toSet());
    }

    /**
     * Returns a new game state with the starting tile placed on the board,
     * and the first normal tile drawn to place
     *
     * @return a new game state with the starting tile placed on the board,
     * and the first normal tile drawn to place
     * @throws IllegalArgumentException if the next action is not to start the game
     */
    public GameState withStartingTilePlaced() {
        // checks that the next action is to start the game
        Preconditions.checkArgument(nextAction == Action.START_GAME);

        // draws the starting tile and places it on the board,
        // at position (0, 0), with no rotation and no placer
        Tile startingTile = tileDecks.topTile(Tile.Kind.START);
        PlacedTile startingPlacedTile = new PlacedTile(
                startingTile,
                null,
                Rotation.NONE,
                Pos.ORIGIN
        );
        // updates the tile decks by removing the drawn starting tile
        TileDecks newDecks = tileDecks.withTopTileDrawn(Tile.Kind.START);
        Board newBoard = board.withNewTile(startingPlacedTile);
        // draws the first normal tile to place,
        // which can always be placed as the starting tile has at least a side of every kind
        Tile tileToPlace = newDecks.topTile(Tile.Kind.NORMAL);
        newDecks = newDecks.withTopTileDrawn(Tile.Kind.NORMAL);
        return new GameState(players, newDecks, tileToPlace, newBoard, Action.PLACE_TILE, messageBoard);
    }

    /**
     * Returns a new game state with the given pawn removed,
     * if the player can occupy the last placed tile, he is given the opportunity to do it
     * during the next action, their turn will end otherwise
     *
     * @param occupant the pawn that the player decided to retake, null if he chose to retake none
     * @return a new game state with the given pawn removed, and the player to occupy the tile,
     * with their turn ending if he can not occupy it
     * @throws IllegalArgumentException if the next action is not to retake a pawn
     *                                  or if the given occupant is neither a pawn nor null
     */
    public GameState withOccupantRemoved(Occupant occupant) {
        Preconditions.checkArgument(nextAction == Action.RETAKE_PAWN);
        Preconditions.checkArgument(occupant == null || occupant.kind() == Occupant.Kind.PAWN);
        return new GameState(players, tileDecks, null,
                occupant == null ? board : board.withoutOccupant(occupant),
                Action.OCCUPY_TILE, messageBoard)
                .withTurnFinishedIfOccupationImpossible();
    }

    /**
     * Returns a new game state with the given occupant added and the player's turn finished
     *
     * @param occupant the occupant that the player decided to place, null if he chose to place none
     * @return a new game state with the given occupant added and the player's turn finished
     * @throws IllegalArgumentException if the next action is not to occupy a tile
     *                                  or if the last placed tile is null
     */
    public GameState withNewOccupant(Occupant occupant) {
        Preconditions.checkArgument(nextAction == Action.OCCUPY_TILE);
        Preconditions.checkArgument(board.lastPlacedTile() != null);
        return new GameState(players, tileDecks, null,
                occupant == null ? board : board.withOccupant(occupant),
                Action.OCCUPY_TILE, messageBoard)
                .withTurnFinished();
    }

    private Set<Animal> deersToCancel(Area<Zone.Meadow> meadowArea) {
        Set<Animal> animals = Area.animals(meadowArea, board.cancelledAnimals());
        Set<Animal> deers = animals.stream().filter(animal -> animal.kind() == Animal.Kind.DEER)
                .collect(Collectors.toSet());
        return deers.stream()
                // we can cancel at most as many deers as there are or
                // as many as there are tigers, whichever is the smallest
                // tells us how many deers we are going to cancel
                .limit(
                        Math.min(
                                deers.size(),
                                animals.stream().filter(animal -> animal.kind() == Animal.Kind.TIGER).count()
                        )
                )
                .collect(Collectors.toSet());
    }

    private Set<Animal> deersToCancelWithPitTrap(Zone.Meadow pitTrapZone) {
        Area<Zone.Meadow> meadowArea = board.meadowArea(pitTrapZone);
        Set<Animal> animals = Area.animals(meadowArea, board.cancelledAnimals());

        // we get the position of the tile where the pit trap
        Pos pitTrapPosition = board.tileWithId(pitTrapZone.tileId()).pos();
        // we get the meadow area surrounding the pit trap
        Area<Zone.Meadow> adjacentMeadow = board.adjacentMeadow(pitTrapPosition, pitTrapZone);

        Set<Animal> adjacentDeers = Area.animals(adjacentMeadow, board.cancelledAnimals())
                .stream().filter(animal -> animal.kind() == Animal.Kind.DEER)
                .collect(Collectors.toSet());

        // we create a new set with all the deers, then remove the adjacent deers
        Set<Animal> outsideDeers = animals.stream().filter(animal -> animal.kind() == Animal.Kind.DEER)
                .collect(Collectors.toSet());
        outsideDeers.removeAll(adjacentDeers);

        // we order the deers by decreasing distance to the pit trap,
        // and cancel as many deers as we have to remove
        // (the max between the number of deers and the number of tigers)
        return Stream.concat(outsideDeers.stream(), adjacentDeers.stream())
                .limit(
                        Math.min(
                                outsideDeers.size() + adjacentDeers.size(),
                                animals.stream().filter(animal -> animal.kind() == Animal.Kind.TIGER).count()
                        )
                )
                .collect(Collectors.toSet());
    }

    /**
     * Places a tile on the board and handles the special power it may have.
     * If there is a shaman, and the player has at least a pawn on the board,
     * then the next action will be to retake a pawn.
     * Otherwise, the game will pass to the occupy tile action,
     * or will end the player's turn if he can't occupy the tile
     * Some special powers may trigger a message signaling that some points have been scored.
     *
     * @param tile the tile to be placed
     * @return a new GameState with the given tile placed and the next action to perform
     * @throws IllegalArgumentException if the next action is not to place a tile
     *                                  or if the tile to place is already occupied
     */
    public GameState withPlacedTile(PlacedTile tile) {
        Preconditions.checkArgument(nextAction == Action.PLACE_TILE && tile.occupant() == null);

        MessageBoard newMessageBoard = messageBoard;
        // places the tile on the board
        Board newBoard = board.withNewTile(tile);
        // checks if the tile that has been placed has a special power zone
        // that would give the placer some points
        switch (tile.specialPowerZone()) {
            case Zone.Lake lake when lake.specialPower() == Zone.SpecialPower.LOGBOAT -> {
                Area<Zone.Water> riverSystem = newBoard.riverSystemArea(lake);
                newMessageBoard = newMessageBoard.withScoredLogboat(currentPlayer(), riverSystem);
            }
            case Zone zone when zone.specialPower() == Zone.SpecialPower.SHAMAN -> {
                // returns a new game state with retake pawn action
                // if the player has at least a pawn on the board (to be removed)
                if (newBoard.occupantCount(currentPlayer(), Occupant.Kind.PAWN) > 0) {
                    return new GameState(
                            players, tileDecks, null, newBoard, Action.RETAKE_PAWN, newMessageBoard
                    );
                }
            }
            case Zone.Meadow meadow when meadow.specialPower() == Zone.SpecialPower.HUNTING_TRAP -> {
                Area<Zone.Meadow> adjacentMeadow = newBoard.adjacentMeadow(tile.pos(), meadow);
                Set<Animal> cancelledDeers = deersToCancel(adjacentMeadow);
                newMessageBoard = newMessageBoard.withScoredHuntingTrap(
                        currentPlayer(), adjacentMeadow, cancelledDeers
                );
                // then cancel all other animals
                newBoard = newBoard.withMoreCancelledAnimals(Area.animals(adjacentMeadow, newBoard.cancelledAnimals()));
            }
            case null, default -> {
            }
        }

        return new GameState(players, tileDecks, null, newBoard, Action.OCCUPY_TILE, newMessageBoard)
                .withTurnFinishedIfOccupationImpossible();
    }

    /**
     * Finishes the player's turn if he can not occupy the tile,
     * or lets him occupy it if there is some potential occupant he may place
     *
     * @return the current instance of game state if the player can occupy the last placed tile,
     * and finishes their turn otherwise
     * @throws IllegalArgumentException if the next action is not to place a tile
     */
    private GameState withTurnFinishedIfOccupationImpossible() {
        Preconditions.checkArgument(nextAction == Action.OCCUPY_TILE);
        return lastTilePotentialOccupants().isEmpty() ? withTurnFinished() : this;
    }

    /**
     * Lets the player place a menhir tile if he used a normal tile to close a forest containing a menhir.
     * Finishes the player's turn otherwise,
     * allowing the next player to place a normal tile if there is one that he can place
     * and ending the game otherwise
     *
     * @return a new game state with the player placing a menhir tile if he can,
     * or generally finishing his turn, eventually ending the game
     * @throws IllegalArgumentException if the next action is not to place a tile
     *                                  or if the last placed tile is null
     */
    private GameState withTurnFinished() {
        Preconditions.checkArgument(nextAction == Action.OCCUPY_TILE);
        Preconditions.checkArgument(board.lastPlacedTile() != null);

        MessageBoard newMessageBoard = messageBoard;
        // the tile has already been added to the board previously
        Board newBoard = board;
        TileDecks newTileDecks = tileDecks;

        // adds a message for every closed forest and river
        Set<Area<Zone.Forest>> closedForests = newBoard.forestsClosedByLastTile();
        Area<Zone.Forest> forestClosedMenhir = null;
        for (Area<Zone.Forest> forest : closedForests) {
            // we look for a closed forest with a menhir if there is one
            if (Area.hasMenhir(forest)) forestClosedMenhir = forest;
            newMessageBoard = newMessageBoard.withScoredForest(forest);
        }

        Set<Area<Zone.River>> closedRivers = newBoard.riversClosedByLastTile();
        for (Area<Zone.River> river : closedRivers) newMessageBoard = newMessageBoard.withScoredRiver(river);
        // removes the gatherers and fishers from the closed forests and rivers after they have been scored
        newBoard = newBoard.withoutGatherersOrFishersIn(closedForests, closedRivers);


        assert newBoard.lastPlacedTile() != null;
        // if there is a forest containing a menhir that has just been closed by a normal tile,
        // the next tile to draw will be a menhir tile
        if (forestClosedMenhir != null && newBoard.lastPlacedTile().kind() == Tile.Kind.NORMAL) {
            newTileDecks = newTileDecks.withTopTileDrawnUntil(Tile.Kind.MENHIR, newBoard::couldPlaceTile);
            boolean couldPlaceMenhirTile = newTileDecks.deckSize(Tile.Kind.MENHIR) > 0;
            // if the player can place a menhir tile, a message signaling it is added to the message board
            if (couldPlaceMenhirTile) {
                newMessageBoard = newMessageBoard.withClosedForestWithMenhir(currentPlayer(), forestClosedMenhir);
                return new GameState(players, newTileDecks.withTopTileDrawn(Tile.Kind.MENHIR),
                        newTileDecks.topTile(Tile.Kind.MENHIR),
                        newBoard, Action.PLACE_TILE, newMessageBoard
                );
            }
        }
        // we look for the first normal tile that can be placed on the board
        newTileDecks = newTileDecks.withTopTileDrawnUntil(Tile.Kind.NORMAL, newBoard::couldPlaceTile);

        // if there is a normal tile that can be placed on the board, the player's turn finishes
        // and the next player will place the tile, otherwise the fame will end
        if (newTileDecks.deckSize(Tile.Kind.NORMAL) > 0) {
            List<PlayerColor> newPlayers = new LinkedList<>(players);
            Collections.rotate(newPlayers, -1);
            return new GameState(newPlayers, newTileDecks.withTopTileDrawn(Tile.Kind.NORMAL),
                    newTileDecks.topTile(Tile.Kind.NORMAL),
                    newBoard, Action.PLACE_TILE, newMessageBoard
            );
        } else {
            return new GameState(players, newTileDecks, null, newBoard, Action.END_GAME, newMessageBoard)
                    .withFinalPointsCounted();
        }

    }

    /**
     * Returns a new game state with the final points counted and the winners determined,
     * representing the end of the game
     *
     * @return the ending game state with the final points counted and the winners determined
     */
    private GameState withFinalPointsCounted() {

        Board newBoard = board;
        MessageBoard newMessageBoard = messageBoard;

        // the first part of the method handles the scoring of the meadow areas
        for (Area<Zone.Meadow> meadowArea : newBoard.meadowAreas()) {

            // checks if there is a meadow area containing a wildfire zone or one containing a pit trap zone
            boolean hasWildFireZone = meadowArea.zoneWithSpecialPower(Zone.SpecialPower.WILD_FIRE) != null;
            Zone.Meadow pitTrapZone = (Zone.Meadow) meadowArea.zoneWithSpecialPower(Zone.SpecialPower.PIT_TRAP);

            // if there is no fire and a pit trap zone, the deers adjacent to it are removed at last
            if (pitTrapZone != null) {
                PlacedTile pitTrapTile = newBoard.tileWithId(pitTrapZone.tileId());
                // removes the deers if there is no fire protecting them
                if (!hasWildFireZone) newBoard = newBoard.withMoreCancelledAnimals(
                        deersToCancelWithPitTrap(pitTrapZone)
                );

                // the pit trap is scored after some deers have been removed
                Area<Zone.Meadow> adjacentMeadow = newBoard.adjacentMeadow(pitTrapTile.pos(), pitTrapZone);
                newMessageBoard = newMessageBoard.withScoredPitTrap(adjacentMeadow, newBoard.cancelledAnimals());
            } else if (!hasWildFireZone) {
                // if there is no pit trap and no fire, the deers are cancelled following an arbitrary order
                newBoard = newBoard.withMoreCancelledAnimals(deersToCancel(meadowArea));
            }
            // the meadow is scored independently of the presence of a pit trap
            newMessageBoard = newMessageBoard.withScoredMeadow(meadowArea, newBoard.cancelledAnimals());
        }

        // the second part of the method handles the scoring of the river system areas

        for (Area<Zone.Water> waterArea : newBoard.riverSystemAreas()) {
            // the river system is scored independently of the presence of a raft,
            // and the raft is scored if there is one
            newMessageBoard = newMessageBoard.withScoredRiverSystem(waterArea);
            boolean hasRaft = waterArea.zoneWithSpecialPower(Zone.SpecialPower.RAFT) != null;
            if (hasRaft) newMessageBoard = newMessageBoard.withScoredRaft(waterArea);
        }

        Map<PlayerColor, Integer> points = newMessageBoard.points();
        int maxCount = points.values().stream().max(Integer::compareTo).orElse(0);
        Set<PlayerColor> winners = points.entrySet().stream()
                // we keep the players with the maximum number of points
                .filter(p -> p.getValue() == maxCount)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        newMessageBoard = newMessageBoard.withWinners(winners, maxCount);
        // the game is ended, and the winners are determined
        return new GameState(players, tileDecks, null, newBoard, Action.END_GAME, newMessageBoard);
    }

}
