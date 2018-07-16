/**
 *  Copyright (C) 2002-2018   The FreeCol Team
 *
 *  This file is part of FreeCol.
 *
 *  FreeCol is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  FreeCol is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FreeCol.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.sf.freecol.client.gui.animation;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.logging.Logger;

import javax.swing.JLabel;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.control.FreeColClientHolder;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.OutForAnimationCallback;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.util.Utils;


/**
 * Class for the animation of units movement.
 */
final class UnitMoveAnimation extends FreeColClientHolder
    implements OutForAnimationCallback {

    private static final Logger logger = Logger.getLogger(UnitAttackAnimation.class.getName());

    /**
     * Display delay between one frame and another, in milliseconds.
     * 33ms == 30 fps
     */
    private static final int ANIMATION_DELAY = 33;

    private final Unit unit;
    private final Tile sourceTile;
    private final Tile destinationTile;
    private final int speed;


    /**
     * Constructor
     *
     * @param freeColClient The enclosing {@code FreeColClient}.
     * @param unit The {@code Unit} to be animated.
     * @param sourceTile The {@code Tile} the unit is moving from.
     * @param destinationTile The {@code Tile} the unit is moving to.
     */
    public UnitMoveAnimation(FreeColClient freeColClient, Unit unit,
                             Tile sourceTile, Tile destinationTile) {
        super(freeColClient);

        this.unit = unit;
        this.sourceTile = sourceTile;
        this.destinationTile = destinationTile;
        this.speed = freeColClient.getAnimationSpeed(unit.getOwner());
    }
    

    /**
     * Do the animation.
     *
     * @return True if the required animations were found and launched,
     *     false on error.
     */
    public boolean animate() {
        if (this.speed <= 0) {
            logger.warning("Failed move animation with zero speed: "
                + this.unit);
            return false;
        }

        getGUI().executeWithUnitOutForAnimation(this.unit, this.sourceTile,
                                                this);
        return true;
    }


    // Interface OutForAnimationCallback

    /**
     * {@inheritDoc}
     */
    @Override
    public void executeWithUnitOutForAnimation(JLabel unitLabel) {
        final GUI gui = getGUI();
        final float scale = gui.getAnimationScale();
        final int movementRatio = (int)(Math.pow(2, this.speed + 1) * scale);
        final Rectangle r1 = gui.getAnimationTileBounds(this.sourceTile);
        final Rectangle r2 = gui.getAnimationTileBounds(this.destinationTile);
        final Rectangle bounds = r1.union(r2);
        final double xratio = ImageLibrary.TILE_SIZE.width
            / (double)ImageLibrary.TILE_SIZE.height;

        // Tile positions should be valid at this point.
        final Point srcP = gui.getAnimationTilePosition(this.sourceTile);
        if (srcP == null) {
            logger.warning("Failed move animation for " + this.unit
                + " at source tile: " + this.sourceTile);
            return;
        }
        final Point dstP = gui.getAnimationTilePosition(this.destinationTile);
        if (dstP == null) {
            logger.warning("Failed move animation for " + this.unit
                + " at destination tile: " + this.destinationTile);
            return;
        }
            
        final int labelWidth = unitLabel.getWidth();
        final int labelHeight = unitLabel.getHeight();
        final Point srcPoint = gui.getAnimationPosition(labelWidth, labelHeight, srcP);
        final Point dstPoint = gui.getAnimationPosition(labelWidth, labelHeight, dstP);
        final int stepX = (srcPoint.getX() == dstPoint.getX()) ? 0
            : (srcPoint.getX() > dstPoint.getX()) ? -1 : 1;
        final int stepY = (srcPoint.getY() == dstPoint.getY()) ? 0
            : (srcPoint.getY() > dstPoint.getY()) ? -1 : 1;

        int dropFrames = 0;
        Point point = srcPoint;
        while (!point.equals(dstPoint)) {
            long time = System.currentTimeMillis();
            point.x += stepX * xratio * movementRatio;
            point.y += stepY * movementRatio;
            if ((stepX < 0 && point.x < dstPoint.x)
                || (stepX > 0 && point.x > dstPoint.x)) {
                point.x = dstPoint.x;
            }
            if ((stepY < 0 && point.y < dstPoint.y)
                || (stepY > 0 && point.y > dstPoint.y)) {
                point.y = dstPoint.y;
            }
            if (dropFrames <= 0) {
                unitLabel.setLocation(point);
                gui.paintImmediately(bounds);
                int timeTaken = (int)(System.currentTimeMillis()-time);
                final int waitTime = ANIMATION_DELAY - timeTaken;
                if (waitTime > 0) {
                    Utils.delay(waitTime, "Animation interrupted.");
                    dropFrames = 0;
                } else {
                    dropFrames = timeTaken / ANIMATION_DELAY - 1;
                }
            } else {
                dropFrames--;
            }
        }
        gui.refresh();
    }
}
