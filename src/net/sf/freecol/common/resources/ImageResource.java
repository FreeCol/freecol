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
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * A <code>Resource</code> wrapping an <code>Image</code>.
 * @see Resource
 */
public class ImageResource extends Resource {

    private Map<Dimension, Image> grayscaleImages = new HashMap<Dimension, Image>();
    private Map<Dimension, Image> scaledImages = new HashMap<Dimension, Image>();
    private Image image = null;
    private Object loadingLock = new Object();
    private static final Component _c = new Component() {};
    
    /**
     * Do not use directly.
     * @param resourceLocator The <code>URI</code> used when loading this
     *      resource.
     * @see ResourceFactory#createResource(URI)
     */
    ImageResource(URI resourceLocator) {
        super(resourceLocator);
    }

    public ImageResource(Image image) {
        this.image = image;
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
            Image im;
            try {
                im = Toolkit.getDefaultToolkit().createImage(getResourceLocator().toURL());
                mt.addImage(im, 0);
                mt.waitForID(0);
            } catch (Exception e) {
                return null;
            }
            image = im;
            return image;
        }
    }
    
    /**
     * Returns the image using the specified scale.
     * 
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
     * Returns the image using the specified dimension.
     * 
     * @param d The dimension of the requested image. Rescaling
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
            mt.addImage(scaledVersion, 0, d.width, d.height);
            try {
                mt.waitForID(0);
            } catch (InterruptedException e) {
                return null;
            }
            scaledImages.put(d, scaledVersion);
            return scaledVersion;
        }
    }
    
    /**
     * Gets a grayscale version of the image of the given size.
     * 
     * @param d The requested size.
     * @return The <code>Image</code>.
     */
    public Image getGrayscaleImage(Dimension d) {
        final Image cachedGrayscaleImage = grayscaleImages.get(d);
        if (cachedGrayscaleImage != null) {
            return cachedGrayscaleImage;
        }
        synchronized (loadingLock) {
            if (grayscaleImages.get(d) != null) {
                return grayscaleImages.get(d);
            }
            final Image grayscaleImage = convertToGrayscale(getImage(d));
            grayscaleImages.put(d, grayscaleImage);
            return grayscaleImage;
        }
    }
    
    /**
     * Returns the image using the specified scale.
     * 
     * @param scale The size of the requested image (with 1 being normal size,
     *      2 twice the size, 0.5 half the size etc). Rescaling
     *      will be performed unless using 1.
     * @return The <code>Image</code>.
     */
    public Image getGrayscaleImage(double scale) {
        final Image im = getImage();
        return getGrayscaleImage(new Dimension((int) (im.getWidth(null) * scale), (int) (im.getHeight(null) * scale)));    
    }
    
    /**
     * Converts an image to grayscale
     * 
     * @param image Source image to convert
     * @return The image in grayscale
     */
    private Image convertToGrayscale(Image image) {
        int width = image.getWidth(null);
        int height = image.getHeight(null);
        ColorConvertOp filter = new ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_GRAY), null);
        BufferedImage srcImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        srcImage.createGraphics().drawImage(image, 0, 0, null);
        return filter.filter(srcImage, null);
    }

}
