/**
 *  Copyright (C) 2002-2012   The FreeCol Team
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
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;


/**
 * A <code>Resource</code> wrapping an <code>Image</code>.
 * @see Resource
 */
public class ImageResource extends Resource {

    private static final Logger logger = Logger.getLogger(ImageResource.class.getName());

    private Map<Dimension, Image> grayscaleImages = new HashMap<Dimension, Image>();
    private Map<Dimension, Image> scaledImages = new HashMap<Dimension, Image>();
    private Image image = null;
    private final Object loadingLock = new Object();
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
     * Preload the image.
     */
    public void preload() {
        synchronized (loadingLock) {
            if (image == null) {
                MediaTracker mt = new MediaTracker(_c);
                Image im;
                try {
                    // Explicitly check that the URI is valid before
                    // letting createImage go off and look for it, as the
                    // error it throws is cryptic.
                    URL url = getResourceLocator().toURL();
                    InputStream is = url.openStream();
                    is.close();
                    im = Toolkit.getDefaultToolkit().createImage(url);
                    mt.addImage(im, 0);
                    mt.waitForID(0);
                    if (mt.statusID(0, false) == MediaTracker.COMPLETE) {
                        image = im;
                    }
                } catch (Exception e) {
                    logger.warning("Failed to load image from: " + getResourceLocator()
                                   + "\r\nProblem: " + e );
                }
            }
        }
    }

    /**
     * Gets the <code>Image</code> represented by this resource.
     *
     * @return The image in it's original size.
     */
    public Image getImage() {
        if (image == null) preload();
        return image;
    }
    
    /**
     * Returns the image using the specified scale.
     * 
     * @param scale The size of the requested image (with 1 being normal size,
     *      2 twice the size, 0.5 half the size etc). Rescaling
     *      will be performed unless using 1.
     * @return The scaled <code>Image</code>.
     */
    public Image getImage(double scale) {
        final Image im = getImage();
        if (im == null) return im;
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
        if (im == null
            || ((im.getWidth(null)==d.width && im.getHeight(null)==d.height))) {
            return im;
        }
        final Image cachedScaledImage = scaledImages.get(d);
        if (cachedScaledImage != null) return cachedScaledImage;
        synchronized (loadingLock) {
            final Image cached = scaledImages.get(d);
            if (cached != null) return cached;
            MediaTracker mt = new MediaTracker(_c);
            try {
                //use SCALE_REPLICATE instead of SCALE_SMOOTH to avoid ClassCastException
                //TODO (perhaps): find better solution
                Image scaledVersion = im.getScaledInstance(d.width, d.height,
                                                           Image.SCALE_REPLICATE);
                mt.addImage(scaledVersion, 0, d.width, d.height);
                mt.waitForID(0);
                if (mt.statusID(0, false) == MediaTracker.COMPLETE) {
                    scaledImages.put(d, scaledVersion);
                    return scaledVersion;
                }
            } catch (Exception e) {
                logger.warning("Failed to scale image: " + getResourceLocator()
                               + "\r\nProblem: " + e );
            }
        }
        return null;
    }
    
    /**
     * Gets a grayscale version of the image of the given size.
     * 
     * @param d The requested size.
     * @return The <code>Image</code>.
     */
    public Image getGrayscaleImage(Dimension d) {
        final Image im = getImage(d);
        if (im == null) return null;
        final Image cachedGrayscaleImage = grayscaleImages.get(d);
        if (cachedGrayscaleImage != null) return cachedGrayscaleImage;
        synchronized (loadingLock) {
            final Image cached = grayscaleImages.get(d);
            if (cached != null) return cached;
            int width = im.getWidth(null);
            int height = im.getHeight(null);
            ColorConvertOp filter = new ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_GRAY), null);
            BufferedImage srcImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            srcImage.createGraphics().drawImage(im, 0, 0, null);
            final Image grayscaleImage = filter.filter(srcImage, null);
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
        if (im == null) return im;
        return getGrayscaleImage(new Dimension((int) (im.getWidth(null) * scale), (int) (im.getHeight(null) * scale)));    
    }

    public int getCount() {
        return grayscaleImages.size() + scaledImages.size();
    }
    
}
