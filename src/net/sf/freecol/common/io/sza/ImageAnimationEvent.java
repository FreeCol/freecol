package net.sf.freecol.common.io.sza;

import java.awt.Image;

/**
 * An event describing that the given image should
 * be displayed.
 */
public interface ImageAnimationEvent extends AnimationEvent {

    /**
     * Returns the image this event contains.
     * @return The image to be displayed. 
     */
    public Image getImage();
    
    /**
     * Returns the duration the image should be displayed.
     * @return The amount of milliseconds to display the
     *      image.
     */
    public int getDurationInMs();
}
