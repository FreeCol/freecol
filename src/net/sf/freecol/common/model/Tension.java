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

public class Tension {

    /**
    * Constants for adding to the tension levels.
    */
    // Unit destroyed, etc
    public static final int TENSION_ADD_MINOR = 100;
    // Unit destroyed in a Settlement, etc
    public static final int TENSION_ADD_NORMAL = 200;
    // Unit destroyed in a capital, etc
    public static final int TENSION_ADD_MAJOR = 300;
    // Grab land without paying
    public static final int TENSION_ADD_LAND_TAKEN = 200;
    // Unit destroyed
    public static final int TENSION_ADD_UNIT_DESTROYED = 400;
    // Settlement attacked
    public static final int TENSION_ADD_SETTLEMENT_ATTACKED = 500;
    // Capital attacked
    public static final int TENSION_ADD_CAPITAL_ATTACKED = 600;

    /** Tension modification to be used when war is declared from a peaceful state. */
    public static final int TENSION_ADD_DECLARE_WAR_FROM_PEACE = 1000;

    /** Tension modification to be used when war is declared from a cease fire. */
    public static final int TENSION_ADD_DECLARE_WAR_FROM_CEASE_FIRE = 750;
    
    /** Tension modification to be used when a peace treaty is signed. */
    public static final int PEACE_TREATY_MODIFIER = -250;

    /** Tension modification to be used when a cease-fire treaty is signed. */
    public static final int CEASE_FIRE_MODIFIER = -250;
    
    /** Tension modification to be used when an alliance treaty is signed. */
    public static final int ALLIANCE_MODIFIER = -500;
    
    /** 
     * Constants for describing alarm levels.
     */
    public static enum Level { 
        HAPPY(100),
        CONTENT(600), 
        DISPLEASED(700),
        ANGRY(800), 
        HATEFUL(1000);

        private int limit;

        Level(int limit) {
            this.limit = limit;
        }

        public int getLimit() {
            return limit;
        }
    }
    
    static int SURRENDED = (Level.CONTENT.limit + Level.HAPPY.limit) / 2;
    private int value;

    /**
     * Constructor.
     */
    public Tension() {
        setValue(Level.HAPPY.getLimit());
    }

    public Tension(int newTension) {
        setValue(newTension);
    }

    /**
     * Returns the current tension value.
     * @return The value of this <code>Tension</code>.
     */
    public int getValue() {
        return this.value;
    }

    /**
     * Sets the current tension value.
     * @param newValue The new value of the tension.
     */
    public void setValue(int newValue) {
        if (newValue < 0) {
            value = 0;
        } else if (newValue > Level.HATEFUL.getLimit()) {
            value = Level.HATEFUL.getLimit();
        } else {
            value = newValue;
        }
    }

    /** 
     * Returns the current tension level.
     * @return The current level.
     */
    public Level getLevel() {
        for (Level level : Level.values()) {
            if (value <= level.getLimit())
                return level;
        }
        return Level.HATEFUL;
   }

    public void setLevel(Level level) {
        if (level != getLevel()) {
            setValue(level.getLimit());
        }
    }

    /**
     * Modifies the tension by the given amount.
     *
     * @param newTension The amount to modify tension by.
     */
    public void modify(int newTension) {
        setValue(value + newTension);
    }

    /** 
     * Returns the current tension level as a string.
     * @return A <code>String</code>-representation of the
     *      current tension level.
     */
    public String toString() {
        return getLevel().toString().toLowerCase();
    }    

}



