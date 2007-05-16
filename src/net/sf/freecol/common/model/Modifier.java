
package net.sf.freecol.common.model;


/**
 * The <code>Modifier</code> class encapsulates a bonus or penalty
 * that can be applied to any action within the game, most obviously
 * combat.
 */
public final class Modifier {

    public static final String  COPYRIGHT = "Copyright (C) 2003-2007 The FreeCol Team";
    public static final String  LICENSE   = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    public static final int ADDITIVE = 0;
    public static final int MULTIPLICATIVE = 1;
    public static final int PERCENTAGE = 2;
    
    /**
     * The ID of the modifier, used to look up name, etc.
     */
    private String id;

    /**
     * The type of this modifier
     */
    private int type;

    /**
     * The value of this modifier
     */
    private float value;



    /**
     * Creates a new <code>Modifier</code> instance.
     *
     * @param id a <code>String</code> value
     * @param value an <code>float</code> value
     * @param additive whether this modifier is additive
     */
    public Modifier(String id, float value, int type) {
        this.id = id;
        this.value = value;
        this.type = type;
    }

    /**
     * Get the <code>Id</code> value.
     *
     * @return a <code>String</code> value
     */
    public String getId() {
        return id;
    }

    /**
     * Set the <code>Id</code> value.
     *
     * @param newId The new Id value.
     */
    public void setId(final String newId) {
        this.id = newId;
    }

    /**
     * Get the <code>Type</code> value.
     *
     * @return an <code>int</code> value
     */
    public int getType() {
        return type;
    }

    /**
     * Set the <code>Type</code> value.
     *
     * @param newType The new Type value.
     */
    public void setType(final int newType) {
        this.type = newType;
    }

    /**
     * Get the <code>Value</code> value.
     *
     * @return a <code>float</code> value
     */
    public float getValue() {
        return value;
    }

    /**
     * Set the <code>Value</code> value.
     *
     * @param newValue The new Value value.
     */
    public void setValue(final float newValue) {
        this.value = newValue;
    }

    /**
     * Combines this modifier with another.
     *
     * @param otherModifier a <code>Modifier</code> value
     */
    public void combine(Modifier otherModifier) {
        switch(otherModifier.getType()) {
        case ADDITIVE:
            value += otherModifier.getValue();
            return;
        case MULTIPLICATIVE:
            value *= otherModifier.getValue();
            return;
        case PERCENTAGE:
            value += (value * otherModifier.getValue()) / 100;
            return;
        }
    }
 
     /**
     * Return a formatted string appropriate for the value this
     * modifier represents.
     *
     * @return a <code>String</code> value
     */
    /*
    public String getFormattedResult() {
        if (isAdditive) {
            if (addend == Float.MIN_VALUE) {
                return "?";
            } else if (addend > 0) {
                return "+" + String.valueOf(addend);
            } else {
                return String.valueOf(addend);
            }
        } else {
            float modifier = (100 * factor) - 100;
            if (modifier > 0) {
                return "+" + String.valueOf(modifier) + "%";
            } else {
                return String.valueOf(modifier) + "%";
            }
        }
    }
    */

}
