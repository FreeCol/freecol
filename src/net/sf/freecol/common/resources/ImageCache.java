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

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.freecol.FreeCol;


/**
 * Wrapper around ResourceManager for caching image resources.
 */
public class ImageCache {

    private static final Logger logger = Logger.getLogger(ImageCache.class.getName());

    private static final boolean DEBUG_PRINT_CACHE_SIZES_TO_STDOUT = false;
    
    public static final String REPLACEMENT_IMAGE = "image.miscicon.delete";

    /** A cache of scaled (and possibly greyed) images. */
    private final Map<Long, BufferedImage> cache;
    private final Map<Long, BufferedImage> lowPriorityCache;
    
    private long cacheSize = 0;
    private long lowPriorityCacheSize = 0;


    /**
     * Initialize this image cache.
     */
    public ImageCache() {
        this.cache = new HashMap<>();
        this.lowPriorityCache = new HashMap<>();
    }


    /**
     * Get an image resource by key from the resource manager.
     *
     * Fall back to a replacement image on failure.  Failure to find
     * that is fatal.  Image resource failure has already been logged.
     *
     * @param key The key to look up.
     * @return The {@code ImageResource} found.
     */
    public static ImageResource getImageResource(final String key) {
        ImageResource r = ResourceManager.getImageResource(key, false);
        if (r == null) {
            r = ResourceManager.getImageResource(REPLACEMENT_IMAGE, false);
            if (r == null) {
                FreeCol.fatal(logger, "Replacement image not found: "
                    + REPLACEMENT_IMAGE);
            }
        }
        return r;
    }

    /**
     * Hash function for images.
     *
     * We accept the standard 32-bit int hashCode of the key, and
     * or it with 12 bits of the width and height, 6 bits for variations and
     * one more bit for the grayscale boolean.
     *
     * @param key The image key.
     * @param size The size of the image.
     * @param grayscale True if grayscale.
     * @param variation The image variation.
     * @return A unique hash of these parameters.
     */
    public static long imageHash(final String key, final Dimension size,
                                 final boolean grayscale, final int variation) {
        if (variation >= 64) {
            throw new IllegalStateException("Update formula below in order to support more variations.");
        }
        if (size.width >= 4096) {
            throw new IllegalStateException("Update formula below in order to support larger image width.");
        }
        if (size.height >= 4096) {
            throw new IllegalStateException("Update formula below in order to support larger image heights.");
        }
        return (key.hashCode() & 0xFFFFFFFFL)
            ^ ((size.width & 0x7FFFL) << 32)
            ^ ((size.height & 0x7FFFL) << 44)
            ^ (variation << 56)
            ^ ((grayscale) ? (0x1L << 62) : 0L)
            + variation;
    }

    /**
     * Get an image, checking first if it is in the cache.
     *
     * All requests for scaled images come through here, so this is
     * where to maintain the image cache.
     *
     * @param ir The {@code ImageResource} to load from.
     * @param size The size of the requested image.
     *     Rescaling will be performed if necessary.
     * @param grayscale If true return a grayscale image.
     * @param variation The image variation.
     * @return The image identified by {@code resource}.
     */
    public BufferedImage getCachedImage(final ImageResource ir,
                                         final Dimension size,
                                         final boolean grayscale,
                                         final int variation) {

        final long cacheKey = imageHash(ir.getCachingKey(), size, grayscale, variation);
        final BufferedImage cached = searchCaches(cacheKey);
        if (cached != null) {
            return cached;
        }

        BufferedImage image = ir.getImage(variation, size, grayscale);
        placeImageInCache(cacheKey, image);
        return image;
    }
    
    public BufferedImage getCachedImageOrGenerate(final String key,
            final Dimension size,
            final boolean grayscale,
            final int variation,
            Callable<BufferedImage> imageCreator) {
        
        final long cacheKey = imageHash(key, size, grayscale, variation);
        final BufferedImage cached = searchCaches(cacheKey);
        if (cached != null) {
            return cached;
        }
        
        try {
            final BufferedImage image = imageCreator.call();
            placeImageInLowPriorityCache(cacheKey, image);
            return image;
        } catch (Exception e) {
            logger.log(Level.WARNING, "Exception while producing image", e);
            return null;
        }
    }
    
    private BufferedImage searchCaches(long cacheKey) {
        BufferedImage image = this.cache.get(cacheKey);
        if (image != null) {
            return image;
        }
        image = this.lowPriorityCache.get(cacheKey);
        if (image != null) {
            return image;
        }
        return null;
    }
    
    private void placeImageInCache(long hashKey, BufferedImage image) {
        if (image != null) {
            this.cache.put(hashKey, image);
            cacheSize += image.getWidth() * image.getHeight() * 4;
            debugPrintCacheSizes();
        }
    }
    
    private void placeImageInLowPriorityCache(long hashKey, BufferedImage image) {
        if (image != null) {
            this.lowPriorityCache.put(hashKey, image);
            lowPriorityCacheSize += image.getWidth() * image.getHeight() * 4;
            debugPrintCacheSizes();
        }
    }

    private void debugPrintCacheSizes() {
        if (DEBUG_PRINT_CACHE_SIZES_TO_STDOUT) {
            System.out.format("Cache: %4sMB   Low priority cache: %4sMB\n", Math.round(cacheSize / (1024 * 1024)), Math.round(lowPriorityCacheSize / (1024 * 1024)));
        }
    }

    // Public interface

    /**
     * Get the image specified by the given name, scale and grayscale.
     *
     * @param key The name of the resource to return.
     * @param scale A scaling to apply to the base image size.
     * @param grayscale If true return a grayscale image.
     * @return The image found.
     */
    public BufferedImage getScaledImage(final String key,
                                        final float scale,
                                        boolean grayscale) {
        final ImageResource ir = getImageResource(key);
        final BufferedImage image = ir.getImage();
        // Shortcut trivial cases
        if (image == null) return null;

        Dimension d = new Dimension(Math.round(image.getWidth() * scale),
                                    Math.round(image.getHeight() * scale));
        return getCachedImage(ir, d, grayscale, 0);
    }

    /**
     * Get the image specified by the given name, size and grayscale.
     *
     * Please, avoid using too many different sizes!
     * For each is a scaled image cached here for a long time,
     * which wastes memory if you are not careful.
     *
     * @param key The name of the resource to return.
     * @param size The size of the requested image.
     * @param grayscale If true return a grayscale image.
     * @return The image found.
     */
    public BufferedImage getSizedImage(final String key,
                                       final Dimension size,
                                       final boolean grayscale) {
        return getSizedImage(key, size, grayscale, 0);
    }
    
    /**
     * Get the image specified by the given name, size and grayscale.
     *
     * Please, avoid using too many different sizes!
     * For each is a scaled image cached here for a long time,
     * which wastes memory if you are not careful.
     *
     * @param key The name of the resource to return.
     * @param size The size of the requested image.
     * @param grayscale If true return a grayscale image.
     * @param seed A seed used for getting the same "random" picture every
     *      time.
     * @return The image found.
     */
    public BufferedImage getSizedImage(final String key,
                                       final Dimension size,
                                       final boolean grayscale,
                                       final int seed) {
        final ImageResource ir = getImageResource(key);
        return getCachedImage(ir, size, grayscale, ir.getVariationNumberForSeed(seed));
    }

    /**
     * Clear this cache.
     */
    public void clear() {
        this.cache.clear();
        this.cacheSize = 0;
        
        clearLowPriorityCache();
    }
    
    public void clearLowPriorityCache() {
        this.lowPriorityCache.clear();
        this.lowPriorityCacheSize = 0;
    }
}
