package net.sf.freecol.client.gui.animation;

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.common.io.sza.SimpleZippedAnimation;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
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
     * @param direction The direction to move the unit.
     */
    public static void unitMove(Canvas canvas, Unit unit, Direction direction) {
        new UnitMoveAnimation(canvas, unit, direction).animate();
    }
    
    /**
     * Animates a unit move.
     * 
     * @param canvas The canvas where the animation should be drawn.
     * @param unit The unit to be animated.
     * @param destination The destination tile for the unit.
     */
    public static void unitMove(Canvas canvas, Unit unit, Tile destination) {
        new UnitMoveAnimation(canvas, unit, destination).animate();
    }
    
    /**
     * Animates a unit attack.
     * 
     * @param canvas The canvas where the animation should be drawn.
     * @param unit The attacking unit.
     * @param direction The direction of the attack
     */
    public static void unitAttack(Canvas canvas, Unit unit, Direction direction) {
        final SimpleZippedAnimation animation = getAnimation(canvas, unit, direction, "attack");
        if (animation != null) {
            new UnitImageAnimation(canvas, unit, animation, direction).animate();
        }
    }
    
    private static SimpleZippedAnimation getAnimation(Canvas canvas, Unit unit, Direction direction, String animation) {
        final GUI gui = canvas.getGUI();
        final float scalingFactor = gui.getImageLibrary().getScalingFactor();
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
