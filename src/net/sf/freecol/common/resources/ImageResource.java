/**
 *  Copyright (C) 2002-2022   The FreeCol Team
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

import static net.sf.freecol.common.util.CollectionUtils.find;
import static net.sf.freecol.common.util.CollectionUtils.first;
import static net.sf.freecol.common.util.ImageUtils.createGrayscaleImage;
import static net.sf.freecol.common.util.ImageUtils.createResizedImage;
import static net.sf.freecol.common.util.ImageUtils.wildcardDimension;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import net.sf.freecol.common.resources.Resource.Cleanable;


/**
 * A {@code Resource} wrapping an {@code Image}.
 * @see Resource
 */
public class ImageResource extends Resource implements Cleanable {

    private static final Logger logger = Logger.getLogger(ImageResource.class.getName());

    /** Comparator to compare buffered images by ascending size. */
    private static final Comparator<BufferedImage> biComp
        = Comparator.<BufferedImage>comparingInt(bi ->
            bi.getWidth() * bi.getHeight());

    /**
     * Experimental flag for forcing lowest quality (for now using bitmask instead of
     * full transparency ... later perhaps also fewer colors.
     * 
     * We should make a separate class for image configurations if we keep this
     * longterm.
     */
    private static boolean forceLowestQuality = false;
    
    private volatile BufferedImage image = null;
    private List<URI> alternativeLocators = null;
    private List<BufferedImage> loadedImages = null;
    private List<ImageResource> variations = new ArrayList<>();


    /**
     * Do not use directly.
     *
     * @param cachingKey The caching key.
     * @param resourceLocator The {@code URI} used when loading this resource.
     */
    public ImageResource(String cachingKey, URI resourceLocator) {
        super(cachingKey, resourceLocator);
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
    
    public synchronized void addVariation(ImageResource imageResource) {
        this.variations.add(imageResource);
    }
    
    /**
     * Adds another URIs for loading a differently sized version of the image.
     * Only use before preload got called!
     *
     * @param uris A {@code List} of {@code URI}s used when loading.
     */
    public synchronized void addAlternativeResourceLocators(List<URI> uris) {
        if (this.alternativeLocators == null)
            this.alternativeLocators = new ArrayList<>();
        this.alternativeLocators.addAll(uris);
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
    
    public int getVariationNumberForSeed(int seed) {
        return new Random(seed).nextInt(variations.size() + 1);
    }
    
    public int getVariationNumberForTick(long ticks) {
        return (int) (ticks % (variations.size() + 1));
    }
    
    public int getNumberOfVariations() {
        return variations.size() + 1;
    }
    
    public ImageResource getVariation(int variationNumber) {
        if (variations.isEmpty()) {
            return this;
        }
        
        if (variationNumber >= variations.size()) {
            return this;
        }
        return variations.get(variationNumber);
    }
    
    
    /**
     * Gets the image using the specified dimension and choice of grayscale.
     * 
     * @param variation The image variation.
     * @param d The {@code Dimension} of the requested image.
     * @param grayscale If true return a grayscale image.
     * @return The scaled {@code BufferedImage}.
     */
    public BufferedImage getImage(int variation, Dimension d, boolean grayscale) {
        return (grayscale) ? getVariation(variation).getGrayscaleImage(d) : getVariation(variation).getColorImage(d);
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

        /*
         * Tileable images should not be scaled with interpolation, as this will produce
         * partially transparent pixels. These pixels will overlap and make visible
         * seams when tiling images.
         * 
         * Do NOT use:
         *  // Directly scaling to less than half size would ignore some pixels.
         *  // Prevent that by halving the base image size as often as needed.
         *  while (wNew*2 <= w && hNew*2 <= h) {
         *      img = createHalvedImage(img);
         *      w = img.getWidth();
         *      h = img.getHeight();
         *  }
         */

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
            for (ImageResource variation : variations) {
                variation.preload();
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
            
            if (image == null) {
                logger.log(Level.WARNING, "Failed to read image from: " + uri);
                return null;
            }

            
            if (canUseBitmask(uri)) {
                final BufferedImage compatibleImage = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration()
                        .createCompatibleImage(image.getWidth(), image.getHeight(), Transparency.BITMASK);
                final Graphics2D g = compatibleImage.createGraphics();
                g.drawImage(image, 0, 0, null);
                g.dispose();
                return compatibleImage;
            } else {
                return image;
            }

        } catch (IOException ioe) {
            logger.log(Level.WARNING, "Exception to loading image from: "
                + uri, ioe);
        }
        return null;
    }

    @Override
    public void clean() {
        image = null;
        if (loadedImages != null) {
            loadedImages.clear();
        }
        if (variations != null) {
            variations.stream().forEach(v -> v.clean());
        }
    }

    private static boolean canUseBitmask(URI uri) {
        /* TODO: Better method for determining images that can use a bitmask. */
        return forceLowestQuality || uri.toString().contains("center") && !uri.toString().contains("mask");
    }
    
    public static final void forceLowestQuality(boolean forceLowestQuality) {
        // Deactive for testing:
        //ImageResource.forceLowestQuality = forceLowestQuality;
    }
    
    public static final boolean isForceLowestQuality() {
        return forceLowestQuality;
    }
}
