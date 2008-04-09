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
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;

/**
 * Class for the animation of units movement.
 */
public final class UnitMoveAnimation extends Animation {
    
    private static final Logger logger = Logger.getLogger(UnitMoveAnimation.class.getName());
    
    private final Unit unit;
    private final Tile destinationTile;
    private final Point destinationPoint;
    private Point currentPoint;
    
    private Rectangle previousBounds;
    
    private JLabel unitLabel;
    private static final Integer UNIT_LABEL_LAYER = JLayeredPane.DEFAULT_LAYER;
    
    // Movement variables & constants
    private final int signalX; // If X is increasing or decreasing
    private final int signalY; // If Y is increasing or decreasing
    private static final int MOVEMENT_RATIO = 4; // pixels/frame (must be power of 2)
    private static final int X_RATIO = 2;
    private static final int Y_RATIO = 1;
    
    
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
        super(canvas);
        this.unit = unit;
        this.destinationTile = destinationTile;
        
        Point p = canvas.getGUI().getTilePosition(unit.getTile());
        if (p != null) {
            ImageIcon unitImg = canvas.getGUI().getImageLibrary().getUnitImageIcon(unit);        
            //TODO: displayOccupationIndicator for the animation?
            //canvas.getGUI().displayOccupationIndicator(unitImg.getImage().getGraphics(), unit, (int) (GUI.STATE_OFFSET_X * canvas.getGUI().getImageLibrary().getScalingFactor()), 0);
            // No need to use media tracker to wait for images to load since javax.swing.ImageIcon already does this.
            
            currentPoint = canvas.getGUI().getUnitPositionInTile(unitImg, p);
            destinationPoint = canvas.getGUI().getUnitPositionInTile(unitImg, canvas.getGUI().getTilePosition(destinationTile));
            
            unitLabel = new JLabel(unitImg);
            unitLabel.setBounds(currentPoint.x, currentPoint.y, unitImg.getIconWidth(), unitImg.getIconHeight());
            canvas.add(unitLabel, UNIT_LABEL_LAYER, 0);
            
            if (currentPoint.getX() == destinationPoint.getX())
                signalX = 0;
            else
                signalX = currentPoint.getX() > destinationPoint.getX() ? -1 : 1;
            
            if (currentPoint.getY() == destinationPoint.getY())
                signalY = 0;
            else
                signalY = currentPoint.getY() > destinationPoint.getY() ? -1 : 1;
            
        } else {
            // Unit is offscreen - no need to animate
            logger.finest("Unit is offscreen - no need to animate.");
            currentPoint = destinationPoint = null;
            signalX = signalY = 0;
        }
    }

    /**
     * Moves the Unit towards its destination point one step.
     */
    protected void readyNextFrame() {
        
        logger.finest("Calculating and setting the new unit location.");
        previousBounds = unitLabel.getBounds();
                
        // Calculating the new coordinates for the unit            
        currentPoint.x += signalX*X_RATIO*MOVEMENT_RATIO;        
        currentPoint.y += signalY*Y_RATIO*MOVEMENT_RATIO;
        
        //Setting new location
        unitLabel.setLocation(currentPoint);
    }

    @Override
    public void animate() {
        logger.finest("Removing the unit temporarily from its Tile and painting screen.");
        unit.getTile().removeUnitNoUpdate(unit);
        // Painting the whole screen once to get rid of disposed dialog-boxes.
        canvas.paintImmediately(canvas.getBounds());
        try {
            super.animate();
        } 
        finally { // If there are any exceptions during animate() I don't want my unit to just vanish.
            logger.finest("Adding the unit back to its Tile and removing the label component.");
            unit.getTile().addUnitNoUpdate(unit);
            canvas.remove(unitLabel);
        }
    }
    
    protected boolean isFinished() {
        if (destinationPoint != null)
            return destinationPoint.equals(currentPoint);
        else
            return true;
    }

    protected Rectangle getAnimationArea() {
        Rectangle r1 = canvas.getGUI().getTileBounds(unit.getTile());
        Rectangle r2 = canvas.getGUI().getTileBounds(destinationTile);
        return r1.union(r2);
    }
    
    protected Rectangle getDirtyAnimationArea() {
        //The dirty area is where the unit image was drawn before 
        //and where it should be drawn now.
        if (previousBounds != null) {
            return previousBounds.union(unitLabel.getBounds());
        }
        else // Should never happen. Just in case.
            return getAnimationArea();
    }

}
