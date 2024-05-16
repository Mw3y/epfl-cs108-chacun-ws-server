package ch.epfl.chacun.game;

import java.util.Map;
import java.util.Set;

public interface TextMaker {
    /**
     * Retourne le nom du joueur de couleur donnée.
     *
     * @param playerColor la couleur du joueur
     * @return le nom du joueur
     */
    String playerName(PlayerColor playerColor);

    /**
     * Retourne la représentation textuelle du nombre de points donnés (p. ex. "3 points").
     *
     * @param points le nombre de points
     * @return la représentation textuelle du nombre de points
     */
    String points(int points);

    /**
     * Retourne le texte d'un message déclarant qu'un joueur a fermé une forêt avec un menhir.
     *
     * @param player le joueur ayant fermé la forêt
     * @return le texte du message
     */
    String playerClosedForestWithMenhir(PlayerColor player);

    /**
     * Retourne le texte d'un message déclarant que les occupants majoritaires d'une forêt nouvellement
     * fermée, constituée d'un certain nombre de tuiles et comportant un certain nombre de groupes de champignons,
     * ont remporté les points correspondants.
     *
     * @param scorers            les occupants majoritaires de la forêt
     * @param points             les points remportés
     * @param mushroomGroupCount le nombre de groupes de champignons que la forêt contient
     * @param tileCount          le nombre de tuiles qui constitue la forêt
     * @return le texte du message
     */
    String playersScoredForest(Set<PlayerColor> scorers, int points, int mushroomGroupCount, int tileCount);

    /**
     * Retourne le texte d'un message déclarant que les occupants majoritaires d'une rivière nouvellement
     * fermée, constituée d'un certain nombre de tuiles et comportant un certain nombre de poissons,
     * ont remporté les points correspondants.
     *
     * @param scorers   les occupants majoritaires de la rivière
     * @param points    les points remportés
     * @param fishCount le nombre de poissons nageant dans la rivière ou les lacs adjacents
     * @param tileCount le nombre de tuiles qui constitue la rivière
     * @return le texte du message
     */
    String playersScoredRiver(Set<PlayerColor> scorers, int points, int fishCount, int tileCount);

    /**
     * Retourne le texte d'un message déclarant qu'un joueur a déposé la fosse à pieux dans un pré contenant,
     * sur les 8 tuiles voisines de la fosse, certains animaux, et remporté les points correspondants.
     *
     * @param scorer  le joueur ayant déposé la fosse à pieux
     * @param points  les points remportés
     * @param animals les animaux présents dans le même pré que la fosse et sur les 8 tuiles voisines
     * @return le texte du message
     */
    String playerScoredHuntingTrap(PlayerColor scorer, int points, Map<Animal.Kind, Integer> animals);

    /**
     * Retourne le texte d'un message déclarant qu'un joueur a déposé la pirogue dans un réseau hydrographique
     * comportant un certain nombre de lacs, et remporté les points correspondants.
     *
     * @param scorer    le joueur ayant déposé la pirogue
     * @param points    les points remportés
     * @param lakeCount le nombre de lacs accessibles à la pirogue
     * @return le texte du message
     */
    String playerScoredLogboat(PlayerColor scorer, int points, int lakeCount);

    /**
     * Retourne le texte d'un message déclarant que les occupants majoritaires d'un pré contenant certains
     * animaux ont remporté les points correspondants.
     *
     * @param scorers les occupants majoritaires du pré
     * @param points  les points remportés
     * @param animals les animaux présents dans le pré (sans ceux ayant été précédemment annulés)
     * @return le texte du message
     */
    String playersScoredMeadow(Set<PlayerColor> scorers, int points, Map<Animal.Kind, Integer> animals);

    /**
     * Retourne le texte d'un message déclarant que les occupants majoritaires d'un réseau hydrographique
     * comportant un certain nombre de poissons ont remporté les points correspondants.
     *
     * @param scorers   les occupants majoritaires du réseau hydrographique
     * @param points    les points remportés
     * @param fishCount le nombre de poissons nageant dans le réseau hydrographique
     * @return le texte du message
     */
    String playersScoredRiverSystem(Set<PlayerColor> scorers, int points, int fishCount);

    /**
     * Retourne le texte d'un message déclarant que les occupants majoritaires d'un pré contenant la
     * grande fosse à pieux et, sur les 8 tuiles voisines d'elles, certains animaux, ont remporté les
     * points correspondants.
     *
     * @param scorers les occupants majoritaires du pré contenant la fosse à pieux
     * @param points  les points remportés
     * @param animals les animaux présents sur les tuiles voisines de la fosse (sans ceux ayant été précédemment annulés)
     * @return le texte du message
     */
    String playersScoredPitTrap(Set<PlayerColor> scorers, int points, Map<Animal.Kind, Integer> animals);

    /**
     * Retourne le texte d'un message déclarant que les occupants majoritaires d'un réseau hydrographique
     * contenant le radeau ont remporté les points correspondants.
     *
     * @param scorers   les occupants majoritaires du réseau hydrographique comportant le radeau
     * @param points    les points remportés
     * @param lakeCount le nombre de lacs contenus dans le réseau hydrographique
     * @return le texte du message
     */
    String playersScoredRaft(Set<PlayerColor> scorers, int points, int lakeCount);

    /**
     * Retourne le texte d'un message déclarant qu'un ou plusieurs joueurs ont remporté la partie, avec un
     * certain nombre de points.
     *
     * @param winners l'ensemble des joueurs ayant remporté la partie
     * @param points  les points des vainqueurs
     * @return le texte du message
     */
    String playersWon(Set<PlayerColor> winners, int points);

    /**
     * Retourne un texte demandant au joueur actuel de cliquer sur l'occupant qu'il désire placer, ou sur le texte
     * du message s'il ne désire placer aucun occupant.
     *
     * @return le texte en question
     */

    String clickToOccupy();

    /**
     * Retourne un texte demandant au joueur actuel de cliquer sur le pion qu'il désire reprendre, ou sur le texte
     * du message s'il ne désire reprendre aucun pion.
     *
     * @return le texte en question
     */
    String clickToUnoccupy();
}