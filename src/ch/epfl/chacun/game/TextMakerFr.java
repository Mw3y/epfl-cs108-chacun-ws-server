package ch.epfl.chacun.game;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.lang.StringTemplate.STR;

final public class TextMakerFr implements TextMaker {

    enum GameItem {
        MUSHROOM_GROUP("groupe"),
        FISH("poisson"),
        LAKE("lac"),
        TILE("tuile");

        private final String frenchName;

        public String getFrenchName() {
            return frenchName;
        }

        GameItem(String frenchName) {
            this.frenchName = frenchName;
        }
    }

    private static final Map<Animal.Kind, String> animalFrenchNames = Map.of(
            Animal.Kind.MAMMOTH, "mammouth",
            Animal.Kind.AUROCHS, "auroch",
            Animal.Kind.DEER, "cerf",
            Animal.Kind.TIGER, "tigre"
    );

    private final Map<PlayerColor, String> names;

    public TextMakerFr(Map<PlayerColor, String> names) {
        this.names = Map.copyOf(names);
    }

    private String pluralizeGameItems(GameItem item, int count) {
        return STR."\{count} \{pluralize(item.getFrenchName(), count)}";
    }

    /**
     * takes an ordered list
     *
     * @param items
     * @return
     */
    private String itemsToString(List<String> items) {
        Preconditions.checkArgument(!items.isEmpty());
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size(); i++) { // 0, 1
            sb.append(items.get(i));
            sb.append(i < items.size() - 2 ? ", " : i == items.size() - 2 ? " et " : "");
        }
        return sb.toString();
    }

    /**
     * Alice, Edgar et Bruno ont remporté X points en tant qu'occupant-e-s majoritaires
     */
    private String earnMessage(Set<PlayerColor> scorers) {
        Preconditions.checkArgument(!scorers.isEmpty());
        List<String> sortedPlayerNames = scorers.stream().sorted().map(this::playerName).toList();
        //the players are already ordered
        String playersToString = itemsToString(sortedPlayerNames);
        return STR."\{scorers.size() == 1 ? STR."\{playersToString} a" : STR."\{playersToString} ont"} remporté";
    }

    private String earnMessagePoints(Set<PlayerColor> scorers, int points) {
        // 10 points
        // 1 point
        return STR."\{earnMessage(scorers)} \{points(points)}";
    }

    private String earnMessageMajorityOccupants(Set<PlayerColor> scorers, int points) {
        int count = scorers.size();
        return STR."\{
                earnMessagePoints(scorers, points)
                } en tant qu'occupant·e\{
                count > 1 ? "·s" : ""
                } \{
                pluralize("majoritaire", count)
                }";
    }

    private String pluralize(String word, int count) {
        return count > 1 ? STR."\{word}s" : word;
    }

    private String animalsToString(Map<Animal.Kind, Integer> animals) {
        List<String> animalsList = animals
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .filter(e -> e.getValue() > 0)
                .map(e -> STR."\{e.getValue()} \{pluralize(animalFrenchNames.get(e.getKey()), e.getValue())}")
                .toList();
        return itemsToString(animalsList);
    }

    @Override
    public String playerName(PlayerColor playerColor) {
        return names.getOrDefault(playerColor, null);
    }

    @Override
    public String points(int points) {
        return STR."\{points} \{pluralize("point", points)}";
    }

    @Override
    public String playerClosedForestWithMenhir(PlayerColor player) {
        return STR."\{playerName(player)} a fermé une forêt contenant un menhir et peut donc placer une tuile menhir.";
    }

    @Override
    public String playersScoredForest(Set<PlayerColor> scorers, int points, int mushroomGroupCount, int tileCount) {
        return STR."\{
                earnMessageMajorityOccupants(scorers, points)
                } d'une forêt composée de \{
                pluralizeGameItems(GameItem.TILE, tileCount)
                }\{
                mushroomGroupCount > 0
                        ? STR." et de \{pluralizeGameItems(GameItem.MUSHROOM_GROUP, mushroomGroupCount)} de champignons."
                        : "."
                }";
    }

    @Override
    public String playersScoredRiver(Set<PlayerColor> scorers, int points, int fishCount, int tileCount) {
        return STR."\{
                earnMessageMajorityOccupants(scorers, points)
                } d'une rivière composée de \{
                pluralizeGameItems(GameItem.TILE, tileCount)
                }\{
                fishCount > 0
                        ? STR." et contenant \{pluralizeGameItems(GameItem.FISH, fishCount)}."
                        : "."}";
    }

    @Override
    public String playerScoredHuntingTrap(PlayerColor scorer, int points, Map<Animal.Kind, Integer> animals) {
        return STR."\{
                earnMessagePoints(Set.of(scorer), points)
                } en plaçant la fosse à pieux dans un pré dans lequel elle est entourée de \{
                animalsToString(animals)
                }.";
    }

    @Override
    public String playerScoredLogboat(PlayerColor scorer, int points, int lakeCount) {
        return STR."\{
                earnMessagePoints(Set.of(scorer), points)
                } en plaçant la pirogue dans un réseau hydrographique contenant \{
                pluralizeGameItems(GameItem.LAKE, lakeCount)
                }.";
    }

    @Override
    public String playersScoredMeadow(Set<PlayerColor> scorers, int points, Map<Animal.Kind, Integer> animals) {
        return STR."\{earnMessageMajorityOccupants(scorers, points)} d'un pré contenant \{animalsToString(animals)}.";
    }

    @Override
    public String playersScoredRiverSystem(Set<PlayerColor> scorers, int points, int fishCount) {
        return STR."\{
                earnMessageMajorityOccupants(scorers, points)
                } d'un réseau hydrographique contenant \{
                pluralizeGameItems(GameItem.FISH, fishCount)
                }.";
    }

    @Override
    public String playersScoredPitTrap(Set<PlayerColor> scorers, int points, Map<Animal.Kind, Integer> animals) {
        return STR."\{
                earnMessageMajorityOccupants(scorers, points)
                } d'un pré contenant la grande fosse à pieux entourée de \{
                animalsToString(animals)
                }.";
    }

    @Override
    public String playersScoredRaft(Set<PlayerColor> scorers, int points, int lakeCount) {
        return STR."\{
                earnMessageMajorityOccupants(scorers, points)
                } d'un réseau hydrographique contenant le radeau et \{
                pluralizeGameItems(GameItem.LAKE, lakeCount)
                }.";
    }

    @Override
    public String playersWon(Set<PlayerColor> winners, int points) {
        return STR."\{earnMessage(winners)} la partie avec \{points(points)} !";
    }

    @Override
    public String clickToOccupy() {
        return "Cliquez sur le pion ou la hutte que vous désirez placer, ou ici pour ne pas en placer.";
    }

    @Override
    public String clickToUnoccupy() {
        return "Cliquez sur le pion que vous désirez reprendre, ou ici pour ne pas en reprendre.";
    }
}
