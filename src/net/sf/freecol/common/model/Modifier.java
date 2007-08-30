
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
     * @param type the type of the modifier
     */
    public Modifier(String id, float value, int type) {
        this.id = id;
        this.value = value;
        this.type = type;
    }

    /**
     * Creates a new <code>Modifier</code> instance.
     *
     * @param id a <code>String</code> value
     * @param value an <code>float</code> value
     * @param type the type of the modifier
     */
    public Modifier(String id, float value, String type) {
        this.id = id;
        this.value = value;
        if ("additive".equals(type)) {
            this.type = ADDITIVE;
        } else if ("multiplicative".equals(type)) {
            this.type = MULTIPLICATIVE;
        } else if ("percentage".equals(type)) {
            this.type = PERCENTAGE;
        }
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
     * Applies this modifier to a number.
     *
     */
    public float applyTo(float number) {
        switch(getType()) {
        case ADDITIVE:
            return number + value;
        case MULTIPLICATIVE:
            return number * value;
        case PERCENTAGE:
            return number + (number * value) / 100;
        default:
            return number;
        }
    }
}
