/**
 *  Copyright (C) 2002-2019   The FreeCol Team
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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.common.io.sza.SimpleZippedAnimation;
import static net.sf.freecol.common.util.CollectionUtils.*;
import static net.sf.freecol.common.util.StringUtils.*;
import net.sf.freecol.common.util.Utils;


/**
 * Class for getting resources (images, audio etc).
 */
public class ResourceManager {

    private static final Logger logger = Logger.getLogger(ResourceManager.class.getName());

    // TODO: There are no obvious flaws currently, but this could still
    // profit from deeper verification, including checking ResourceMapping and
    // all Resource classes another time, by someone knowledgeable
    // in thread safety issues in Java. 
    // It is currently assumed changing of mappings can happen on any thread,
    // but Resources are only retrieved or cleaned from the AWT thread.

    public static final String REPLACEMENT_IMAGE = "image.miscicon.delete";
    public static final String REPLACEMENT_STRING = "X";

    /**
     * All the mappings are merged in order into this single ResourceMapping.
     */
    private static final ResourceMapping mergedContainer
        = new ResourceMapping();

    /** The thread that handles preloading of resources. */
    private static volatile Thread preloadThread = null;

    /** A cache of scaled (and possibly greyed) images. */
    private static Map<Long, BufferedImage> imageCache = new HashMap<>();


    /**
     * Sets the mappings specified in the date/base-directory.
     * Do not access the mapping after the call.
     *
     * @param name The name of the mapping, for logging purposes.
     * @param mapping The mapping between IDs and files.
     */
    public static synchronized void addMapping(String name,
                                               final ResourceMapping mapping) {
        logger.info("Resource manager adding mapping " + name);
        mergedContainer.addAll(mapping);
        preloadThread = null;
        // TODO: This should wait for the thread to exit, if one
        // was running.
        startBackgroundPreloading();
    }

    /**
     * Clean up easily replaced modified copies in caches.
     */
    public static void clearImageCache() {
        synchronized (imageCache) {
            imageCache.clear();
        }
    }

    /**
     * Get the image cache.
     *
     * @return The image cache.
     */
    public static Map<Long, BufferedImage> getImageCache() {
        return imageCache;
    }

    /**
     * Create and start a new background preload thread.
     */
    private static void startBackgroundPreloading() {
        if (FreeCol.getHeadless()) return; // Do not preload in headless mode

        preloadThread = new Thread(FreeCol.CLIENT_THREAD + "-Resource loader") {
                @Override
                public void run() {
                    // Make a local list of the resources to load.
                    logger.info("Preload background thread started");
                    List<Resource> resources
                        = new ArrayList<>(getResources().values());
                    int n = 0;
                    for (Resource r : resources) {
                        if (preloadThread != this) {
                            logger.info("Preload background thread cancelled"
                                + " after it preloaded " + n + " resources.");
                            return;
                        }
                        // TODO: Filter list before running thread?
                        r.preload();
                        n++;
                    }
                    logger.info("Preload background thread preloaded " + n
                        + " resources.");
                }
            };
        preloadThread.start();
    }

    /**
     * Gets the resource of the given type.
     *
     * @param key The resource to get.
     * @return The resource if there is one with the given
     *     resource key and type, or else {@code null}.
     */
    private static synchronized ColorResource getColorResource(final String key) {
        final ColorResource r = mergedContainer.getColorResource(key);
        if (r == null) logger.warning("getColorResource(" + key + ") failed");
        return r;
    }

    private static synchronized FontResource getFontResource(final String key) {
        final FontResource r = mergedContainer.getFontResource(key);
        if (r == null) logger.warning("getFontResource(" + key + ") failed");
        return r;
    }

    private static synchronized StringResource getStringResource(final String key) {
        final StringResource r = mergedContainer.getStringResource(key);
        if (r == null) logger.warning("getStringResource(" + key + ") failed");
        return r;
    }

    private static synchronized FAFileResource getFAFileResource(final String key) {
        final FAFileResource r = mergedContainer.getFAFileResource(key);
        if (r == null) logger.warning("getFAFileResource(" + key + ") failed");
        return r;
    }

    private static synchronized SZAResource getSZAResource(final String key) {
        final SZAResource r = mergedContainer.getSZAResource(key);
        if (r == null) logger.warning("getSZAResource(" + key + ") failed");
        return r;
    }

    private static synchronized AudioResource getAudioResource(final String key) {
        final AudioResource r = mergedContainer.getAudioResource(key);
        if (r == null) logger.warning("getAudioResource(" + key + ") failed");
        return r;
    }

    private static synchronized VideoResource getVideoResource(final String key) {
        final VideoResource r = mergedContainer.getVideoResource(key);
        if (r == null) logger.warning("getVideoResource(" + key + ") failed");
        return r;
    }

    private static synchronized ImageResource getImageResource(final String key) {
        ImageResource r = mergedContainer.getImageResource(key);
        if (r == null) {
            logger.warning("getImageResource(" + key + ") failed");
            r = mergedContainer.getImageResource(REPLACEMENT_IMAGE);
            if (r == null) {
                FreeCol.fatal(logger, "Failed getting replacement image.");
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
    private static long imageHash(final String key, final Dimension size,
                                  final boolean grayscale) {
        return (key.hashCode() & 0xFFFFFFFFL)
            | ((size.width & 0x7FFFL) << 32)
            | ((size.height & 0x7FFFL) << 47)
            | ((grayscale) ? (0x1L << 62) : 0L);
    }

    /**
     * Invert imageHash.
     *
     * Public for ImageLibrary.getImageResourceSummary.
     *
     * @param h A hash code from imageHash.
     * @return A string in the form "key.width(g|_)height", except the
     *     image key is a hash code.
     */
    public static String imageUnhash(long h) {
        int key = (int)(h & 0xFFFFFFFFL); h >>= 32;
        int width = (int)(h & 0x7FFFL); h >>= 15;
        int height = (int)(h & 0x7FFFL); h >>= 15;
        boolean grey = (h & 0x1L)==1L;
        StringBuilder sb = new StringBuilder(32);
        sb.append(key).append('.').append(width)
            .append((grey) ? 'g' : '_').append(height);
        return sb.toString();
    }

    /**
     * Low level image access.
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
    private static BufferedImage getImage(final ImageResource ir,
                                          final String key,
                                          final Dimension size,
                                          final boolean grayscale) {
        final long hashKey = imageHash(key, size, grayscale);
        final BufferedImage cached = imageCache.get(hashKey);
        if (cached != null) return cached;

        BufferedImage image = ir.getImage(size, grayscale);
        if (image == null) {
            logger.warning("getImage(" + key + ", " + size + ", " + grayscale
                + ") failed");
            if ((image = getImageResource(REPLACEMENT_IMAGE)
                    .getImage(size, grayscale)) == null) {
                FreeCol.fatal(logger, "Failed getting replacement image.");
            }
        } else {
            imageCache.put(hashKey, image);
        }
        return image;
    }


    // Public resource accessors

    public static synchronized boolean hasColorResource(final String key) {
        return mergedContainer.containsColorKey(key);
    }

    public static synchronized boolean hasImageResource(final String key) {
        return mergedContainer.containsImageKey(key);
    }

    public static synchronized boolean hasStringResource(final String key) {
        return mergedContainer.containsStringKey(key);
    }

    public static synchronized boolean hasSZAResource(final String key) {
        return mergedContainer.containsSZAKey(key);
    }
    
    public static synchronized Map<String, Resource> getResources() {
        return mergedContainer.getResources();
    }

    public static synchronized Map<String, ImageResource> getImageResources() {
        return mergedContainer.getImageResources();
    }

    /**
     * Gets an audio resource with the given name.
     * This can return null as there as not all necessary sounds have
     * been added to the game.
     * FIXME: Change calling code to check using hasResource, then
     *        replace null return with calling FreeColClient.fatal on error.
     *
     * @param key The name of the resource to query.
     * @return A {@code File} containing the audio data.
     */
    public static File getAudio(final String key) {
        final AudioResource r = getAudioResource(key);
        return (r == null) ? null : r.getAudio();
    }

    /**
     * Gets a color resource with the given name.
     *
     * @param key The name of the resource to query.
     * @param replacement A fallback color.
     * @return The {@code Color} found, or if not found, the replacement color,
     *     or finally the generic replacement color.
     */
    public static Color getColor(final String key, Color replacement) {
        final ColorResource r = getColorResource(key);
        return (r != null) ? r.getColor()
            : (replacement != null) ? replacement
            : ColorResource.REPLACEMENT_COLOR;
    }

    /**
     * Gets a FAFile resource with the given name.
     * This can return null as there is only one FAFile in FreeCol.
     * FIXME: Consider calling FreeColClient.fatal on error.
     *
     * @param key The name of the resource to query.
     * @return The {@code FAFile} found in a FAFileResource.
     */
    public static FAFile getFAFile(final String key) {
        final FAFileResource r = getFAFileResource(key);
        return (r == null) ? null : r.getFAFile();
    }

    /**
     * Gets the font with the given name.
     *
     * @param key The name of the resource to query.
     * @return The {@code Font} found in a FontResource, or the default
     *     Java font if not found.
     */
    public static Font getFont(final String key) {
        final FontResource r = getFontResource(key);
        if (r == null) return FontResource.getEmergencyFont();
        return r.getFont();
    }

    /**
     * Get the image specified by the given key.
     *
     * Trying to get non-existing images from any of the below methods
     * is an error. To make modding easier and prevent edited resource
     * files from crashing the game, a replacement image is returned
     * and a warning logged.
     *
     * @param key The name of the resource to return.
     * @return The image identified by {@code resource}.
     */
    public static BufferedImage getImage(final String key) {
        final ImageResource ir = getImageResource(key);
        return ir.getImage();
    }

    /**
     * Get the image specified by the given name, scale and grayscale.
     *
     * @param key The name of the resource to return.
     * @param scale The size of the requested image (with 1 being normal size,
     *     2 twice the size, 0.5 half the size etc). Rescaling
     *     will be performed unless using 1.
     * @param grayscale If true return a grayscale image.
     * @return The image identified by {@code resource}.
     */
    public static BufferedImage getImage(final String key, final float scale,
                                         boolean grayscale) {
        final ImageResource ir = getImageResource(key);
        final BufferedImage image = ir.getImage();
        // Shortcut trivial cases
        if (image == null || (scale == 1f && !grayscale)) return image;

        Dimension d = new Dimension(Math.round(image.getWidth() * scale),
                                    Math.round(image.getHeight() * scale));
        return getImage(ir, key, d, grayscale);
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
     *     Rescaling will be performed if necessary.
     * @param grayscale If true return a grayscale image.
     * @return The image identified by {@code resource}.
     */
    public static BufferedImage getImage(final String key,
                                         final Dimension size,
                                         final boolean grayscale) {
        return getImage(getImageResource(key), key, size, grayscale);
    }

    /**
     * Returns a list of all keys starting with the given prefix.
     *
     * @param prefix the prefix
     * @return a list of all keys starting with the given prefix
     */
    public static synchronized List<String> getImageKeys(String prefix) {
        return mergedContainer.getImageKeys(prefix);
    }

    /**
     * Returns a list of all keys starting with the given prefix and
     * ending with the given suffix.
     *
     * @param prefix the prefix
     * @param suffix the suffix
     * @return a list of all resulting keys
     */
    public static synchronized List<String> getImageKeys(String prefix,
                                                         String suffix) {
        return mergedContainer.getImageKeys(prefix, suffix);
    }

    /**
     * Returns a set of all keys starting with the given prefix.
     *
     * @param prefix the prefix
     * @return a set of all keysstarting with the given prefix
     */
    public static synchronized Set<String> getImageKeySet(String prefix) {
        return mergedContainer.getImageKeySet(prefix);
    }

    /**
     * Gets a string resource with the given name.
     * Trying to get a nonexisting string is an error, but returns
     * a replacement string to prevent crashes.
     *
     * @param key The name of the resource to query.
     * @return The string value.
     */
    public static String getString(final String key) {
        final StringResource r = getStringResource(key);
        return (r == null) ? REPLACEMENT_STRING : r.getString();
    }

    /**
     * Returns the animation specified by the given name.
     *
     * As the artwork is still incomplete and animations exist only for
     * some military units, null can still be returned in many cases.
     * FIXME: Check using hasResource before calling this, then replace
     *        null return with calling FreeColClient.fatal on error.
     *
     * @param key The name of the resource to return.
     * @return The animation identified by {@code resource}
     *      or {@code null} if there is no animation
     *      identified by that name.
     */
    public static SimpleZippedAnimation getSZA(final String key) {
        final SZAResource r = getSZAResource(key);
        return (r != null) ? r.getSimpleZippedAnimation() : null;
    }

    /**
     * Returns the animation specified by the given name.
     *
     * @param key The name of the resource to return.
     * @param scale The size of the requested animation (with 1
     *      being normal size, 2 twice the size, 0.5 half the
     *      size etc). Rescaling will be performed unless using 1.
     * @return The animation identified by {@code resource}
     *      or {@code null} if there is no animation
     *      identified by that name.
     */
    public static SimpleZippedAnimation getSZA(final String key,
                                               final float scale) {
        final SZAResource r = getSZAResource(key);
        return (r != null) ? r.getSimpleZippedAnimation(scale) : null;
    }

    /**
     * Gets the {@code Video} represented by the given resource.
     * This can return null as there is only one video in FreeCol.
     * FIXME: Consider calling FreeColClient.fatal on error.
     * 
     * @param key The name of the resource to return.
     * @return The {@code Video} in it's original size.
     */
    public static Video getVideo(final String key) {
        final VideoResource r = getVideoResource(key);
        return (r != null) ? r.getVideo() : null;
    }

    /**
     * Summarize the image resources and image cache contents.
     *
     * @param sb A {@code StringBuilder} to summarize to.
     */
    public static void summarizeImageResources(StringBuilder sb) {
        Set<String> keys = mergedContainer.getImageKeySet();
        sb.append("All keys\n").append(join(" ", keys)).append('\n');
        Map<Integer,String> decode = new HashMap<>();
        for (String k : keys) decode.put(k.hashCode(), k);
        sb.append("Cache\n");
        synchronized (imageCache) {
            forEachMapEntry(imageCache, e -> {
                    Long key = e.getKey();
                    String rep = ResourceManager.imageUnhash(key);
                    int i = Integer.parseInt(firstPart(rep, "."));
                    sb.append(decode.get(i)).append('.')
                        .append(lastPart(rep, ".")).append('\n');
                });
        }
    }
}
