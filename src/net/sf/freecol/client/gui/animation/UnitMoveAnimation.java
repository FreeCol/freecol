/**
 *  Copyright (C) 2002-2015   The FreeCol Team
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

import javax.swing.JLabel;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.SwingGUI;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;


/**
 * Class for the animation of units movement.
 */
final class UnitMoveAnimation {

    /*
     * Display delay between one frame and another, in milliseconds.
     * 33ms == 30 fps
     */
    private static final int ANIMATION_DELAY = 33;

    private final FreeColClient freeColClient;
    private final Unit unit;
    private final Tile sourceTile;
    private final Tile destinationTile;

    /**
     * Constructor
     *
     * @param freeColClient The enclosing <code>FreeColClient</code>.
     * @param unit The <code>Unit</code> to be animated.
     * @param sourceTile The <code>Tile</code> the unit is moving from.
     * @param destinationTile The <code>Tile</code> the unit is moving to.
     */
    public UnitMoveAnimation(FreeColClient freeColClient, Unit unit,
                             Tile sourceTile, Tile destinationTile) {
        this.freeColClient = freeColClient;
        this.unit = unit;
        this.sourceTile = sourceTile;
        this.destinationTile = destinationTile;
    }
    

    /**
     * Do the animation.
     */
    public void animate() {
        final int movementSpeed
            = freeColClient.getAnimationSpeed(unit.getOwner());
        final SwingGUI gui = (SwingGUI)freeColClient.getGUI();
        final Point srcP = gui.getTilePosition(sourceTile);
        final Point dstP = gui.getTilePosition(destinationTile);
        
        if (srcP == null || dstP == null || movementSpeed <= 0) return;

        float scale = gui.getMapScale();
        final int movementRatio = (int)(Math.pow(2, movementSpeed + 1) * scale);
        final Rectangle r1 = gui.getTileBounds(sourceTile);
        final Rectangle r2 = gui.getTileBounds(destinationTile);
        final Rectangle bounds = r1.union(r2);

        gui.executeWithUnitOutForAnimation(unit, sourceTile,
            (JLabel unitLabel) -> {
                final int labelWidth = unitLabel.getWidth();
                final int labelHeight = unitLabel.getHeight();
                final Point srcPoint = gui.calculateUnitLabelPositionInTile(labelWidth, labelHeight, srcP);
                final Point dstPoint = gui.calculateUnitLabelPositionInTile(labelWidth, labelHeight, dstP);
                final double xratio = ImageLibrary.TILE_SIZE.width
                    / (double)ImageLibrary.TILE_SIZE.height;
                final int stepX = (srcPoint.getX() == dstPoint.getX()) ? 0
                    : (srcPoint.getX() > dstPoint.getX()) ? -1 : 1;
                final int stepY = (srcPoint.getY() == dstPoint.getY()) ? 0
                    : (srcPoint.getY() > dstPoint.getY()) ? -1 : 1;

                // Painting the whole screen once to get rid of
                // disposed dialog-boxes.
                gui.paintImmediatelyCanvasInItsBounds();

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
                        gui.paintImmediatelyCanvasIn(bounds);
                            
                        int timeTaken = (int)(System.currentTimeMillis() - time);
                        final int waitTime = ANIMATION_DELAY - timeTaken;
                        if (waitTime > 0) {
                            try {
                                Thread.sleep(waitTime);
                            } catch (InterruptedException ex) {
                                //ignore
                            }
                            dropFrames = 0;
                        } else {
                            dropFrames = timeTaken / ANIMATION_DELAY - 1;
                        }
                    } else {
                            dropFrames--;
                    }
                }
            });
    }
}
