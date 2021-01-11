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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.AbstractQueue;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;
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

    /** Thread-safe queue of mappings for the preload thread to process. */
    private static final AbstractQueue<ResourceMapping> preloadQueue
        = new ConcurrentLinkedQueue<>();
    
    /** The thread that handles preloading of resources. */
    private static Thread preloadThread = null;

    /** Flag to inform the preload thead that all mappings are queued. */
    private static volatile boolean preloadDone = false;

    /**
     * All the mappings are merged in order into this single ResourceMapping.
     */
    private static final ResourceMapping mergedContainer
        = new ResourceMapping();


    /**
     * Sets the mappings specified in the date/base-directory.
     * Do not access the mapping after the call.
     *
     * @param name The name of the mapping, for logging purposes.
     * @param mapping The mapping between IDs and files.
     */
    public static void addMapping(String name, ResourceMapping mapping) {
        logger.info("Resource manager adding mapping " + name);
        preloadQueue.add(mapping); // thread-safe
        startPreloading();
    }

    /**
     * Create and start a new background preload thread.
     *
     * Synchronization protects preloadThread.  The thread is the only place
     * mergedContainer is written.
     */
    private static synchronized void startPreloading() {
        if (FreeCol.getHeadless()) return; // Do not preload in headless mode
        if (preloadThread != null) return;

        preloadDone = false;
        preloadThread = new Thread(FreeCol.CLIENT_THREAD + "-Resource loader") {
                /**
                 * {@inheritDoc}
                 */
                @Override
                public void run() {
                    logger.info("Preload thread started");
                    int n = 0;
                    while (true) {
                        ResourceMapping mapping = preloadQueue.poll();
                        if (mapping == null) { // Quit if done, else wait
                            if (preloadDone) break;
                            Utils.delay(10, null);
                        } else { // Preload the mapping found
                            mergedContainer.addAll(mapping);
                            n += mapping.preload();
                        }
                    }
                    preloadThread = null;
                    logger.info("Preload done, " + n + " resources.");
                }
            };
        preloadThread.start();
    }

    /**
     * Signal to the preload thread that no further mappings need to
     * be loaded.
     */
    public static synchronized void finishPreloading() {
        preloadDone = true;
    }


    // Normally these accessors *should* be synchronized, but do not
    // need to be because the mergedContainer is never written again
    // after the preload thread is done.

    /**
     * Get an audio resource.
     *
     * @param key The resource to get.
     * @param warn Log a warning if the resource is not found.
     * @return The resource if there is one with the given
     *     resource key and type, or else {@code null}.
     */
    public static AudioResource getAudioResource(String key, boolean warn) {
        final AudioResource r = mergedContainer.getAudioResource(key);
        if (warn && r == null) logger.warning("getAudioResource(" + key + ") failed");
        return r;
    }

    /**
     * Get a color resource.
     *
     * @param key The resource to get.
     * @param warn Log a warning if the resource is not found.
     * @return The resource if there is one with the given
     *     resource key and type, or else {@code null}.
     */
    public static ColorResource getColorResource(String key, boolean warn) {
        final ColorResource r = mergedContainer.getColorResource(key);
        if (warn && r == null) logger.warning("getColorResource(" + key + ") failed");
        return r;
    }

    /**
     * Get a FAFile resource.
     *
     * @param key The resource to get.
     * @param warn Log a warning if the resource is not found.
     * @return The resource if there is one with the given
     *     resource key and type, or else {@code null}.
     */
    public static FAFileResource getFAFileResource(String key, boolean warn) {
        final FAFileResource r = mergedContainer.getFAFileResource(key);
        if (warn && r == null) logger.warning("getFAFileResource(" + key + ") failed");
        return r;
    }

    /**
     * Get a font resource.
     *
     * @param key The resource to get.
     * @param warn Log a warning if the resource is not found.
     * @return The resource if there is one with the given
     *     resource key and type, or else {@code null}.
     */
    public static FontResource getFontResource(String key, boolean warn) {
        final FontResource r = mergedContainer.getFontResource(key);
        if (warn && r == null) logger.warning("getFontResource(" + key + ") failed");
        return r;
    }

    /**
     * Get an image resource.
     *
     * @param key The resource to get.
     * @param warn Log a warning if the resource is not found.
     * @return The resource if there is one with the given
     *     resource key and type, or else {@code null}.
     */
    public static ImageResource getImageResource(String key, boolean warn) {
        // Public for ImageCache
        final ImageResource r = mergedContainer.getImageResource(key);
        if (warn && r == null) logger.warning("getImageResource(" + key + ") failed");
        return r;
    }

    /**
     * Get a string resource.
     *
     * @param key The resource to get.
     * @param warn Log a warning if the resource is not found.
     * @return The resource if there is one with the given
     *     resource key and type, or else {@code null}.
     */
    public static StringResource getStringResource(String key, boolean warn) {
        final StringResource r = mergedContainer.getStringResource(key);
        if (warn && r == null) logger.warning("getStringResource(" + key + ") failed");
        return r;
    }

    /**
     * Get a SZA resource.
     *
     * @param key The resource to get.
     * @param warn Log a warning if the resource is not found.
     * @return The resource if there is one with the given
     *     resource key and type, or else {@code null}.
     */
    public static SZAResource getSZAResource(String key, boolean warn) {
        final SZAResource r = mergedContainer.getSZAResource(key);
        if (warn && r == null) logger.warning("getSZAResource(" + key + ") failed");
        return r;
    }

    /**
     * Get a video resource.
     *
     * @param key The resource to get.
     * @param warn Log a warning if the resource is not found.
     * @return The resource if there is one with the given
     *     resource key and type, or else {@code null}.
     */
    public static VideoResource getVideoResource(String key, boolean warn) {
        final VideoResource r = mergedContainer.getVideoResource(key);
        if (warn && r == null) logger.warning("getVideoResource(" + key + ") failed");
        return r;
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
        final AudioResource r = getAudioResource(key, true);
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
        final ColorResource r = getColorResource(key, true);
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
        final FAFileResource r = getFAFileResource(key, true);
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
        final FontResource r = getFontResource(key, true);
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
        ImageResource ir = getImageResource(key, true);
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
        ImageResource ir = getImageResource(key, true);
        return (ir == null) ? null : ir.getImage(size, grayscale);
    }

    /**
     * Get a list of all image keys starting with the given prefix.
     *
     * @param prefix The prefix.
     * @return A list of all image resource keys starting with the prefix.
     */
    public static List<String> getImageKeys(String prefix) {
        return transform(mergedContainer.getImageKeySet(),
                         k -> k.startsWith(prefix));
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
        final StringResource r = getStringResource(key, true);
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
        final SZAResource r = getSZAResource(key, true);
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
        final SZAResource r = getSZAResource(key, true);
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
        final VideoResource r = getVideoResource(key, true);
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
