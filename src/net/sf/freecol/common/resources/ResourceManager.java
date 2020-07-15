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

    /**
     * All the mappings are merged in order into this single ResourceMapping.
     */
    private static final ResourceMapping mergedContainer
        = new ResourceMapping();

    /** The thread that handles preloading of resources. */
    private static volatile Thread preloadThread = null;


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

    public static synchronized ImageResource getImageResource(final String key) {
        // Public for ImageCache
        ImageResource r = mergedContainer.getImageResource(key);
        if (r == null) logger.warning("getImageResource(" + key + ") failed");
        return r;
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
     * @param key The name of the resource to return.
     * @return The image identified by {@code resource}.
     */
    public static BufferedImage getImage(final String key) {
        ImageResource ir = getImageResource(key);
        return (ir == null) ? null : ir.getImage();
    }

    /**
     * Get the image specified by the given name, size and grayscale.
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
        ImageResource ir = getImageResource(key);
        return (ir == null) ? null : ir.getImage(size, grayscale);
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
        return (r == null) ? StringResource.REPLACEMENT_STRING : r.getString();
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
     * Summarize the image resources.
     *
     * @param sb A {@code StringBuilder} to summarize to.
     */
    public static void summarizeImageResources(StringBuilder sb) {
        Set<String> keys = mergedContainer.getImageKeySet();
        sb.append("All keys\n").append(join(" ", keys)).append('\n');
    }
}
