package net.sf.freecol.client.gui.animation;

import javax.swing.JLabel;

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.OutForAnimationCallback;
import net.sf.freecol.common.io.sza.SimpleZippedAnimation;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.CombatModel.CombatResultType;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.Unit.Role;
import net.sf.freecol.common.resources.ResourceManager;

/**
 * A facade for animations. 
 */
public class Animations {

    /**
     * Animates a unit move.
     *
     * @param canvas The canvas where the animation should be drawn.
     * @param unit The unit to be animated.
     * @param source The source tile for the unit.
     * @param destination The destination tile for the unit.
     */
    public static void unitMove(Canvas canvas, Unit unit, Tile source,
                                Tile destination) {
        new UnitMoveAnimation(canvas, unit, source, destination).animate();
    }
    
    /**
     * Animates a unit attack.
     * 
     * @param canvas The canvas where the animation should be drawn.
     * @param attacker an <code>Unit</code> value
     * @param defender The defending unit.
     * @param result a <code>CombatResultType</code> value
     */
    public static void unitAttack(final Canvas canvas,
            final Unit attacker,
            final Unit defender,
            final CombatResultType result) {
        final GUI gui = canvas.getGUI();
        final Map map = attacker.getGame().getMap();
        final Direction direction = map.getDirection(attacker.getTile(), defender.getTile());
        final SimpleZippedAnimation attackerAnimation = getAnimation(canvas, attacker, direction, "attack");
        if (attackerAnimation != null) {
            new UnitImageAnimation(canvas, attacker, attackerAnimation, direction).animate();
        }
        if (!result.isSuccess()) {
            final SimpleZippedAnimation defenderAnimation = getAnimation(canvas, defender, direction.getReverseDirection(), "attack");
            if (defenderAnimation != null) {
                new UnitImageAnimation(canvas, defender, defenderAnimation, direction).animate();
            }
        }
    }
    
    private static SimpleZippedAnimation getAnimation(Canvas canvas, Unit unit, Direction direction, String animation) {
        final GUI gui = canvas.getGUI();
        final float scalingFactor = gui.getMapScale();
        final String roleStr = (unit.getRole() != Role.DEFAULT) ? "." + unit.getRole().getId() : "";
        final String startStr = unit.getType().getId() + roleStr + "." + animation + ".";
        final String specialId = startStr + direction.toString().toLowerCase() + ".animation";
        final SimpleZippedAnimation specialSza = ResourceManager.getSimpleZippedAnimation(specialId, scalingFactor);
        if (specialSza != null) {
            return specialSza;
        } else {
            final String genericDirection;
            switch (direction) {
            case SW:
            case W:
            case NW:
                genericDirection = "w";
                break;
            default:
                genericDirection = "e";
                break;    
            }
            final String genericId = startStr + genericDirection + ".animation";
            return ResourceManager.getSimpleZippedAnimation(genericId, scalingFactor);
        }
    }
}
