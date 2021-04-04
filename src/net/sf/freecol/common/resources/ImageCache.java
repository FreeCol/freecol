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
import java.awt.image.BufferedImage;

import java.util.HashMap;
import java.util.logging.Logger;
import java.util.Map;

import net.sf.freecol.FreeCol;


/**
 * Wrapper around ResourceManager for caching image resources.
 */
public class ImageCache {

    private static final Logger logger = Logger.getLogger(ImageCache.class.getName());

    public static final String REPLACEMENT_IMAGE = "image.miscicon.delete";

    /** A cache of scaled (and possibly greyed) images. */
    private final Map<Long, BufferedImage> cache;


    /**
     * Initialize this image cache.
     */
    public ImageCache() {
        this.cache = new HashMap<>();
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
    private ImageResource getImageResource(final String key) {
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
     * or it with 15 bits of the width and height, and one more bit for the
     * grayscale boolean.
     *
     * @param key The image key.
     * @param size The size of the image.
     * @param grayscale True if grayscale.
     * @return A unique hash of these parameters.
     */
    public static long imageHash(final String key, final Dimension size,
                                 final boolean grayscale) {
        return (key.hashCode() & 0xFFFFFFFFL)
            ^ ((size.width & 0x7FFFL) << 32)
            ^ ((size.height & 0x7FFFL) << 47)
            ^ ((grayscale) ? (0x1L << 62) : 0L);
    }

    /**
     * Get an image, checking first if it is in the cache.
     *
     * All requests for scaled images come through here, so this is
     * where to maintain the image cache.
     *
     * @param ir The {@code ImageResource} to load from.
     * @param key The name of the resource to return.
     * @param size The size of the requested image.
     *     Rescaling will be performed if necessary.
     * @param grayscale If true return a grayscale image.
     * @return The image identified by {@code resource}.
     */
    private BufferedImage getCachedImage(final ImageResource ir,
                                         final String key,
                                         final Dimension size,
                                         final boolean grayscale) {
        // TODO: add logging but probably in a branch for now
        // until we understand the performance issue better
        final long hashKey = imageHash(key, size, grayscale);
        final BufferedImage cached = this.cache.get(hashKey);
        if (cached != null) return cached;

        BufferedImage image = ir.getImage(size, grayscale);
        if (image != null) this.cache.put(hashKey, image);
        return image;
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
        return getCachedImage(ir, key, d, grayscale);
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
        return getCachedImage(getImageResource(key), key, size, grayscale);
    }

    /**
     * Clear this cache.
     */
    public void clear() {
        this.cache.clear();
    }
}
