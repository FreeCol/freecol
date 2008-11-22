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

import net.sf.freecol.client.gui.i18n.Messages;


/**
 * Represents a given turn in the game.
 */
public class Turn {

    public static final int STARTING_YEAR = 1492;
    public static final int SEASON_YEAR = 1600;

    private int turn;

    public Turn(int turn) {
        this.turn = turn;
    }

    
    /**
     * Increases the turn number by one.
     */
    public void increase() {
        turn++;
    }


    /**
     * Gets the turn number.
     * @return The number of turns.
     */
    public int getNumber() {
        return turn;
    }

    
    /**
     * Sets the turn number.
     * @param turn The number of turns.
     */
    public void setNumber(int turn) {
        this.turn = turn;
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

        if ( ! (o instanceof Turn) ) { return false; }

        return turn == ((Turn) o).turn;
    }

    
    /**
     * Gets the year the given turn is in.
     * @return The calculated year based on the turn
     *       number.
     */
    public static int getYear(int turn) {
        if (STARTING_YEAR + turn - 1 < SEASON_YEAR) {
            return STARTING_YEAR + turn - 1;
        }

        int c = turn - (SEASON_YEAR - STARTING_YEAR - 1);
        return SEASON_YEAR + c/2 - 1;
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
     * Returns a string representation of the given turn.
     * @return A string with the format: "<i>[season] year</i>".
     *         Examples: "Spring 1602", "1503"...
     */
    public static String toString(int turn) {
        if (STARTING_YEAR + turn - 1 < SEASON_YEAR) {
            return Integer.toString(STARTING_YEAR + turn - 1);
        }

        int c = turn - (SEASON_YEAR - STARTING_YEAR - 1);
        return ((c%2==0) ? Messages.message("spring") : Messages.message("autumn"))
            + " " + Integer.toString(SEASON_YEAR + c/2 - 1);
    }

    /**
     * Returns a string representation of this turn suitable for
     * savegame files.
     * @return A string with the format: "<i>[season] year</i>".
     *         Examples: "1602_1_Spring", "1503"...
     */
    public String toSaveGameString() {
        if (STARTING_YEAR + turn - 1 < SEASON_YEAR) {
            return Integer.toString(STARTING_YEAR + turn - 1);
        }

        int c = turn - (SEASON_YEAR - STARTING_YEAR - 1);
        String result = Integer.toString(SEASON_YEAR + c/2 - 1);
        if (c % 2 == 0) {
            result += "_1_" + Messages.message("spring");
        } else {
            result += "_2_" + Messages.message("autumn");
        }
        return result;
    }
}
