package net.sf.freecol.client.gui;

import javax.swing.JLabel;

/**
 * A callback interface for {@link GUI#executeWithUnitOutForAnimation(net.sf.freecol.common.model.Unit, OutForAnimationCallback)}.
 */
public interface OutForAnimationCallback {

    /**
     * The code to be executed when a unit is out for animation.
     * @param unitLabel A <code>JLabel</code> with an image of
     *      the unit provided as a parameter to
     *      {@link GUI#executeWithUnitOutForAnimation(net.sf.freecol.common.model.Unit, OutForAnimationCallback)}.
     */
    public void executeWithUnitOutForAnimation(JLabel unitLabel);
}
