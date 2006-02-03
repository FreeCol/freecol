
package net.sf.freecol.common.model;


import net.sf.freecol.client.gui.i18n.Messages;


public class Tension {
    public static final String  COPYRIGHT = "Copyright (C) 2003-2006 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    /** 
     * Constants for describing alarm levels.
     */
    public static final int HAPPY = 0,
        CONTENT = 1,
        DISPLEASED = 2,
        ANGRY = 3,
        HATEFUL = 4,
        NUMBER_OF_LEVELS = 5;

    public static final String[] level = {
        "happy",
        "displeased",
        "content",
        "angry",
        "hateful"
    };

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
    public static final int TENSION_ADD_LAND_TAKEN = 350;
    // Unit destroyed
    public static final int TENSION_ADD_UNIT_DESTROYED = 400;
    // Settlement attacked
    public static final int TENSION_ADD_SETTLEMENT_ATTACKED = 500;
    // Capital attacked
    public static final int TENSION_ADD_CAPITAL_ATTACKED = 600;
    
    /** The AI player is happy if <code>tension <= TENSION_HAPPY</code>. */
    public static final int TENSION_HAPPY = 100;

    /** The AI player is content if <code>tension <= TENSION_CONTENT && tension > TENSION_HAPPY</code>. */
    public static final int TENSION_CONTENT = 600;

    /** The AI player is displeased if <code>tension <= TENSION_DISPLEASED && tension > TENSION_CONTENT</code>. */
    public static final int TENSION_DISPLEASED = 700;

    /** The AI player is angry if <code>tension <= TENSION_ANGRY && tension > TENSION_DISPLEASED</code>. */
    public static final int TENSION_ANGRY = 800;

    /** The AI player is hateful if <code>tension > TENSION_ANGRY</code>. */
    public static final int TENSION_HATEFUL = 1000;

    private int value;

    /**
     * Constructor.
     */
    public Tension() {
        setValue(TENSION_HAPPY);
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
        } else if (newValue > TENSION_HATEFUL) {
            value = TENSION_HATEFUL;
        } else {
            value = newValue;
        }
    }

    /** 
     * Returns the current tension level.
     * @return The current level.
     */
    public int getLevel() {
    if (value <= TENSION_HAPPY) {
        return HAPPY;
    } else if (value <= TENSION_CONTENT) {
        return CONTENT;
    } else if (value <= TENSION_DISPLEASED) {
        return DISPLEASED;
    } else if (value <= TENSION_ANGRY) {
        return ANGRY;
    } else {
        return HATEFUL;
    }
    }

    public void setLevel(int level) {
        if (level != getLevel()) {
            switch(level) {
            case HAPPY:
                setValue(TENSION_HAPPY);
            case CONTENT:
                setValue(TENSION_CONTENT);
            case DISPLEASED:
                setValue(TENSION_DISPLEASED);
            case ANGRY:
                setValue(TENSION_ANGRY);
            case HATEFUL:
                setValue(TENSION_HATEFUL);
            }
        }
    }

    /** 
     * Returns the current tension level as a string.
     * @return A <code>String</code>-representation of the
     *      current tension level.
     */
    public String getLevelAsString() {
    if (value <= TENSION_HAPPY) {
        return Messages.message(level[HAPPY]);
    } else if (value <= TENSION_CONTENT) {
        return Messages.message(level[CONTENT]);
    } else if (value <= TENSION_DISPLEASED) {
            return Messages.message(level[DISPLEASED]);
    } else if (value <= TENSION_ANGRY) {
            return Messages.message(level[ANGRY]);
    } else {
        return Messages.message(level[HATEFUL]);
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

}



