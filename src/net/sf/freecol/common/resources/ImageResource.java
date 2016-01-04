/**
 *  Copyright (C) 2002-2016   The FreeCol Team
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

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;


/**
 * A <code>Resource</code> wrapping an <code>Image</code>.
 * @see Resource
 */
public class ImageResource extends Resource
                           implements Resource.Preloadable, Resource.Cleanable {

    private static final Logger logger = Logger.getLogger(ImageResource.class.getName());

    private final Object loadingLock = new Object();
    private volatile BufferedImage image = null;
    private List<URI> alternativeLocators = new ArrayList<>();
    private List<BufferedImage> loadedImages = null;

    private HashMap<Dimension, BufferedImage> scaledImages = new HashMap<>();
    private HashMap<Dimension, BufferedImage> grayscaleImages = new HashMap<>();


    /**
     * Do not use directly.
     *
     * @param resourceLocator The <code>URI</code> used when loading this
     *      resource.
     */
    public ImageResource(URI resourceLocator) {
        super(resourceLocator);
    }

    /**
     * Adds another URI for loading a differently sized version of the image.
     * Only use before preload got called!
     *
     * @param resourceLocator The <code>URI</code> used when loading.
     */
    public void addAlternativeResourceLocator(URI resourceLocator) {
        alternativeLocators.add(resourceLocator);
    }

    /**
     * Preload the image.
     */
    @Override
    public void preload() {
        synchronized (loadingLock) {
            if (image == null) {
                BufferedImage baseImage = loadImage(getResourceLocator());

                loadedImages = alternativeLocators.stream()
                    .map(uri -> loadImage(uri))
                    .filter(img -> img != null)
                    .collect(Collectors.toList());
                if(baseImage != null)
                    loadedImages.add(baseImage);
                loadedImages.sort((img0, img1) ->
                    img0.getWidth()*img0.getHeight() -
                        img1.getWidth()*img1.getHeight());

                image = baseImage != null ? baseImage :
                    loadedImages.isEmpty() ? null : loadedImages.get(0);
            }
        }
    }

    private static BufferedImage loadImage(URI uri) {
        try {
            URL url = uri.toURL();
            BufferedImage image = ImageIO.read(url);
            if(image != null)
                return image;
            logger.log(Level.WARNING, "Failed to load image from: "
                + uri);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to load image from: "
                + uri, e);
        }
        return null;
    }

    /**
     * Clean up old cached copies.
     */
    @Override
    public void clean() {
        scaledImages = new HashMap<>();
        grayscaleImages = new HashMap<>();
    }

    /**
     * Gets the <code>Image</code> represented by this resource.
     *
     * @return The image in it's original size.
     */
    public BufferedImage getImage() {
        if (image == null) {
            logger.finest("Preload not ready for " + getResourceLocator());
            preload();
        }
        return image;
    }

    /**
     * Gets the image using the specified scale.
     * 
     * @param scale The size of the requested image (with 1 being
     *     normal size, 2 twice the size, 0.5 half the size etc).
     * @return The scaled <code>BufferedImage</code>.
     */
    public BufferedImage getImage(float scale) {
        final BufferedImage im = getImage();
        if(scale == 1.0f || im == null)
            return im;
        return getImage(new Dimension(Math.round(im.getWidth() * scale),
                                      Math.round(im.getHeight() * scale)));
    }

    /**
     * Gets the image using the specified dimension.
     * 
     * @param d The <code>Dimension</code> of the requested
     *      image.  Rescaling will be performed if necessary.
     * @return The <code>BufferedImage</code> with the required dimension.
     */
    public BufferedImage getImage(Dimension d) {
        BufferedImage im = getImage();
        if (im == null)
            return null;
        int wNew = d.width;
        int hNew = d.height;
        if(wNew < 0 && hNew < 0)
            return im;
        int w = im.getWidth();
        int h = im.getHeight();
        if(wNew < 0 || (!(hNew < 0) && wNew*h > w*hNew)) {
            wNew = (2*w*hNew + (h+1)) / (2*h);
        } else if(hNew < 0 || wNew*h < w*hNew) {
            hNew = (2*h*wNew + (w+1)) / (2*w);
        }
        if(wNew == w && hNew == h)
            return im;

        final BufferedImage cached = scaledImages.get(d);
        if (cached != null) return cached;

        final int wNew2 = wNew;
        final int hNew2 = hNew;
        Optional<BufferedImage> oim = loadedImages.stream()
            .filter(img -> img.getWidth() >= wNew2 && img.getHeight() >= hNew2)
            .findFirst();
        im = oim.isPresent() ? oim.get() :
            loadedImages.get(loadedImages.size() - 1);
        w = im.getWidth();
        h = im.getHeight();
        if(wNew*h > w*hNew) {
            wNew = (2*w*hNew + (h+1)) / (2*h);
        } else if(wNew*h < w*hNew) {
            hNew = (2*h*wNew + (w+1)) / (2*w);
        }
        if(wNew == w && hNew == h)
            return im;

        // Directly scaling to less than half size would ignore some pixels.
        // Prevent that by halving the base image size as often as needed.
        while(wNew*2 <= w && hNew*2 <= h) {
            w = (w+1)/2;
            h = (h+1)/2;
            BufferedImage halved = new BufferedImage(w, h,
                BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = halved.createGraphics();
            // For halving bilinear should most correctly average 2x2 pixels.
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(im, 0, 0, w, h, null);
            g.dispose();
            im = halved;
        }

        if(wNew != w || hNew != h) {
            BufferedImage scaled = new BufferedImage(wNew, hNew,
                BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = scaled.createGraphics();
            // Bicubic should give best quality for odd scaling factors.
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.drawImage(im, 0, 0, wNew, hNew, null);
            g.dispose();
            im = scaled;
        }
        scaledImages.put(d, im);
        return im;
    }

    /**
     * Gets a grayscale version of the image of the given size.
     * 
     * @param d The requested size.
     * @return The <code>BufferedImage</code>.
     */
    public BufferedImage getGrayscaleImage(Dimension d) {
        final BufferedImage cached = grayscaleImages.get(d);
        if (cached != null) return cached;
        final BufferedImage im = getImage(d);
        if (im == null) return null;
        int width = im.getWidth();
        int height = im.getHeight();
        // TODO: Find out why making a copy is necessary here to prevent
        //       images from getting too dark.
        BufferedImage srcImage = new BufferedImage(width, height,
            BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = srcImage.createGraphics();
        g.drawImage(im, 0, 0, null);
        g.dispose();
        ColorConvertOp filter = new ColorConvertOp(
            ColorSpace.getInstance(ColorSpace.CS_GRAY), null);
        final BufferedImage grayscaleImage = filter.filter(srcImage, null);
        grayscaleImages.put(d, grayscaleImage);
        return grayscaleImage;
    }

    /**
     * Returns the image using the specified scale.
     * 
     * @param scale The size of the requested image (with 1 being normal size,
     *      2 twice the size, 0.5 half the size etc). Rescaling
     *      will be performed unless using 1.
     * @return The <code>BufferedImage</code>.
     */
    public BufferedImage getGrayscaleImage(float scale) {
        final BufferedImage im = getImage();
        if (im == null) return null;
        return getGrayscaleImage(new Dimension(Math.round(im.getWidth() * scale),
                                               Math.round(im.getHeight() * scale)));
    }

    public int getCount() {
        return grayscaleImages.size() + scaledImages.size();
    }
}
