package ch.epfl.chacun.game;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents the message board of the game.
 *
 * @param textMaker the text maker used to generate the content of the messages
 * @param messages  the ordered messages on the message board, from the oldest to the newest
 * @author Valerio De Santis (373247)
 * @author Simon Lefort (371918)
 */
public record MessageBoard(TextMaker textMaker, List<Message> messages) {

    /**
     * Constructor for MessageBoard, validating the parameters
     *
     * @param textMaker the text maker used to generate the content of the messages
     * @param messages  the ordered messages on the message board, from the oldest to the newest
     */
    public MessageBoard {
        // the list is copied to ensure immutability
        messages = List.copyOf(messages);
    }

    /**
     * The possible types of a meadow message
     */
    private enum MeadowMessageType {
        HUNTING_TRAP, PIT_TRAP, MEADOW
    }

    /**
     * Returns a map linking the kinds of animals to their number
     *
     * @param animals the animals in the meadow
     * @return a map linking the kinds of animals to their number
     */
    private Map<Animal.Kind, Integer> forMeadowAnimalCount(Set<Animal> animals) {
        return animals.stream().collect(Collectors.groupingBy(Animal::kind, Collectors.summingInt(a -> 1)));
    }

    /**
     * In the case of a meadow with a hunting trap,
     * the scorer is the player who placed the tile containing it.
     * Otherwise, the scorers are the majority occupants of the meadow.
     * Returns a new message board with the message of the event added
     *
     * @param messageType      the type of the meadow message
     * @param meadow           the meadow area to score
     * @param cancelledAnimals the animals whose presence has to be ignored
     * @param tilePlacer       the player who placed the tile, if the message is about a hunting trap
     * @return a new message board with the message of the event added
     */
    private MessageBoard withGenericScoredMeadow(
            MeadowMessageType messageType, Area<Zone.Meadow> meadow, Set<Animal> cancelledAnimals, PlayerColor tilePlacer
    ) {
        Set<Animal> animals = Area.animals(meadow, cancelledAnimals);
        int points = forMeadowTotalAnimals(animals);
        if (points <= 0) return this;
        return switch (messageType) {
            case MEADOW -> {
                if (!meadow.isOccupied()) yield this;
                Set<PlayerColor> majorityOccupants = meadow.majorityOccupants();
                yield withNewMessage(
                        textMaker.playersScoredMeadow(majorityOccupants, points, forMeadowAnimalCount(animals)),
                        points,
                        majorityOccupants,
                        meadow.tileIds()
                );
            }
            case HUNTING_TRAP -> withNewMessage(
                    textMaker.playerScoredHuntingTrap(tilePlacer, points, forMeadowAnimalCount(animals)),
                    points,
                    Set.of(tilePlacer),
                    meadow.tileIds()
            );
            case PIT_TRAP -> {
                if (!meadow.isOccupied()) yield this;
                Set<PlayerColor> majorityOccupants = meadow.majorityOccupants();
                yield withNewMessage(
                        textMaker.playersScoredPitTrap(majorityOccupants, points, forMeadowAnimalCount(animals)),
                        points,
                        majorityOccupants,
                        meadow.tileIds()
                );
            }
        };
    }

    /**
     * Returns a new message board with the message of the event added.
     * The scorers are the majority occupants of the meadow.
     *
     * @param messageType      the type of the meadow message
     * @param meadow           the meadow area to score
     * @param cancelledAnimals the animals whose presence has to be ignored
     * @return a new message board with the message of the event added
     */
    private MessageBoard withGenericScoredMeadow(
            MeadowMessageType messageType, Area<Zone.Meadow> meadow, Set<Animal> cancelledAnimals
    ) {
        return withGenericScoredMeadow(messageType, meadow, cancelledAnimals, null);
    }

    /**
     * Returns the points the animals in the meadow give to the player
     *
     * @param animals the animals in the meadow
     * @return the points the animals in the meadow give to the player
     */
    private int forMeadowTotalAnimals(Set<Animal> animals) {
        Map<Animal.Kind, Integer> points = forMeadowAnimalCount(animals);
        return Points.forMeadow(
                points.getOrDefault(Animal.Kind.MAMMOTH, 0),
                points.getOrDefault(Animal.Kind.AUROCHS, 0),
                points.getOrDefault(Animal.Kind.DEER, 0)
        );
    }

    /**
     * Returns a new message board with the given message added
     *
     * @param text    the text of the message
     * @param count   the points the scorers get from the event triggering this message
     * @param scorers the players who will get the points
     * @param tileIds the ids of the tiles involved in the event triggering this message
     * @return a new message board with the given message added
     */
    private MessageBoard withNewMessage(String text, int count, Set<PlayerColor> scorers, Set<Integer> tileIds) {
        // we instantiate this as an array list because we will only add messages at the end
        List<Message> newMessages = new ArrayList<>(messages);
        newMessages.add(new Message(text, count, scorers, tileIds));
        return new MessageBoard(textMaker, newMessages);
    }

    /**
     * Returns a map matching the scorers to the points
     * they got from the messages on the message board
     *
     * @return a map matching the scorers to the points they got from the messages on the message board
     */
    public Map<PlayerColor, Integer> points() {
        return messages.stream()
                // we map every message to a stream of simple entries (scorer, points)
                .flatMap(message -> message.scorers().stream()
                        .map(scorer -> new AbstractMap.SimpleEntry<>(scorer, message.points())))
                // we collect the entries to a map, summing the points of the scorers that appear in more than one message
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, Integer::sum));
    }

    /**
     * If the river area is occupied,
     * the closure of the river getting some player some points,
     * the method returns a new message board with the message of the event added
     * and the points added to the scorers
     * If the forest isn't occupied (no scorers), returns the same message board
     *
     * @param forest the forest that has been closed
     * @return a new message board with the message of the event,
     * the same message board if the forest isn't occupied
     */
    public MessageBoard withScoredForest(Area<Zone.Forest> forest) {
        if (!forest.isOccupied()) return this;
        int tileCount = forest.tileIds().size();
        int mushroomCount = Area.mushroomGroupCount(forest);
        int points = Points.forClosedForest(tileCount, mushroomCount);
        Set<PlayerColor> majorityOccupants = forest.majorityOccupants();
        return withNewMessage(
                textMaker.playersScoredForest(majorityOccupants, points, mushroomCount, tileCount),
                points,
                majorityOccupants,
                forest.tileIds()
        );
    }

    /**
     * Returns a new message board with the message of the event added
     * signaling that the player can place another tile because the forest
     * he has closed contains one or more menhir.
     *
     * @param player the player who placed the tile
     * @param forest the forest area that has been closed
     * @return a new message board with the message of the event added
     */
    public MessageBoard withClosedForestWithMenhir(PlayerColor player, Area<Zone.Forest> forest) {
        return withNewMessage(
                textMaker.playerClosedForestWithMenhir(player),
                0,
                Set.of(),
                forest.tileIds()
        );
    }

    /**
     * If the river area is occupied,
     * the closure of the river getting some player some points,
     * the method returns a new message board with the message of the event added
     * and the points added to the scorers
     * If the river isn't occupied (no scorers), returns the same message board
     *
     * @param river the river that has been closed
     * @return a new message board with the message of the event,
     * the same message board if the river isn't occupied
     */
    public MessageBoard withScoredRiver(Area<Zone.River> river) {
        if (!river.isOccupied()) return this;
        int tileCount = river.tileIds().size();
        int fishCount = Area.riverFishCount(river);
        int points = Points.forClosedRiver(tileCount, fishCount);
        Set<PlayerColor> majorityOccupants = river.majorityOccupants();
        return withNewMessage(
                textMaker.playersScoredRiver(majorityOccupants, points, fishCount, tileCount),
                points,
                majorityOccupants,
                river.tileIds()
        );
    }

    /**
     * Returns a new message board with the message of the event added,
     * signaling that the player got some points from placing the logboat
     * on the given river system
     *
     * @param scorer      the player who placed the logboat
     * @param riverSystem the river system the logboat has been placed on
     * @return a new message board with the message of the event added
     */
    public MessageBoard withScoredLogboat(PlayerColor scorer, Area<Zone.Water> riverSystem) {
        int lakeCount = Area.lakeCount(riverSystem);
        int points = Points.forLogboat(lakeCount);
        return withNewMessage(
                textMaker.playerScoredLogboat(scorer, points, lakeCount),
                points,
                Set.of(scorer),
                riverSystem.tileIds()
        );
    }

    /**
     * If placing the hunting trap got the player some points,
     * the method returns a new message board with the message of the event added
     *
     * @param scorer         the player who placed the hunting trap
     * @param adjacentMeadow the meadow area adjacent to the hunting trap,
     *                       containing the meadows surrounding the placed hunting trap
     * @return a new message board with the message of the event added,
     * the same message board if the hunting trap didn't get any point
     */
    public MessageBoard withScoredHuntingTrap(
            PlayerColor scorer,
            Area<Zone.Meadow> adjacentMeadow,
            Set<Animal> cancelledAnimals
    ) {
        return withGenericScoredMeadow(MeadowMessageType.HUNTING_TRAP, adjacentMeadow, cancelledAnimals, scorer);
    }

    /**
     * If the meadow area is occupied and the scored points are positive (depending on the animals in the meadow),
     * the method returns a new message board with the message of the event added
     *
     * @param meadow           the meadow area
     * @param cancelledAnimals the animals whose presence has to be ignored
     * @return a new message board with the message of the event added if some player got some points
     */
    public MessageBoard withScoredMeadow(Area<Zone.Meadow> meadow, Set<Animal> cancelledAnimals) {
        return withGenericScoredMeadow(MeadowMessageType.MEADOW, meadow, cancelledAnimals);
    }

    /**
     * If the meadow area is occupied and the scored points are positive (depending on the animals in the meadow),
     * the method returns a new message board with the message of the event added
     *
     * @param adjacentMeadow   the meadow area containing the pit trap and the meadows surrounding it
     * @param cancelledAnimals the animals whose presence has to be ignored
     * @return a new message board with the message of the event added if some player got some points
     */
    public MessageBoard withScoredPitTrap(Area<Zone.Meadow> adjacentMeadow, Set<Animal> cancelledAnimals) {
        return withGenericScoredMeadow(MeadowMessageType.PIT_TRAP, adjacentMeadow, cancelledAnimals);
    }

    /**
     * If the river system area is occupied and the scored points are positive (depending on the fishes in the system),
     * the method returns a new message board with the message of the event added
     *
     * @param riverSystem the river system area
     * @return a new message board with the message of the event added if some player got some points
     */
    public MessageBoard withScoredRiverSystem(Area<Zone.Water> riverSystem) {
        if (!riverSystem.isOccupied()) return this;
        int fishCount = Area.riverSystemFishCount(riverSystem);
        int points = Points.forRiverSystem(fishCount);
        if (points <= 0) return this;
        Set<PlayerColor> majorityOccupants = riverSystem.majorityOccupants();
        return withNewMessage(
                textMaker.playersScoredRiverSystem(majorityOccupants, points, fishCount),
                points,
                majorityOccupants,
                riverSystem.tileIds()
        );
    }

    /**
     * If the river system is occupied,
     * returns a new message board with the message of the event added,
     *
     * @param riverSystem the river system area containing the raft
     * @return a new message board with the message of the event added if the river system had some occupant
     */
    public MessageBoard withScoredRaft(Area<Zone.Water> riverSystem) {
        if (!riverSystem.isOccupied()) return this;
        int lakeCount = Area.lakeCount(riverSystem);
        int points = Points.forRaft(lakeCount);
        Set<PlayerColor> majorityOccupants = riverSystem.majorityOccupants();
        return withNewMessage(
                textMaker.playersScoredRaft(majorityOccupants, points, lakeCount),
                points,
                majorityOccupants,
                riverSystem.tileIds()
        );
    }

    /**
     * Creates a new board with a new message added,
     * stating that the given players have won the game,
     * with the given number of points
     *
     * @param winners the players having won the game
     * @param points  the number of points they had at the end of the game
     * @return a new board with a new message added, stating that the given players have won the game,
     * with the given number of points
     */
    public MessageBoard withWinners(Set<PlayerColor> winners, int points) {
        return withNewMessage(
                textMaker.playersWon(winners, points),
                0,
                Set.of(),
                Set.of()
        );
    }

    /**
     * Represents a message on the message board.
     *
     * @param text    the text of the message, non-null
     * @param points  the points the scorers get from the event triggering this message, a non-negative integer
     * @param scorers the players who will get the points
     * @param tileIds the ids of the tiles involved in the event triggering this message
     */
    public record Message(String text, int points, Set<PlayerColor> scorers, Set<Integer> tileIds) {
        /**
         * Constructor for Message, validating the parameters
         * the text has to be non-null
         * the points have to be non-negative
         * the scorers and the tileIds may be empty
         * (if the event triggering the message doesn't get any point)
         *
         * @throws NullPointerException if the text is null
         */
        public Message {
            Objects.requireNonNull(text);
            Preconditions.checkArgument(points >= 0);
            // the sets are copied to ensure immutability,
            // PlayerColors and Integers are immutable
            scorers = Set.copyOf(scorers);
            tileIds = Set.copyOf(tileIds);
        }
    }
}
