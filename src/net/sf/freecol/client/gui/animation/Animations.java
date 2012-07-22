package net.sf.freecol.client.gui.animation;

import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;


/**
 * A facade for animations. 
 */
public class Animations {

    /**
     * Animates a unit move.
     *
     * @param gui The <code>GUI</code> to display on.
     * @param unit The <code>Unit</code> to be animated.
     * @param source The source <code>Tile</code> for the unit.
     * @param destination The destination <code>Tile</code> for the unit.
     */
    public static void unitMove(GUI gui, Unit unit,
                                Tile source, Tile destination) {
        new UnitMoveAnimation(gui, unit, source, destination)
            .animate();
    }
    
    /**
     * Animates a unit attack.
     * 
     * @param gui The <code>GUI</code> to display on.
     * @param attacker The <code>Unit</code> that is attacking.
     * @param defender The <code>Unit</code> that is defending.
     * @param success Did the attack succeed?
     */
    public static void unitAttack(GUI gui,
                                  Unit attacker, Unit defender, 
                                  boolean success) {
        new UnitAttackAnimation(gui, attacker, defender, success)
            .animate();
    }

}
