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

import java.awt.Image;
import java.awt.Rectangle;
import java.util.ArrayList;
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
    
    /**
     * The Canvas' x coordinate where the animation is being drawn
     */
    protected int canvasX;
    
    /**
     * The Canvas' y coordinate where the animation is being drawn
     */
    protected int canvasY;
    
    /**
     * Display delay between one frame and another, in milliseconds.
     * 33ms == 30 fps
     */
    private static final int ANIMATION_DELAY = 33;
    
    public Animation(Canvas canvas) {
        this.canvas = canvas;
        this.canvasX = this.canvasY = 0;
    }
    
    /**
     * Plays the animation, no buffer mode.
     */
    public void animate() {
        animate(false);
    }    
    
    /**
     * Plays the animation
     * @param buffer If the animation should be buffered or not
     */
    public void animate(boolean buffer) {
        logger.fine("Starting Animation.animate(" + buffer + ")");
        if (!isFinished()) {
            if (buffer)
                bufferedAnimation();
            else
                nonbufferedAnimation();
            logger.fine("Ending Animation.animate().");
        } else {
            logger.fine("Animation already isFinished, do nothing.");
        }
    }
    
    /**
     * Refreshes the screen until the animation is done. 
     * Renders frames on the go.
     */
    protected void nonbufferedAnimation() {
        while (!isFinished()) {
            long time = System.currentTimeMillis();
            
            drawFrame(getNextFrame());
            
            // Time it took to draw the next frame - should be discounted from the animation delay
            time = System.currentTimeMillis() - time;
            if (time < ANIMATION_DELAY) {
                try {
                    Thread.sleep(ANIMATION_DELAY - time);
                } catch (InterruptedException ex) {
                    //ignore
                }
            }
        } 
    }
    
    /**
     *  Refreshes the screen until the animation is done. 
     *  Renders frames on a buffer before animating. 
     */
    protected void bufferedAnimation() {
        ArrayList<Image> frameBuffer = new ArrayList<Image>();
        logger.finest("Storing animation's frames in buffer.");
        while (!isFinished()) {
            frameBuffer.add(getNextFrame());
        }
        for (Image image : frameBuffer) {
            long time = System.currentTimeMillis();
            
            drawFrame(image);
            
            // Time it took to draw the next frame - should be discounted from the animation delay
            time = System.currentTimeMillis() - time;
            if (time < ANIMATION_DELAY) {
                try {
                    Thread.sleep(ANIMATION_DELAY - time);
                } catch (InterruptedException ex) {
                    //ignore
                }
            }
        }
    }
    
    private void drawFrame(Image frame) {
        Rectangle animationArea = getAnimationArea();
        canvas.getGraphics().drawImage(frame, 
                                        (int) animationArea.getMinX(), (int) animationArea.getMinY(), (int) animationArea.getMaxX(), (int) animationArea.getMaxY(),  
                                        0, 0, (int) animationArea.getWidth(), (int) animationArea.getHeight(), 
                                        null);
        canvas.repaint(getAnimationArea()); 
        //canvas.repaint();
        
    }
    
    /**
     * Draw the next frame of the animation and return it as a Image object
     * @return Image object of the next frame of the animation
     */
    protected abstract Image getNextFrame();
    
    /**
     * Returns the area where the animation is being drawn.
     * @return The Rectangle object that represents the area where the animation is being drawn
     */
    protected abstract Rectangle getAnimationArea();
    
    /**
     * Checks the condition for the end of the animation
     * @return True if the animation is finished, false otherwise.
     */
    protected abstract boolean isFinished();

}
