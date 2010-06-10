/**
 *  Copyright (C) 2002-2007  The FreeCol Team
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

    public static final int STARTING_YEAR = 1492;
    public static final int SEASON_YEAR = 1600;
    private static final int OFFSET = SEASON_YEAR - STARTING_YEAR - 1;

    /**
     * The numerical value of the Turn, never less than one.
     *
     */
    private int turn;

    public Turn(int turn) {
        this.turn = turn;
    }

    
    /**
     * Increases the turn number by one.
     */
    public Turn next() {
        return new Turn(turn++);
    }


    /**
     * Gets the turn number.
     * @return The number of turns.
     */
    public int getNumber() {
        return turn;
    }

    
    /**
     * Gets the age.
     * 
     * @return The age:
     *       <br>
     *       <br>1 - if before {@link #SEASON_YEAR}
     *       <br>2 - if between 1600 and 1700.
     *       <br>3 - if after 1700.
     */
    public int getAge() {
        if (getYear() < SEASON_YEAR) {
            return 1;
        } else if (getYear() < 1700) {
            return 2;
        } else {
            return 3;
        }
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

    
    /**
     * Gets the year the given turn is in.
     * @return The calculated year based on the turn
     *       number.
     */
    public static int getYear(int turn) {
        int c = turn - OFFSET;
        if (c < 0) {
            return STARTING_YEAR + turn - 1;
        } else {
            return SEASON_YEAR + c/2 - 1;
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
        int c = turn - OFFSET;
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

}
