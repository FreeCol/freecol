/**
 *  Copyright (C) 2002-2011  The FreeCol Team
 *
 *  This file is part of FreeCol.
 *
 *  FreeCol is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  FreeCol is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FreeCol.  If not, see <http://www.gnu.org/licenses/>.
 */


package net.sf.freecol.common.model;


/**
 * Keeps track of the available game options. New options must be
 * added to the {@link Specification} and each option should be given
 * an unique identifier (defined as a constant in this class).
 */
public class GameOptions {

    /** The amount of money each player will receive before the game starts. */
    public static final String STARTING_MONEY = "model.option.startingMoney";

    /** The cost of a single hammer when buying a building in a colony. */
    //Unused at the moment
    // public static final String HAMMER_PRICE = "hammerPrice";

    /** Does the Custom House sell boycotted goods **/
    public static final String CUSTOM_IGNORE_BOYCOTT = "model.option.customIgnoreBoycott";

    /** Whether experts have connections, producing without raw materials in factories */
    public static final String EXPERTS_HAVE_CONNECTIONS = "model.option.expertsHaveConnections";

    public static final String SAVE_PRODUCTION_OVERFLOW = "model.option.saveProductionOverflow";

    /** Whether to award exploration points or not. */
    public static final String EXPLORATION_POINTS = "model.option.explorationPoints";

    /** Enables/disables fog of war. */
    public static final String FOG_OF_WAR = "model.option.fogOfWar";

    /**
     * Victory condition: Should the <code>Player</code> who first defeats the
     * Royal Expeditionary Force win the game?
     */
    public static final String VICTORY_DEFEAT_REF = "model.option.victoryDefeatREF";

    /**
     * Victory condition: Should a <code>Player</code> who first defeats all
     * other european players win the game?
     */
    public static final String VICTORY_DEFEAT_EUROPEANS = "model.option.victoryDefeatEuropeans";

    /**
     * Victory condition: Should a <code>Player</code> who first defeats all
     * other human players win the game?
     */
    public static final String VICTORY_DEFEAT_HUMANS = "model.option.victoryDefeatHumans";

    /**
     * Whether to educate the least skilled unit first. This is the
     * behaviour of the original game and disallows manually assigning
     * students to teachers.
     */
    public static final String ALLOW_STUDENT_SELECTION =
        "model.option.allowStudentSelection";

    /**
     * The year in which the game starts. At the moment, changing this
     * value only shortens the game. In future, it might cause the map
     * generator to create foreign colonies.
     */
    public static final String STARTING_YEAR = "model.option.startingYear";

    /**
     * The first year in which there are two seasons. Changing this
     * value influences the duration of the game.
     */
    public static final String SEASON_YEAR = "model.option.seasonYear";

    /**
     * The year in which owning at least one colony becomes mandatory.
     */
    public static final String MANDATORY_COLONY_YEAR = "model.option.mandatoryColonyYear";

    /**
     * The very last year of the game.
     */
    public static final String LAST_YEAR = "model.option.lastYear";

    /**
     * The last year of the game for colonial players. In other words,
     * if a colonial player does not declare independence by the end
     * of this year, the game is lost.
     */
    public static final String LAST_COLONIAL_YEAR = "model.option.lastColonialYear";

    /**
     * How to determine the starting positions of European players.
     */
    public static final String STARTING_POSITIONS = "model.option.startingPositions";
    public static final int STARTING_POSITIONS_CLASSIC = 0;
    public static final int STARTING_POSITIONS_RANDOM = 1;
    public static final int STARTING_POSITIONS_HISTORICAL = 2;

}
