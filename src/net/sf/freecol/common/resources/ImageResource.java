/**
 *  Copyright (C) 2002-2008  The FreeCol Team
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

package net.sf.freecol.common.resources;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * A <code>Resource</code> wrapping an <code>Image</code>.
 * @see Resource
 */
public class ImageResource extends Resource {

    private Map<Dimension, Image> scaledImages = new HashMap<Dimension, Image>();
    private Image image = null;
    private Object loadingLock = new Object();
    private final Component _c = new Component() {};
    
    /**
     * Do not use directly.
     * @param resourceLocator The <code>URL</code> used when loading this
     *      resource.
     * @see ResourceFactory#createResource(URL)
     */
    ImageResource(URL resourceLocator) {
        super(resourceLocator);
    }
    
    
    /**
     * Gets the <code>Image</code> represented by this resource.
     * @return The image in it's original size.
     */
    public Image getImage() {
        if (image != null) {
            return image;
        }
        synchronized (loadingLock) {
            if (image != null) {
                return image;
            }
            MediaTracker mt = new MediaTracker(_c);
            final Image im = Toolkit.getDefaultToolkit().createImage(getResourceLocator());
            mt.addImage(im, 0);
            try {
                mt.waitForID(0);
            } catch (InterruptedException e) {
                return null;
            }
            image = im;
            return image;
        }
    }
    
    /**
     * Returns the image using the specified scale.
     * 
     * @param resource The name of the resource to return.
     * @param scale The size of the requested image (with 1 being normal size,
     *      2 twice the size, 0.5 half the size etc). Rescaling
     *      will be performed unless using 1.
     * @return The <code>Image</code>.
     */
    public Image getImage(double scale) {
        final Image im = getImage();
        return getImage(new Dimension((int) (im.getWidth(null) * scale), (int) (im.getHeight(null) * scale)));    
    }
    
    /**
     * Returns the image using the specified size.
     * 
     * @param size The size of the requested image. Rescaling
     *      will be performed if necessary.
     * @return The <code>Image</code>.
     */
    public Image getImage(Dimension d) {
        final Image im = getImage();
        if (im.getWidth(null) == d.width
                && im.getHeight(null) == d.height) {
            return im;
        }
        final Image cachedScaledVersion = scaledImages.get(d);
        if (cachedScaledVersion != null) {
            return cachedScaledVersion;
        }
        synchronized (loadingLock) {
            if (scaledImages.get(d) != null) {
                return scaledImages.get(d);
            }
            MediaTracker mt = new MediaTracker(_c);
            final Image scaledVersion = im.getScaledInstance(d.width, d.height, Image.SCALE_SMOOTH);
            mt.addImage(im, 0, d.width, d.height);
            try {
                mt.waitForID(0);
            } catch (InterruptedException e) {
                return null;
            }
            scaledImages.put(d, scaledVersion);
            return scaledVersion;
        }
    }
}
