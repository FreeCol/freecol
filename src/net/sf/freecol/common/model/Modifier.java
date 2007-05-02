
package net.sf.freecol.common.model;


/**
 * The <code>Modifier</code> class encapsulates a bonus or penalty
 * that can be applied to any action within the game, most obviously
 * combat.
 */
public final class Modifier {

    public static final  String  COPYRIGHT = "Copyright (C) 2003-2007 The FreeCol Team";
    public static final  String  LICENSE   = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    
    // the ID of the modifier, used to look up name, etc.
    public final String id;
    // whether this modifier is additive or multiplicative
    private final boolean isAdditive;
    // the addend of an additive modifier, defaults to zero
    public final float addend;
    // the factor of a multiplicative modifier, defaults to zero
    public final float factor;


    /**
     * Creates a new <code>Modifier</code> instance.
     *
     * @param id a <code>String</code> value
     * @param value an <code>float</code> value
     * @param additive whether this modifier is additive
     */
    public Modifier(String id, float value, boolean additive) {
        this.id = id;
        this.isAdditive = additive;
        if (additive) {
            this.addend = value;
            this.factor = 1;
        } else {
            this.factor = value;
            this.addend = 0;
        }
    }

    /**
     * Returns a new additive <code>Modifier</code> instance.
     *
     * @param id a <code>String</code> value
     * @param addend a <code>float</code> value
     */
    public static Modifier createAdditiveModifier(String id, float addend) {
        return new Modifier(id, addend, true);
    }

    /**
     * Returns a new multiplicative <code>Modifier</code> instance.
     *
     * @param id a <code>String</code> value
     * @param factor an <code>float</code> value
     */
    public static Modifier createMultiplicativeModifier(String id, float factor) {
        return new Modifier(id, factor, false);
    }



    /**
     * Return a formatted string appropriate for the value this
     * modifier represents.
     *
     * @return a <code>String</code> value
     */
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

}
