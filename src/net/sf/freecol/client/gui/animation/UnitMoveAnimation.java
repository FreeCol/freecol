/**
 *  Copyright (C) 2002-2022   The FreeCol Team
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

import static net.sf.freecol.common.util.CollectionUtils.makeUnmodifiableList;
import static net.sf.freecol.common.util.Utils.delay;
import static net.sf.freecol.common.util.Utils.now;

import java.awt.Point;
import java.util.logging.Logger;

import javax.swing.JLabel;

import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;


/**
 * Class for the animation of units movement.
 */
final class UnitMoveAnimation extends Animation {

    private static final Logger logger = Logger.getLogger(UnitMoveAnimation.class.getName());

    /**
     * Display delay between one frame and another, in milliseconds.
     * 33ms == 30 fps
     */
    private static final long ANIMATION_DELAY = 33L;

    /** The animation speed client option. */
    private final int speed;

    /** The image scale. */
    private final float scale;


    /**
     * Build a new movement animation.
     *
     * @param unit The {@code Unit} to be animated.
     * @param sourceTile The {@code Tile} the unit is moving from.
     * @param destinationTile The {@code Tile} the unit is moving to.
     * @param speed The animation speed.
     * @param scale The scale factor for the unit image.
     */
    public UnitMoveAnimation(Unit unit,
                             Tile sourceTile, Tile destinationTile,
                             int speed, float scale) {
        super(unit, makeUnmodifiableList(sourceTile, destinationTile));

        this.speed = speed;
        this.scale = scale;
    }
    

    // Implement Animation

    /**
     * {@inheritDoc}
     */
    @Override
    public void executeWithLabel(JLabel unitLabel,
                                 Animations.Procedure paintCallback) {
        final int movementRatio = (int)(Math.pow(2, this.speed + 1)
            * this.scale);
        final double xratio = ImageLibrary.TILE_SIZE.width
            / (double)ImageLibrary.TILE_SIZE.height;
        final Point srcPoint = this.points.get(0);
        final Point dstPoint = this.points.get(1);
        final int stepX = (int)(Math.signum(dstPoint.getX() - srcPoint.getX())
            * xratio * movementRatio);
        final int stepY = (int)(Math.signum(dstPoint.getY() - srcPoint.getY())
            * movementRatio);

        Point point = srcPoint;
        long time = now(), dropFrames = 0;
        while (!point.equals(dstPoint)) {
            point.x += stepX;
            point.y += stepY;
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
                paintCallback.execute(); // repaint now
                long newTime = now();
                long timeTaken = newTime - time;
                time = newTime;
                final long waitTime = ANIMATION_DELAY - timeTaken;
                if (waitTime > 0) {
                    delay(waitTime, "Animation interrupted.");
                    dropFrames = 0;
                } else {
                    dropFrames = timeTaken / ANIMATION_DELAY - 1;
                }
            } else {
                dropFrames--;
            }
        }
    }
}
