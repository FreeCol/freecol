package net.sf.freecol.client.gui.animation;

import net.sf.freecol.client.ClientOptions;
import net.sf.freecol.client.FreeColClient;
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
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param gui The <code>GUI</code> to display on.
     * @param unit The <code>Unit</code> to be animated.
     * @param source The source <code>Tile</code> for the unit.
     * @param destination The destination <code>Tile</code> for the unit.
     */
    public static void unitMove(FreeColClient freeColClient, GUI gui, Unit unit,
                                Tile source, Tile destination) {
        new UnitMoveAnimation(freeColClient, gui, unit, source, destination)
            .animate();
    }
    
    /**
     * Animates a unit attack.
     * 
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param gui The <code>GUI</code> to display on.
     * @param attacker The <code>Unit</code> that is attacking.
     * @param defender The <code>Unit</code> that is defending.
     * @param success Did the attack succeed?
     */
    public static void unitAttack(FreeColClient freeColClient, GUI gui,
                                  Unit attacker, Unit defender, 
                                  boolean success) {
        new UnitAttackAnimation(freeColClient, gui, attacker, defender, success)
            .animate();
    }

    /**
     * Common utility routine to retrieve animation speed.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param unit The <code>Unit</code> to be animated.
     * @return The animation speed.
     */
    public static int getAnimationSpeed(FreeColClient freeColClient, 
                                        Unit unit) {
        String key = (freeColClient.getMyPlayer() == unit.getOwner())
            ? ClientOptions.MOVE_ANIMATION_SPEED
            : ClientOptions.ENEMY_MOVE_ANIMATION_SPEED;
        return freeColClient.getClientOptions().getInteger(key);
    }
}
