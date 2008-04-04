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

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.logging.Logger;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.GUI;
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
    
    private Image unitImage;
    private Image bgImage;
    
    // Movement constants
    private static final int MOVEMENT_RATIO = 4; // pixels/frame (must be power of 2)
    private static final int X_RATIO = 2;
    private static final int Y_RATIO = 1;
    
    // MediaTracker ids
    private static final int MT_UNIT_IMAGE_ID = 0;
    private static final int MT_FRAME_IMAGE_ID = 1;
    private static final int MT_BG_IMAGE_ID = 2;
    
    
    /**
     * Constructor
     * @param canvas The canvas where the animation will be drawn
     * @param unit The unit to be animated. nextTile must be set.
     * @param direction The Direction in which the Unit will be moving.
     */
    public UnitMoveAnimation(Canvas canvas, Unit unit, Direction direction) {
        this(canvas, unit, unit.getGame().getMap().getNeighbourOrNull(direction, unit.getTile()));
    }
    
    /**
     * Constructor
     * @param canvas The canvas where the animation will be drawn
     * @param unit The unit to be animated. nextTile must be set.
     * @param destinationTile The Tile where the Unit will be moving to.
     */
    public UnitMoveAnimation(Canvas canvas, Unit unit, Tile destinationTile) {
        super(canvas);
        this.unit = unit;
        this.destinationTile = destinationTile;
        
        logger.fine("Starting Constructor");
        
        
        // Painting screen immediately to get rid of dialog boxes
        logger.finest("Painting screen immediately.");
        canvas.paintImmediately(canvas.getBounds());    
        
        this.unitImage = canvas.getGUI().getImageLibrary().getUnitImageIcon(unit, false).getImage();

        Point p = canvas.getGUI().getTilePosition(unit.getTile());
        if (p != null) {
            canvasX = new Double(p.getX()).intValue();
            canvasY = new Double(p.getY()).intValue();
        
            destinationPoint = canvas.getGUI().getTilePosition(destinationTile);
        } else {
            // Unit is offscreen - no need to animate
            logger.finest("Unit is offscreen - no need to animate.");
            destinationPoint = null;
            return;
        }
        
        // Drawing the background of the animation area.
        // Removing the unit from the tile temporarily
        logger.finest("Removing the unit from its Tile temporarily.");
        unit.getTile().removeUnitNoUpdate(unit);
        
        // TODO: Optimize bgImage, since it is larger than it needs to be. Make it so that it uses only animationArea.
        logger.finest("Drawing the animation's background image.");
        bgImage = canvas.createImage((int) canvas.getWidth(), (int) canvas.getHeight());
        Graphics2D bgGraphics =  (Graphics2D) bgImage.getGraphics();
        bgGraphics.setClip(getAnimationArea());
        canvas.getGUI().display(bgGraphics);
        
        // Putting the unit back
        logger.finest("Putting the unit back to its Tile.");
        unit.getTile().addUnitNoUpdate(unit);
    }

    
    protected Image getNextFrame() {
        
        logger.finest("Drawing new frame");
        
        //MediaTracker to check if all images are loaded before proceeding
        MediaTracker mt = new MediaTracker(canvas);
        
        // Calculating the new coordinates for the unit
        if (canvasX != destinationPoint.getX()) {
            int signal = 1;
            if (canvasX > destinationPoint.getX()) signal = -1;
            
            canvasX += signal*X_RATIO*MOVEMENT_RATIO;
        }
        if (canvasY != destinationPoint.getY()) {
            int signal = 1;
            if (canvasY > destinationPoint.getY()) signal = -1;
            
            canvasY += signal*Y_RATIO*MOVEMENT_RATIO;
        }
                
        //Readying the next frame
        logger.finest("Getting offscreen graphics context for drawing the frame.");
        Rectangle animationArea = getAnimationArea();
        Image nextFrame = canvas.createImage((int) animationArea.getWidth(), (int) animationArea.getHeight());
        Graphics2D frameGraphics = (Graphics2D) nextFrame.getGraphics();
        
        logger.finest("Drawing frame's background image.");
        // Checking if the bgImage is loaded
        mt.addImage(bgImage, MT_BG_IMAGE_ID);
        try {
            mt.waitForID(MT_BG_IMAGE_ID);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
            return null;
        }
        //Drawing the background
        frameGraphics.drawImage(bgImage, 
                                0, 0, (int) animationArea.getWidth(), (int) animationArea.getHeight(), 
                                (int) animationArea.getMinX(), (int) animationArea.getMinY(), (int) animationArea.getMaxX(), (int) animationArea.getMaxY(),  
                                null);
        
        logger.finest("Drawing frame's unit image.");
        // Checking if the unitImage is loaded
        mt.addImage(unitImage, MT_UNIT_IMAGE_ID);
        try {
            mt.waitForID(MT_UNIT_IMAGE_ID);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
            return null;
        }
        //Drawing the unit
        //TODO: Weird coords calculations taken from GUI.displayUnit :) Maybe place them in one location only
        int unitX = ((canvasX + canvas.getGUI().getTileWidth() / 2) - unitImage.getWidth(null) / 2) - animationArea.x;
        int unitY = (canvasY + canvas.getGUI().getTileHeight() / 2) - unitImage.getHeight(null) / 2 -
                    (int) (GUI.UNIT_OFFSET * canvas.getGUI().getImageLibrary().getScalingFactor()) - animationArea.y;
        frameGraphics.drawImage(unitImage, unitX, unitY, null);
        
        canvas.getGUI().displayOccupationIndicator(frameGraphics, unit, canvasX + (int) (GUI.STATE_OFFSET_X * canvas.getGUI().getImageLibrary().getScalingFactor()) - animationArea.x, canvasY - animationArea.y);
        
        //Wait for the frame to finish before returning
        mt.addImage(nextFrame, MT_FRAME_IMAGE_ID);
        try {
            mt.waitForID(MT_FRAME_IMAGE_ID);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
            return null;
        }
        
        return nextFrame;
    }

    protected boolean isFinished() {
        if (destinationPoint != null)
            return (canvasX == destinationPoint.getX() && canvasY == destinationPoint.getY());
        else
            return true;
    }

    protected Rectangle getAnimationArea() {
        Rectangle r1 = canvas.getGUI().getTileBounds(unit.getTile());
        Rectangle r2 = canvas.getGUI().getTileBounds(destinationTile);
        return r1.union(r2);
    }

}
