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
 * Represents a given turn in the game.
 */
public class Turn {

    public static enum Season { YEAR, SPRING, AUTUMN }

    /**
     * The numerical value of the Turn, never less than one.
     *
     */
    private int turn = 1;

    /**
     * The year in which the game starts.
     */
    private static int startingYear = 1492;

    /**
     * The first year in which there are two seasons.
     */
    private static int seasonYear = 1600;

    /**
     * The first years of the "ages" of the game, which are only used
     * for weighting {@link FoundingFather}s.
     */
    private static int[] ages = new int[] {
        1492, 1600, 1700
    };



    /**
     * Creates a new <code>Turn</code> instance.
     *
     * @param turn an <code>int</code> value
     */
    public Turn(int turn) {
        this.turn = turn;
    }

    /**
     * Describe <code>yearToTurn</code> method here.
     *
     * @param year an <code>int</code> value
     * @return an <code>int</code> value
     */
    public static int yearToTurn(int year) {
        return yearToTurn(year, Season.YEAR);
    }

    /**
     * Describe <code>yearToTurn</code> method here.
     *
     * @param year an <code>int</code> value
     * @param season a <code>Season</code> value
     * @return an <code>int</code> value
     */
    public static int yearToTurn(int year, Season season) {
        int turn = 1;
        if (year >= startingYear) {
            turn += year - startingYear;
            if (year >= seasonYear) {
                turn += (year - seasonYear);
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
     * @return The number of turns.
     */
    public int getNumber() {
        return turn;
    }


    /**
     * Describe <code>getAge</code> method here.
     *
     * @return an <code>int</code> value
     */
    public int getAge() {
        return getAge(getYear());
    }

    /**
     * Describe <code>getAge</code> method here.
     *
     * @param year an <code>int</code> value
     * @return an <code>int</code> value
     */
    public static int getAge(int year) {
        for (int index = 0; index < ages.length; index++) {
            if (year < ages[index]) {
                return index;
            }
        }
        return ages.length;
    }


    /**
     * Checks if this turn is equal to another turn.
     */
    public boolean equals(Object o) {
        if (o instanceof Turn) {
            return turn == ((Turn) o).turn;
        } else {
            return false;
        }
    }


    private static int getOffset(int turn) {
        return turn - (seasonYear - startingYear - 1);
    }


    /**
     * Gets the year the given turn is in.
     * @return The calculated year based on the turn
     *       number.
     */
    public static int getYear(int turn) {
        int c = getOffset(turn);
        if (c < 0) {
            return startingYear + turn - 1;
        } else {
            return seasonYear + c/2 - 1;
        }
    }


    /**
     * Gets the year this turn is in.
     * @return The calculated year based on the turn
     *       number.
     */
    public int getYear() {
        return getYear(turn);
    }


    /**
     * Returns a string representation of this turn.
     * @return A string with the format: "<i>[season] year</i>".
     *         Examples: "Spring 1602", "1503"...
     */
    public String toString() {
        return toString(turn);
    }

    /**
     * Returns a non-localized string representation of the given turn.
     * @return A string with the format: "<i>season year</i>".
     *         Examples: "SPRING 1602", "YEAR 1503"...
     */
    public static String toString(int turn) {
        return getSeason(turn).toString() + " " + Integer.toString(getYear(turn));
    }

    /**
     * Return the Season of the given Turn number.
     *
     * @param turn an <code>int</code> value
     * @return a <code>Season</code> value
     */
    public static Season getSeason(int turn) {
        int c = getOffset(turn);
        if (c <= 1) {
            return Season.YEAR;
        } else if (c % 2 == 0) {
            return Season.SPRING;
        } else {
            return Season.AUTUMN;
        }
    }

    /**
     * Return the Season of this Turn.
     *
     * @return a <code>Season</code> value
     */
    public Season getSeason() {
        return getSeason(turn);
    }


    /**
     * Describe <code>getLabel</code> method here.
     *
     * @return a <code>StringTemplate</code> value
     */
    public StringTemplate getLabel() {
        return getLabel(turn);
    }

    /**
     * Describe <code>getLabel</code> method here.
     *
     * @param turn an <code>int</code> value
     * @return a <code>StringTemplate</code> value
     */
    public static StringTemplate getLabel(int turn) {
        return StringTemplate.template("year." + getSeason(turn))
            .addAmount("%year%", getYear(turn));
    }

    /**
     * Get the <code>StartingYear</code> value.
     *
     * @return an <code>int</code> value
     */
    public static final int getStartingYear() {
        return startingYear;
    }

    /**
     * Set the <code>StartingYear</code> value.
     *
     * @param newStartingYear The new StartingYear value.
     */
    public static final void setStartingYear(final int newStartingYear) {
        startingYear = newStartingYear;
    }

    /**
     * Get the <code>SeasonYear</code> value.
     *
     * @return an <code>int</code> value
     */
    public static final int getSeasonYear() {
        return seasonYear;
    }

    /**
     * Set the <code>SeasonYear</code> value.
     *
     * @param newSeasonYear The new SeasonYear value.
     */
    public static final void setSeasonYear(final int newSeasonYear) {
        seasonYear = newSeasonYear;
    }

    /**
     * Describe <code>isFirstSeasonTurn</code> method here.
     *
     * @return a <code>boolean</code> value
     */
    public boolean isFirstSeasonTurn() {
        return turn == yearToTurn(seasonYear, Season.SPRING);
    }

    /**
     * Get the <code>Ages</code> value.
     *
     * @return an <code>int[]</code> value
     */
    public static final int[] getAges() {
        return ages;
    }

    /**
     * Set the <code>Ages</code> value.
     *
     * @param newAges The new Ages value.
     */
    public static final void setAges(final int[] newAges) {
        ages = newAges;
    }

}
