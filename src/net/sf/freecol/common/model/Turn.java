
package net.sf.freecol.common.model;

import java.util.logging.Logger;


/**
* Represents a given turn in the game.
*/
public class Turn {
    public static final String  COPYRIGHT = "Copyright (C) 2003 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    private static final Logger logger = Logger.getLogger(Turn.class.getName());


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
    */
    public int getNumber() {
        return turn;
    }

    
    /**
    * Sets the turn number.
    */
    public void setNumber(int turn) {
        this.turn = turn;
    }


    /**
    * Checks if this turn is equal to another turn.
    */
    public boolean equals(Object o) {
        if (!(o instanceof Turn)) {
            return false;
        } else {
            return (getNumber() == ((Turn) o).getNumber());
        }
    }


    /**
    * Returns a string representation of this turn.
    * @return A string with the format: "<i>[season] year</i>".
    *         Examples: "Spring 1602", "1503"...
    */
    public String toString() {
        if (STARTING_YEAR + turn - 1 < SEASON_YEAR) {
            return Integer.toString(STARTING_YEAR + turn - 1);
        } else {
            int c = turn - (SEASON_YEAR - STARTING_YEAR - 1);
            return ((c%2==0) ? " Spring" : " Autumn") + Integer.toString(SEASON_YEAR + c/2 - 1);
        }
    }
}
