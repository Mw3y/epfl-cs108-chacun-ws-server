package ch.epfl.chacun.game;


import java.util.List;
import java.util.function.Predicate;

/**
 * Represents the decks of tiles in the game
 *
 * @param startTiles  the start tiles deck (the first card of the game)
 * @param normalTiles the normal tiles deck
 * @param menhirTiles the menhir tiles deck
 * @author Valerio De Santis (373247)
 * @author Simon Lefort (371918)
 */
public record TileDecks(List<Tile> startTiles, List<Tile> normalTiles, List<Tile> menhirTiles) {

    /**
     * Constructor for TileDecks
     *
     * @param startTiles  the start tiles deck (the first card of the game)
     * @param normalTiles the normal tiles deck
     * @param menhirTiles the menhir tiles deck
     */
    public TileDecks {
        startTiles = List.copyOf(startTiles);
        normalTiles = List.copyOf(normalTiles);
        menhirTiles = List.copyOf(menhirTiles);
    }

    /**
     * returns the size of the deck of the given kind
     *
     * @param kind the kind of deck whose size is to be returned
     * @return the size of the deck of the given kind
     */
    public int deckSize(Tile.Kind kind) {
        return switch (kind) {
            case START -> startTiles.size();
            case NORMAL -> normalTiles.size();
            case MENHIR -> menhirTiles.size();
        };
    }

    /**
     * Gets the first card of a deck or null if the latter is empty
     *
     * @param deck the deck to get the first card from
     * @return the first card of a deck or null if the latter is empty
     */
    private Tile getFirstCard(List<Tile> deck) {
        return deck.isEmpty() ? null : deck.getFirst();
    }

    /**
     * Draws a card from a deck if the latter is not empty, throws an exception otherwise
     *
     * @param deck the deck to draw the card from
     * @return the deck without the first card
     * @throws IllegalArgumentException if the deck is empty
     */
    private List<Tile> drawCardFromDeck(List<Tile> deck) {
        Preconditions.checkArgument(!deck.isEmpty());
        // size() returns the index of the last element +1
        // subList() takes the index of the first element (inclusive)
        // and the index of the last element (exclusive)
        return deck.subList(1, deck.size()); // returns a new list without the first card
    }

    /**
     * Gets the top tile of the deck of the given kind, returning the first card of the deck,
     * null if there isn't any
     *
     * @param kind the kind of deck to get the top tile from
     * @return the first card of the deck of the given kind, null if there isn't any
     */
    public Tile topTile(Tile.Kind kind) {
        return switch (kind) {
            case START -> getFirstCard(startTiles);
            case NORMAL -> getFirstCard(normalTiles);
            case MENHIR -> getFirstCard(menhirTiles);
        };
    }

    /**
     * Draws the top tile of the deck of the given kind returning the decks with the given deck without the first card
     *
     * @param kind the kind of deck to draw the top tile from (not empty, otherwise throws an illegal argument exception)
     * @return the decks with the top tile drawn from the given deck
     */
    public TileDecks withTopTileDrawn(Tile.Kind kind) {
        return switch (kind) {
            case START -> new TileDecks(drawCardFromDeck(startTiles), normalTiles, menhirTiles);
            case NORMAL -> new TileDecks(startTiles, drawCardFromDeck(normalTiles), menhirTiles);
            case MENHIR -> new TileDecks(startTiles, normalTiles, drawCardFromDeck(menhirTiles));
        };
    }

    /**
     * Draws the top tile of the deck of the given kind until a predicate is satisfied
     *
     * @param kind      the kind of deck to draw the top tile from
     * @param predicate the predicate to satisfy by the first card of the specified deck
     * @return the tile decks, with the necessary tiles drawn
     */
    public TileDecks withTopTileDrawnUntil(Tile.Kind kind, Predicate<Tile> predicate) {
        boolean deckIsEmpty = this.deckSize(kind) == 0;
        if (deckIsEmpty) return this;
        boolean predicateIsVerified = predicate.test(this.topTile(kind));
        return predicateIsVerified ? this : withTopTileDrawn(kind).withTopTileDrawnUntil(kind, predicate);
    }

}
