/**
 *  Copyright (C) 2002-2007  The FreeCol Team
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

import net.sf.freecol.client.ClientOptions;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.OutForAnimationCallback;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Map.Direction;

/**
 * Class for the animation of units movement.
 */
final class UnitMoveAnimation {
    
    private static final Logger logger = Logger.getLogger(UnitMoveAnimation.class.getName());
    
    /*
     * Display delay between one frame and another, in milliseconds.
     * 33ms == 30 fps
     */
    private static final int ANIMATION_DELAY = 33;

    private final Canvas canvas;
    private final Unit unit;
    private final Location currentLocation;
    private final Tile destinationTile;
    
    
    /**
     * Constructor
     * @param canvas The canvas where the animation will be drawn
     * @param unit The unit to be animated. 
     * @param direction The Direction in which the Unit will be moving.
     */
    public UnitMoveAnimation(Canvas canvas, Unit unit, Direction direction) {
        this(canvas, unit, unit.getGame().getMap().getNeighbourOrNull(direction, unit.getTile()));
    }
    
    /**
     * Constructor
     * @param canvas The canvas where the animation will be drawn
     * @param unit The unit to be animated. 
     * @param destinationTile The Tile where the Unit will be moving to.
     */
    public UnitMoveAnimation(Canvas canvas, Unit unit, Tile destinationTile) {
        this.canvas = canvas;
        this.unit = unit;
        this.destinationTile = destinationTile;
        this.currentLocation = unit.getLocation();        
    }
    
    public void animate() {
        final GUI gui = canvas.getGUI();
        final String key = (canvas.getClient().getMyPlayer() == unit.getOwner()) ?
                ClientOptions.MOVE_ANIMATION_SPEED
                : ClientOptions.ENEMY_MOVE_ANIMATION_SPEED;
        final int movementSpeed = canvas.getClient().getClientOptions().getInteger(key);
        final int movementRatio = (int) (Math.pow(2, movementSpeed+1) * gui.getImageLibrary().getScalingFactor());
        final Point currP = gui.getTilePosition(unit.getTile());
        final Point destP = gui.getTilePosition(destinationTile);
        
        if (currP == null || destP == null || movementSpeed <= 0) {
            return;
        }
        
        final Rectangle bounds = getAnimationArea();
        
        canvas.getGUI().executeWithUnitOutForAnimation(unit, new OutForAnimationCallback() {
            public void executeWithUnitOutForAnimation(final JLabel unitLabel) {                    
                final Point currentPoint = gui.getUnitLabelPositionInTile(unitLabel, currP);
                final Point destinationPoint = gui.getUnitLabelPositionInTile(unitLabel, destP);
                
                final double xratio = gui.getTileWidth() / gui.getTileHeight();
                final int signalX = (currentPoint.getX() == destinationPoint.getX()) ? 0
                        : (currentPoint.getX() > destinationPoint.getX()) ? -1 : 1;
                final int signalY = (currentPoint.getY() == destinationPoint.getY()) ? 0
                        : (currentPoint.getY() > destinationPoint.getY()) ? -1 : 1;
                // Painting the whole screen once to get rid of disposed dialog-boxes.
                canvas.paintImmediately(canvas.getBounds());
                while (!currentPoint.equals(destinationPoint)) {
                    long time = System.currentTimeMillis();
                    
                    currentPoint.x += signalX*xratio*movementRatio;
                    currentPoint.y += signalY*movementRatio;
                    if (signalX == -1 && currentPoint.x < destinationPoint.x) {
                        currentPoint.x = destinationPoint.x;
                    } else if (signalX == 1 && currentPoint.x > destinationPoint.x) {
                        currentPoint.x = destinationPoint.x;
                    }
                    if (signalY == -1 && currentPoint.y < destinationPoint.y) {
                        currentPoint.y = destinationPoint.y;
                    } else if (signalY == 1 && currentPoint.y > destinationPoint.y) {
                        currentPoint.y = destinationPoint.y;
                    }
                    
                    //Setting new location
                    unitLabel.setLocation(currentPoint);
                    canvas.paintImmediately(bounds);
                    
                    // Time it took to draw the next frame - should be discounted from the animation delay
                    time = ANIMATION_DELAY - System.currentTimeMillis() + time;
                    if (time > 0) {
                        try {
                            Thread.sleep(time);
                        } catch (InterruptedException ex) {
                            //ignore
                        }
                    }
                }
            }
        });
    }

    protected Rectangle getAnimationArea() {
        final Rectangle r1 = canvas.getGUI().getTileBounds(currentLocation.getTile());
        final Rectangle r2 = canvas.getGUI().getTileBounds(destinationTile);
        return r1.union(r2);
    }
}
