package net.sf.freecol.client.gui.animation;

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Map.Direction;

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
    
}
