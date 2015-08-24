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
import net.sf.freecol.common.i18n.NameCache;


/**
 * Represents a given turn in the game.
 */
public class Turn {

    /** The starting year (1492 in Col1). */
    private static int startingYear = 1492;

    /** The year where the seasons split (1600 in Col1). */
    private static int seasonYear = 1600;

    /** The number of seasons. */
    private static int seasonNumber = 2;


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
     * Initialize the fundamental Turn year constants.  Called from
     * the spec cleanup when the values are available and checked.
     *
     * @param newStartingYear The starting year for the game.
     * @param newSeasonYear The year at which the seasons split.
     * @param newSeasonNumber The number of seasons in the year.
     */
    public static synchronized void initialize(int newStartingYear,
                                               int newSeasonYear,
                                               int newSeasonNumber) {
        startingYear = newStartingYear;
        seasonYear = newSeasonYear;
        seasonNumber = newSeasonNumber;
    }
    
    /**
     * Gets the starting year.
     *
     * @return The numeric value of the starting year.
     */
    public static final synchronized int getStartingYear() {
        return startingYear;
    }

    /**
     * Gets the season year (the year the seasons split).
     *
     * @return The numeric value of the season year.
     */
    public static final synchronized int getSeasonYear() {
        return seasonYear;
    }

    /**
     * Gets the number of seasons.
     *
     * @return The number of seasons.
     */
    public static final synchronized int getSeasonNumber() {
        return seasonNumber;
    }

    /**
     * Converts a year to a turn number.
     *
     * @param year The year to convert.
     * @return The integer value of the corresponding turn.
     */
    public static int yearToTurn(int year) {
        return yearToTurn(year, 0);
    }

    /**
     * Converts an integer year and specified season to a turn-integer-value.
     *
     * @param year The year to convert.
     * @param season The season index.
     * @return The integer value of the corresponding turn.
     */
    public static int yearToTurn(int year, int season) {
        int ret = 1, startingYear = getStartingYear();
        if (year >= startingYear) {
            ret += year - startingYear;
            int seasonYear = getSeasonYear();
            if (year >= seasonYear) {
                ret += (year - seasonYear) * (getSeasonNumber() - 1) + season;
            }
        }
        return ret;
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
        int year = turn - 1 + getStartingYear(), seasonYear = getSeasonYear();
        return (year < seasonYear) ? year
            : seasonYear + (year - seasonYear) / getSeasonNumber();
    }

    /**
     * Gets the season index of the given turn number.
     *
     * @param turn The turn number to calculate from.
     * @return The season index corresponding to the turn number or negative
     *     if before the season year.
     */
    public static int getSeason(int turn) {
        int year = turn - 1 + getStartingYear();
        return (year < getSeasonYear()) ? -1
            : (year - seasonYear) % getSeasonNumber();
    }

    /**
     * Gets the season index of this turn.
     *
     * @return The season index corresponding to the current turn
     *     number, or negative if before the season year.
     */
    public int getSeason() {
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
        int season = getSeason(turn);
        StringTemplate t = StringTemplate.label("");
        if (season >= 0) {
            t.addName(NameCache.getSeasonName(season));
            t.addName(" ");
        }
        t.addName(Integer.toString(getYear(turn)));
        return t;
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
        return turn == yearToTurn(getSeasonYear());
    }

    /**
     * Get the suffix for a save game name for this turn.
     *
     * @return The save game suffix.
     */
    public String getSaveGameSuffix() {
        final int season = getSeason();
        String result = String.valueOf(getYear());
        if (season >= 0) {
            final int SeasonNumberDigits = String.valueOf(getSeasonNumber()).length(); // for leading zeroes
            result += "_" + String.format("%0"+String.valueOf(SeasonNumberDigits)+"d", season+1)
                + "_" + NameCache.getSeasonName(season);
        }
        return result;
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
            : ">" + Integer.toString(-turns - 1);
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
        return String.valueOf(turn);
    }
}
