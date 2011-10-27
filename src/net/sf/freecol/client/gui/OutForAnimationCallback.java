package net.sf.freecol.client.gui;

import javax.swing.JLabel;

/**
 * A callback interface for {@link MapViewer#executeWithUnitOutForAnimation(net.sf.freecol.common.model.Unit, net.sf.freecol.common.model.Tile, OutForAnimationCallback)}.
 */
public interface OutForAnimationCallback {

    /**
     * The code to be executed when a unit is out for animation.
     * @param unitLabel A <code>JLabel</code> with an image of
     *      the unit provided as a parameter to
     *      {@link MapViewer#executeWithUnitOutForAnimation(net.sf.freecol.common.model.Unit, net.sf.freecol.common.model.Tile, OutForAnimationCallback)}.
     */
    public void executeWithUnitOutForAnimation(JLabel unitLabel);
}
