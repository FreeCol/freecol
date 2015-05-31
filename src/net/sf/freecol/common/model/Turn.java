/**
 *  Copyright (C) 2002-2015   The FreeCol Team
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

import net.sf.freecol.common.i18n.Messages;
import static net.sf.freecol.common.util.StringUtils.*;


/**
 * Represents a given turn in the game.
 */
public class Turn {

    /**
     * The season.  Normally not distingished before 1600, then split
     * into SPRING and AUTUMN.
     */
    public static enum Season implements Named {
        YEAR,
        SPRING,
        AUTUMN;


        /**
         * Get the suffix for a save game name for a turn with this season.
         *
         * @return The save game suffix.
         */
        public String getSaveGameSeasonSuffix() {
            switch (this) {
            case YEAR:
                break;
            case SPRING:
                return "_1_" + Messages.getName(this);
            case AUTUMN:
                return "_2_" + Messages.getName(this);
            }
            return "";
        }

        /**
         * Get the stem key for this season.
         *
         * @return The stem key.
         */
        private String getKey() {
            return "season." + getEnumKey(this);
        }

        // Interface Named

        /**
         * {@inheritDoc}
         */
        @Override
        public String getNameKey() {
            return Messages.nameKey("model." + getKey());
        }
    }

    /** The starting year (1492 in Col1). */
    private static int startingYear;

    /** The year where the seasons split (1600 in Col1). */
    private static int seasonYear;


    /** The numerical value of the Turn, never less than one. */
    private int turn = 1;


    /**
     * Creates a new <code>Turn</code> instance.
     *
     * @param turn The numeric value of the turn.
     */
    public Turn(int turn) {
        this.turn = turn;
    }


    /**
     * Initialize the fundamental Turn constants.  Called from the spec
     * initialization when the values are available and checked.
     *
     * @param newStartingYear The starting year for the game.
     * @param newSeasonYear The year at which the seasons split.
     */
    public static synchronized void initialize(int newStartingYear,
                                               int newSeasonYear) {
        startingYear = newStartingYear;
        seasonYear = newSeasonYear;
    }
    

    /**
     * Converts an integer year to a turn-integer-value.
     * Allows for the season split.
     *
     * @param year A year.
     * @return The integer value of the corresponding turn.
     */
    public static int yearToTurn(int year) {
        return yearToTurn(year, Season.YEAR);
    }

    /**
     * Converts an integer year and specified season to a turn-integer-value.
     *
     * @param year an <code>int</code> value
     * @param season a <code>Season</code> value
     * @return The integer value of the corresponding turn.
     */
    public static int yearToTurn(int year, Season season) {
        int turn = 1;
        if (year >= getStartingYear()) {
            turn += year - getStartingYear();
            if (year >= getSeasonYear()) {
                turn += (year - getSeasonYear());
                if (season == Season.AUTUMN) {
                    turn++;
                }
            }
        }
        return turn;
    }

    /**
     * Increases the turn number by one.
     */
    public Turn next() {
        return new Turn(turn + 1);
    }

    /**
     * Gets the turn number.
     *
     * @return The number of turns.
     */
    public int getNumber() {
        return turn;
    }

    /**
     * Gets the year this turn is in.
     *
     * @return The calculated year based on the turn number.
     */
    public int getYear() {
        return getYear(turn);
    }

    /**
     * Gets the year the given turn is in.
     *
     * @return The calculated year based on the turn number.
     */
    public static int getYear(int turn) {
        int year = turn - 1 + getStartingYear();
        return (year < getSeasonYear()) ? year
            : getSeasonYear() + (year - getSeasonYear())/2;
    }

    /**
     * Gets the Season of the given Turn number.
     *
     * @param turn The turn number to calculate from.
     * @return The season corresponding to the turn number.
     */
    public static Season getSeason(int turn) {
        int year = turn - 1 + getStartingYear();
        return (year < getSeasonYear()) ? Season.YEAR
            : (year % 2 == 0) ? Season.SPRING : Season.AUTUMN;
    }

    /**
     * Gets the Season of this Turn.
     *
     * @return a <code>Season</code> value
     */
    public Season getSeason() {
        return getSeason(turn);
    }

    /**
     * Gets a localization template for this turn.
     *
     * @return A <code>StringTemplate</code> describing the turn.
     */
    public StringTemplate getLabel() {
        return getLabel(turn);
    }

    /**
     * Gets a localization template for a given turn.
     *
     * @param turn The integer value of the turn to describe.
     * @return A <code>StringTemplate</code> describing the turn.
     */
    public static StringTemplate getLabel(int turn) {
        Season season = getSeason(turn);
        StringTemplate t = StringTemplate.label("");
        t.add(season.getNameKey());
        if (season != Season.YEAR) t.addName(" ");
        t.addName(Integer.toString(getYear(turn)));
        return t;
    }

    /**
     * Gets the starting year.
     *
     * @return The numeric value of the starting year.
     */
    public static final int getStartingYear() {
        return startingYear;
    }

    /**
     * Gets the season year (the year the seasons split).
     *
     * @return The numeric value of the season year.
     */
    public static final int getSeasonYear() {
        return seasonYear;
    }

    /**
     * Is this turn the first one?
     *
     * @return True if this turn is the first turn.
     */
    public boolean isFirstTurn() {
        return turn == 1;
    }

    /**
     * Is this turn the season year?
     *
     * @return True if this turn is the season year.
     */
    public boolean isFirstSeasonTurn() {
        return turn == yearToTurn(getSeasonYear(), Season.SPRING);
    }

    /**
     * Gets a non-localized string representation of the given turn.
     *
     * @return A string with the format: "<i>season year</i>", such as
     *     "SPRING 1602", "YEAR 1503"...
     */
    public static String toString(int turn) {
        return getSeason(turn) + " " + Integer.toString(getYear(turn));
    }

    /**
     * Gets a string describing the number of turns.
     *
     * @param turns The number of turns.
     * @return A descriptive string.
     */
    public static String getTurnsText(int turns) {
        return (turns == FreeColObject.UNDEFINED)
            ? Messages.message("notApplicable")
            : (turns >= 0) ? Integer.toString(turns)
            : ">" + Integer.toString(-turns);
    }


    // Override Object

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (o instanceof Turn) {
            return this.turn == ((Turn)o).turn;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return turn;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return toString(turn);
    }
}
