package ch.epfl.chacun.logic;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents the logic of the game server.
 * @author Maxence Espagnet (sciper: 372808)
 * @author Simon Lefort (sciper: 371918)
 */
public class GameLogic {

    /**
     * All the game lobbies currently open.
     * A game lobby is a group of players waiting for a game to start.
     */
    private final Map<String, GameLobby> lobbies = new HashMap<>();

    /**
     * All the games currently ongoing.
     */
    private final Map<String, OnGoingGame> games = new HashMap<>();

    /**
     * Parse and apply an action received by the WebSocket.
     * @param action The action to parse and apply.
     * @param context The context of the player sending the action.
     * @return The response action to send back to the player or broadcast to the game.
     */
    public GameActionData parseAndApplyWebSocketAction(String action, GamePlayerData context) {
        String[] payload = action.split("\\.");
        System.out.println(Arrays.toString(payload));
        ServerAction serverAction = ServerAction.fromString(payload[0]);
        if (serverAction != ServerAction.UNKNOWN) {
            String gameName = context != null ? context.gameName() : null;
            String username = context != null ? context.username() : null;
            String[] data = payload.length > 1 ? payload[1].split(",") : new String[0];
            return applyAction(serverAction, data, gameName, username);
        }
        return null;
    }

    /**
     * Try to apply the action to a lobby or an ongoing game and determines the response to send.
     * @param action The action to apply.
     * @param data The data of the action.
     * @param gameName The name of the game the player is in.
     * @param username The username of the player sending the action.
     * @return The response action to send back to the player or broadcast to the game.
     */
    private GameActionData applyAction(ServerAction action, String[] data, String gameName, String username) {
        OnGoingGame game = games.get(gameName);
        return switch (action) {
            case GAMEJOIN -> {
                String providedGameName = data[0];
                String providedUsername = data[1];

                if (gameName != null) {
                    // The player is already in a game
                    yield new GameActionData(ServerAction.GAMEJOIN_DENY, "ALREADY_IN_GAME");
                }

                GameLobby lobby = lobbies.get(providedGameName);
                if (lobby != null) {
                    yield lobby.addPlayer(providedUsername);
                }

                if (games.get(providedGameName) != null) {
                    // The game has already started
                    yield new GameActionData(ServerAction.GAMEJOIN_DENY, "GAME_ALREADY_STARTED");
                }

                // Create a new game lobby
                lobbies.put(providedGameName, new GameLobby(providedGameName, providedUsername));
                yield new GameActionData(ServerAction.GAMEJOIN_ACCEPT, providedUsername,
                        new GamePlayerData(providedGameName, providedUsername));
            }
            case GAMEACTION -> {
                GameLobby lobby = lobbies.get(gameName);
                if (lobby != null && lobby.getPlayers().size() >= 2 && lobby.getPlayers().getFirst().equals(username)) {
                    game = startGameWithLobby(lobby);
                }

                if (game == null) {
                    yield new GameActionData(ServerAction.GAMEACTION_DENY, "GAME_NOT_STARTED");
                }

                yield game.applyAction(data[0], username);
            }
            case GAMELEAVE -> {
                GameLobby lobby = lobbies.get(gameName);
                if (lobby != null) {
                    yield lobby.removePlayer(username);
                }
                // Check if the game exists and has not ended
                if (game != null && !game.hasEnded()) {
                        games.remove(gameName); // Remove the game
                        yield new GameActionData(
                                ServerAction.GAMEEND, "PLAYER_LEFT_MIDGAME", true);
                }
                yield null;
            }
            case GAMEMSG -> {
                if (gameName != null) {
                    String message = STR."\{username}=\{data[0]}";
                    yield new GameActionData(ServerAction.GAMEMSG, message, true);
                }
                yield null;
            }
            default -> null;
        };
    }

    private OnGoingGame startGameWithLobby(GameLobby lobby) {
        OnGoingGame newGame = lobby.startGame();
        games.put(lobby.getGameName(), newGame);
        lobbies.remove(lobby.getGameName());
        return newGame;
    }
}
