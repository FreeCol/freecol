/**
 *  Copyright (C) 2002-2021   The FreeCol Team
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
import static net.sf.freecol.common.util.ImageUtils.*;


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
        if (img == null) return null; // Preload failed
        int w = img.getWidth();
        int h = img.getHeight();
        Dimension dNew = wildcardDimension(siz, new Dimension(w, h));
        int wNew = dNew.width, hNew = dNew.height;
        if (wNew == w && hNew == h) return img; // Matching dimension

        if (this.haveAlternatives()) {
            final int fwNew = wNew, fhNew = hNew;
            final Predicate<BufferedImage> sizePred = i ->
                i.getWidth() >= fwNew && i.getHeight() >= fhNew;
            img = findLoadedImage(sizePred);
            w = img.getWidth();
            h = img.getHeight();
            dNew = wildcardDimension(siz, new Dimension(w, h));
            wNew = dNew.width;
            hNew = dNew.height;
            if (wNew == w && hNew == h) return img; // Found loaded image
        }

        // Directly scaling to less than half size would ignore some pixels.
        // Prevent that by halving the base image size as often as needed.
        while (wNew*2 <= w && hNew*2 <= h) {
            img = createHalvedImage(img);
            w = img.getWidth();
            h = img.getHeight();
        }

        // Do a final resize
        if (wNew != w || hNew != h) {
            img = createResizedImage(img, wNew, hNew);
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
        return createGrayscaleImage(img);
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
