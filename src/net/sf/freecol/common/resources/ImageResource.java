/**
 *  Copyright (C) 2002-2020   The FreeCol Team
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
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import static net.sf.freecol.common.util.CollectionUtils.*;


/**
 * A {@code Resource} wrapping an {@code Image}.
 * @see Resource
 */
public class ImageResource extends Resource {

    private static final Logger logger = Logger.getLogger(ImageResource.class.getName());

    /** Comparator to compare buffered images by ascending size. */
    private static final Comparator<BufferedImage> biComp
        = Comparator.<BufferedImage>comparingInt(bi ->
            bi.getWidth() * bi.getHeight());

    private volatile BufferedImage image = null;
    private List<URI> alternativeLocators = null;
    private List<BufferedImage> loadedImages = null;

    /**
     * Public utility to assign positive values to negative(wildcard)
     * parts of a {@code Dimension} that is to be used to specify a scaled
     * image size.
     *
     * Used below when getColoredImage needs to replace an image, but
     * it is assumed that it has already been called on the Dimension 
     * passed to getImage(size, gray) (which is currently true as it is
     * only called out of ImageCache.  TODO: do this in a less brittle way.
     *
     * @param img The {@code BufferedImage} to be scaled.
     * @param siz The {@code Dimension} to fix.
     * @return A {@code Dimension} with all non-negative values.
     */
    public static Dimension wildcardedDimensions(BufferedImage img,
                                                 Dimension siz) {
        Dimension ret;
        if (siz.width < 0 && siz.height < 0) {
            // Both dimensions wildcarded, just use the actual size
            ret = new Dimension(img.getWidth(), img.getHeight());
        } else if (siz.width < 0) {
            // Width wildcarded, tend to w * req_h/h + eps
            int h = img.getHeight();
            int w = (2 * img.getWidth() * siz.height + (h+1)) / (2*h);
            ret = new Dimension(w, siz.height);
        } else if (siz.height < 0) {
            // Height wildcarded, tend to h * req_w/w + eps
            int w = img.getWidth();
            int h = (2 * img.getHeight() * siz.width + (w+1)) / (2*w);
            ret = new Dimension(siz.width, h);
        } else {
            ret = siz;
        }
        return ret;
    }


    /**
     * Do not use directly.
     *
     * @param resourceLocator The {@code URI} used when loading this resource.
     */
    public ImageResource(URI resourceLocator) {
        super(resourceLocator);
    }


    /**
     * Adds another URI for loading a differently sized version of the image.
     * Only use before preload got called!
     *
     * @param uri The {@code URI} used when loading.
     */
    public synchronized void addAlternativeResourceLocator(URI uri) {
        if (this.alternativeLocators == null)
            this.alternativeLocators = new ArrayList<>();
        this.alternativeLocators.add(uri);
    }

    /**
     * Gets the {@code Image} represented by this resource.
     *
     * @return The image in its original size and color.
     */
    public BufferedImage getImage() {
        if (this.image == null) {
            logger.finest("Preload not ready for " + getResourceLocator());
            preload();
        }
        return this.image;
    }

    /**
     * Gets the image using the specified dimension and choice of grayscale.
     * 
     * d is assumed to be de-wildcarded, see above.
     *
     * @param d The {@code Dimension} of the requested image.
     * @param grayscale If true return a grayscale image.
     * @return The scaled {@code BufferedImage}.
     */
    public BufferedImage getImage(Dimension d, boolean grayscale) {
        return (grayscale) ? getGrayscaleImage(d) : getColorImage(d);
    }

    /**
     * Find the loaded image that satisfies a predicate.
     *
     * @param pred The <code>Predicate</code> to satisfy.
     * @return The <code>BufferedImage</code> found, or if not found, the
     *     one at the end of the loaded images list.
     */
    private synchronized BufferedImage findLoadedImage(Predicate<BufferedImage> pred) {
        BufferedImage oim = find(this.loadedImages, pred);
        return (oim != null) ? oim
            : this.loadedImages.get(this.loadedImages.size() - 1);
    }

    private synchronized boolean haveAlternatives() {
        return this.loadedImages != null;
    }

    /**
     * Gets the image using the specified dimension.
     *
     * Rescaling will be performed if necessary.
     * 
     * @param siz The {@code Dimension} of the requested image.
     * @return The {@code BufferedImage} with the required dimension.
     */
    private BufferedImage getColorImage(Dimension siz) {
        BufferedImage img = getImage();
        int w = img.getWidth(), h = img.getHeight(),
            wNew = siz.width, hNew = siz.height;
        if (wNew == w && hNew == h) return img; // Matching dimension

        if (this.haveAlternatives()) {
            final int fwNew = wNew, fhNew = hNew;
            final Predicate<BufferedImage> sizePred = i ->
                i.getWidth() >= fwNew && i.getHeight() >= fhNew;
            img = findLoadedImage(sizePred);
            siz = wildcardedDimensions(img, siz);
            w = img.getWidth();
            h = img.getHeight();
            wNew = siz.width;
            hNew = siz.height;
            if (wNew == w && hNew == h) return img; // Found loaded image
        }

        // Directly scaling to less than half size would ignore some pixels.
        // Prevent that by halving the base image size as often as needed.
        while (wNew*2 <= w && hNew*2 <= h) {
            w = (w+1)/2;
            h = (h+1)/2;
            BufferedImage halved
                = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = halved.createGraphics();
            // For halving bilinear should most correctly average 2x2 pixels.
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                                 RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.drawImage(img, 0, 0, w, h, null);
            g2d.dispose();
            img = halved;
        }

        if (wNew != w || hNew != h) {
            BufferedImage scaled
                = new BufferedImage(wNew, hNew, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = scaled.createGraphics();
            // Bicubic should give best quality for odd scaling factors.
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                                 RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2d.drawImage(img, 0, 0, wNew, hNew, null);
            g2d.dispose();
            img = scaled;
        }
        return img;
    }

    /**
     * Gets a grayscale version of the image of the given size.
     * 
     * @param siz The {@code Dimension} of the requested image.
     * @return The {@code BufferedImage}.
     */
    private BufferedImage getGrayscaleImage(Dimension siz) {
        final BufferedImage img = getColorImage(siz); // Get the scaled image
        int width = img.getWidth();
        int height = img.getHeight();
        // TODO: Find out why making a copy is necessary here to prevent
        //       images from getting too dark.
        BufferedImage src = new BufferedImage(width, height,
                                              BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = src.createGraphics();
        g2d.drawImage(img, 0, 0, null);
        g2d.dispose();
        ColorConvertOp filter = new ColorConvertOp(ColorSpace
            .getInstance(ColorSpace.CS_GRAY), null);
        return filter.filter(src, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void preload() {
        if (this.image == null) {
            this.image = loadImage(getResourceLocator());
            if (this.alternativeLocators != null) {
                this.loadedImages = new ArrayList<>();
                if (this.image != null) this.loadedImages.add(this.image);
                for (URI uri : alternativeLocators) {
                    BufferedImage image = loadImage(uri);
                    if (image != null) this.loadedImages.add(image);
                }
                this.loadedImages.sort(biComp);
                if (this.image == null && !this.loadedImages.isEmpty()) {
                    this.image = first(this.loadedImages);
                }
            }
        }
    }

    /**
     * Load an image from a URI.
     *
     * @param uri The {@code URI} to load from.
     * @return The loaded {@code BufferedImage}, or null on error.
     */
    private static BufferedImage loadImage(URI uri) {
        try {
            URL url = uri.toURL();
            BufferedImage image = ImageIO.read(url);
            if (image != null) return image;
            logger.log(Level.WARNING, "Failed to read image from: " + uri);
        } catch (IOException ioe) {
            logger.log(Level.WARNING, "Exception to loading image from: "
                + uri, ioe);
        }
        return null;
    }
}
