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

import java.awt.Rectangle;
import java.util.logging.Logger;
import net.sf.freecol.client.gui.Canvas;

/**
 * Parent class for animations.
 */
public abstract class Animation {
    
    private static final Logger logger = Logger.getLogger(Animation.class.getName());
    
    /**
     * The client's canvas where the animation will be drawn
     */
    protected final Canvas canvas;
    
    /*
     * Display delay between one frame and another, in milliseconds.
     * 33ms == 30 fps
     */
    private static final int ANIMATION_DELAY = 33;
    
    public Animation(Canvas canvas) {
        this.canvas = canvas;
    }
    
    /**
     * Refreshes the canvas until the animation is done. 
     * Renders frames on the go.
     */
    public void animate() {
        logger.finer("Starting animation");
        while (!isFinished()) {
            long time = System.currentTimeMillis();
            
            drawNextFrame();
            
            // Time it took to draw the next frame - should be discounted from the animation delay
            time = System.currentTimeMillis() - time;
            if (time < ANIMATION_DELAY) {
                try {
                    Thread.sleep(ANIMATION_DELAY - time);
                } catch (InterruptedException ex) {
                    //ignore
                }
            }
            logger.finest("Frame took " + time + "ms to render.");
        } 
        logger.finer("Finishing animation");
    }
    
    /**
     * Draw the next frame of the animation on the canvas
     */
    protected void drawNextFrame() {
        
        readyNextFrame();
        
        logger.finest("Drawing next animation frame");
        canvas.paintImmediately(getDirtyAnimationArea());
    }
    
    /**
     * Readies the next frame before we draw it. 
     * Make sure to set the dirty animation area accordingly.
     */
    protected abstract void readyNextFrame();
    
    /**
     * Returns the area where the animation happens i.e. the animation's bounds.
     * @return The Rectangle object that represents the area where the animation is being drawn
     */
    protected abstract Rectangle getAnimationArea();
    
    /**
     * Returns the area where the screen is dirty i.e. not updated.
     * @return The Rectangle object of the area where the screen should be redrawn.
     */
    protected abstract Rectangle getDirtyAnimationArea();
    
    /**
     * Checks the condition for the end of the animation
     * @return True if the animation is finished, false otherwise.
     */
    protected abstract boolean isFinished();

}
