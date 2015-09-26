/**
 *  Copyright (C) 2002-2015   The FreeCol Team
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.common.io.sza.SimpleZippedAnimation;


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
     * The following fields are mappings from resource IDs to
     * resources.  A mapping is defined within a specific context.
     * See the comment on each field's setter for more information:
     */
    private static ResourceMapping baseMapping;
    private static ResourceMapping tcMapping;
    private static ResourceMapping scenarioMapping;
    private static ResourceMapping modMapping;

    /**
     * All the mappings above merged into this single ResourceMapping
     * according to precendence.
     */
    private static ResourceMapping mergedContainer;

    private static volatile Thread preloadThread = null;


    /**
     * Sets the mappings specified in the date/base-directory.
     * Do not access the mapping after the call.
     *
     * @param mapping The mapping between IDs and files.
     */
    public static synchronized void setBaseMapping(final ResourceMapping mapping) {
        logger.info("setBaseMapping " + mapping);
        baseMapping = mapping;
        update(mapping != null);
    }

    /**
     * Sets the mappings specified for a Total Conversion (TC).
     * Do not access the mapping after the call.
     *
     * @param mapping The mapping between IDs and files.
     */
    public static synchronized void setTcMapping(final ResourceMapping mapping) {
        logger.info("setTcMapping " + mapping);
        tcMapping = mapping;
        update(mapping != null);
    }

    /**
     * Sets the mappings specified by mods.
     * Do not access the mapping after the call.
     *
     * @param mapping A list of the mappings between IDs and files.
     */
    public static synchronized void setModMapping(final ResourceMapping mapping) {
        logger.info("setModMapping " + mapping);
        modMapping = mapping;
        update(mapping != null);
    }

    /**
     * Sets the mappings specified in a scenario.
     * Do not access the mapping after the call.
     *
     * @param mapping The mapping between IDs and files.
     */
    public static synchronized void setScenarioMapping(final ResourceMapping mapping) {
        logger.info("setScenarioMapping " + mapping);
        scenarioMapping = mapping;
        // As this is called when loading a new savegame,
        // use it as a hint for cleaning up
        clean();
        update(mapping != null);
    }

    /**
     * Clean up easily replaced modified copies in caches.
     */
    public static synchronized void clean() {
        if(baseMapping != null) {
            for (Map.Entry<String,ImageResource> entry
                : baseMapping.getImageResources().entrySet()) {
                ImageResource resource = entry.getValue();
                resource.clean();
            }
        }
        if(tcMapping != null) {
            for (Map.Entry<String,ImageResource> entry
                : tcMapping.getImageResources().entrySet()) {
                ImageResource resource = entry.getValue();
                resource.clean();
            }
        }
        if(scenarioMapping != null) {
            for (Map.Entry<String,ImageResource> entry
                : scenarioMapping.getImageResources().entrySet()) {
                ImageResource resource = entry.getValue();
                resource.clean();
            }
        }
        if(modMapping != null) {
            for (Map.Entry<String,ImageResource> entry
                : modMapping.getImageResources().entrySet()) {
                ImageResource resource = entry.getValue();
                resource.clean();
            }
        }
        System.gc();
    }

    /**
     * Updates the resource mappings after making changes.
     * 
     * @param newItems If new items have been added.
     */
    private static void update(boolean newItems) {
        logger.finest("update(" + newItems + ")");
        if(newItems) {
            preloadThread = null;
        }
        createMergedContainer();
        if(newItems) {
            // TODO: This should wait for the thread to exit, if one was running.
            startBackgroundPreloading();
        }
    }

    /**
     * Creates a merged container containing all the resources.
     */
    private static void createMergedContainer() {
        ResourceMapping mc = new ResourceMapping();
        mc.addAll(baseMapping);
        mc.addAll(tcMapping);
        mc.addAll(scenarioMapping);
        mc.addAll(modMapping);
        mergedContainer = mc;
    }

    /**
     * Create and start a new background preload thread.
     */
    private static void startBackgroundPreloading() {
        if ("true".equals(System.getProperty("java.awt.headless", "false"))) {
            return; // Do not preload in headless mode
        }

        preloadThread = new Thread(FreeCol.CLIENT_THREAD + "-Resource loader") {
                @Override
                public void run() {
                    // Make a local list of the resources to load.
                    logger.info("Background thread started");
                    List<Resource> resources
                        = new ArrayList<>(getResources().values());
                    int n = 0;
                    for (Resource r : resources) {
                        if (preloadThread != this) {
                            logger.info(
                                "Background thread cancelled after it preloaded "
                                + n + " resources.");
                            return;
                        }
                        // TODO: Filter list before running thread?
                        if (r instanceof Resource.Preloadable) {
                            ((Resource.Preloadable)r).preload();
                            n++;
                        }
                    }
                    logger.info("Background thread preloaded " + n
                        + " resources.");
                }
            };
        preloadThread.setPriority(2);
        preloadThread.start();
    }

    /**
     * When it is anticipated that a resource could not exist use
     * this method for checking beforehand.
     * Other metods below are specializations running faster or
     * allowing to check for many keys at once.
     * 
     * @param key The resource to check for.
     * @return true when the resource exists.
     */
    public static synchronized boolean hasResource(final String key) {
        logger.finest("hasResource(" + key + ")");
        return mergedContainer.containsKey(key);
    }

    public static synchronized boolean hasImageResource(final String key) {
        //logger.finest("hasImageResource(" + key + ")");
        return mergedContainer.containsImageKey(key);
    }

    public static synchronized boolean hasColorResource(final String key) {
        //logger.finest("hasColorResource(" + key + ")");
        return mergedContainer.containsColorKey(key);
    }

    public static synchronized Map<String, Resource> getResources() {
        logger.finest("getResources");
        return mergedContainer.getResources();
    }

    public static synchronized Map<String, ImageResource> getImageResources() {
        logger.finest("getImageResources");
        return mergedContainer.getImageResources();
    }

    /**
     * Returns a list of all keys starting with the given prefix.
     *
     * @param prefix the prefix
     * @return a list of all keys starting with the given prefix
     */
    public static synchronized List<String> getImageKeys(String prefix) {
        //logger.finest("getImageKeys(" + prefix + ")");
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
        logger.finest("getImageKeys(" + prefix + ", " + suffix + ")");
        return mergedContainer.getImageKeys(prefix, suffix);
    }

    /**
     * Returns a set of all keys starting with the given prefix.
     *
     * @param prefix the prefix
     * @return a set of all keysstarting with the given prefix
     */
    public static synchronized Set<String> getImageKeySet(String prefix) {
        //logger.finest("getImageKeySet(" + prefix + ")");
        return mergedContainer.getImageKeySet(prefix);
    }

    /**
     * Gets the resource of the given type.
     *
     * @param key The resource to get.
     * @return The resource if there is one with the given
     *     resource key and type, or else <code>null</code>.
     */
    private static synchronized ColorResource getColorResource(final String key) {
        final ColorResource r = mergedContainer.getColorResource(key);
        if (r == null) {
            logger.warning("getColorResource(" + key + ") failed");
        }
        return r;
    }

    private static synchronized FontResource getFontResource(final String key) {
        final FontResource r = mergedContainer.getFontResource(key);
        if (r == null) {
            logger.warning("getFontResource(" + key + ") failed");
        }
        return r;
    }

    private static synchronized StringResource getStringResource(final String key) {
        final StringResource r = mergedContainer.getStringResource(key);
        if (r == null) {
            logger.warning("getStringResource(" + key + ") failed");
        }
        return r;
    }

    private static synchronized FAFileResource getFAFileResource(final String key) {
        final FAFileResource r = mergedContainer.getFAFileResource(key);
        if (r == null) {
            logger.warning("getFAFileResource(" + key + ") failed");
        }
        return r;
    }

    private static synchronized SZAResource getSZAResource(final String key) {
        final SZAResource r = mergedContainer.getSZAResource(key);
        if (r == null) {
            logger.warning("getSZAResource(" + key + ") failed");
        }
        return r;
    }

    private static synchronized AudioResource getAudioResource(final String key) {
        final AudioResource r = mergedContainer.getAudioResource(key);
        if (r == null) {
            logger.warning("getAudioResource(" + key + ") failed");
        }
        return r;
    }

    private static synchronized VideoResource getVideoResource(final String key) {
        final VideoResource r = mergedContainer.getVideoResource(key);
        if (r == null) {
            logger.warning("getVideoResource(" + key + ") failed");
        }
        return r;
    }

    private static synchronized ImageResource getImageResource(final String key) {
        ImageResource r = mergedContainer.getImageResource(key);
        if (r == null) {
            logger.warning("getImageResource(" + key + ") failed");
            r = mergedContainer.getImageResource(REPLACEMENT_IMAGE);
            if(r == null) {
                FreeColClient.fatal("Failed getting replacement image.");
            }
        }
        return r;
    }

    /**
     * Returns the image specified by the given key.
     * Trying to get non-existing images from any of the below methods
     * is an error. To make modding easier and prevent edited resource
     * files from crashing the game, a replacement image is returned
     * and a warning logged.
     *
     * @param key The name of the resource to return.
     * @return The image identified by <code>resource</code>.
     */
    public static BufferedImage getImage(final String key) {
        BufferedImage image = getImageResource(key).getImage();
        if(image == null) {
            logger.warning("getImage(" + key + ") failed");
            image = getImageResource(REPLACEMENT_IMAGE).getImage();
            if(image == null) {
                FreeColClient.fatal("Failed getting replacement image.");
            }
        }
        return image;
    }

    /**
     * Returns the image specified by the given name.
     * Please, avoid using too many different scaling factors!
     * For each is a scaled image cached here for a long time,
     * which wastes memory if you are not careful.
     *
     * @param key The name of the resource to return.
     * @param scale The size of the requested image (with 1 being normal size,
     *      2 twice the size, 0.5 half the size etc). Rescaling
     *      will be performed unless using 1.
     * @return The image identified by <code>resource</code>.
     */
    public static BufferedImage getImage(final String key, final float scale) {
        BufferedImage image = getImageResource(key).getImage(scale);
        if(image == null) {
            logger.warning("getImage(" + key + ", " + scale + ") failed");
            image = getImageResource(REPLACEMENT_IMAGE).getImage(scale);
            if(image == null) {
                FreeColClient.fatal("Failed getting replacement image.");
            }
        }
        return image;
    }

    /**
     * Returns the image specified by the given name.
     * Please, avoid using this, as for each size another scaled version
     * of the image is cached!
     *
     * @param key The name of the resource to return.
     * @param size The size of the requested image. Rescaling
     *      will be performed if necessary.
     * @return The image identified by <code>resource</code>.
     */
    public static BufferedImage getImage(final String key, final Dimension size) {
        BufferedImage image = getImageResource(key).getImage(size);
        if(image == null) {
            logger.warning("getImage(" + key + ", " + size + ") failed");
            image = getImageResource(REPLACEMENT_IMAGE).getImage(size);
            if(image == null) {
                FreeColClient.fatal("Failed getting replacement image.");
            }
        }
        return image;
    }

    /**
     * Returns the a grayscale version of the image specified by
     * the given name.
     *
     * @param key The name of the resource to return.
     * @param size The size of the requested image. Rescaling
     *      will be performed if necessary.
     * @return The image identified by <code>resource</code>.
     */
    public static BufferedImage getGrayscaleImage(final String key, final Dimension size) {
        BufferedImage image = getImageResource(key).getGrayscaleImage(size);
        if(image == null) {
            logger.warning("getGrayscaleImage(" + key + ", " + size + ") failed");
            image = getImageResource(REPLACEMENT_IMAGE).getGrayscaleImage(size);
            if(image == null) {
                FreeColClient.fatal("Failed getting replacement image.");
            }
        }
        return image;
    }

    /**
     * Returns the grayscale version of the image specified by the given name.
     *
     * @param key The name of the resource to return.
     * @param scale The size of the requested image (with 1 being normal size,
     *      2 twice the size, 0.5 half the size etc). Rescaling
     *      will be performed unless using 1.
     * @return The image identified by <code>resource</code>.
     */
    public static BufferedImage getGrayscaleImage(final String key, final float scale) {
        BufferedImage image = getImageResource(key).getGrayscaleImage(scale);
        if(image == null) {
            logger.warning("getGrayscaleImage(" + key + ", " + scale + ") failed");
            image = getImageResource(REPLACEMENT_IMAGE).getGrayscaleImage(scale);
            if(image == null) {
                FreeColClient.fatal("Failed getting replacement image.");
            }
        }
        return image;
    }

    /**
     * Returns the animation specified by the given name.
     * As the artwork is still incomplete and animations exist only for
     * some military units, null can still be returned in many cases.
     * FIXME: Check using hasResource before calling this, then replace
     *        null return with calling FreeColClient.fatal on error.
     *
     * @param key The name of the resource to return.
     * @return The animation identified by <code>resource</code>
     *      or <code>null</code> if there is no animation
     *      identified by that name.
     */
    public static SimpleZippedAnimation getSimpleZippedAnimation(final String key) {
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
     * @return The animation identified by <code>resource</code>
     *      or <code>null</code> if there is no animation
     *      identified by that name.
     */
    public static SimpleZippedAnimation getSimpleZippedAnimation(final String key, final float scale) {
        final SZAResource r = getSZAResource(key);
        return (r != null) ? r.getSimpleZippedAnimation(scale) : null;
    }

    /**
     * Returns the <code>Color</code> with the given name.
     * Trying to get a non-existing color is an error and will return
     * magenta as replacement color to prevent crashes when modding.
     *
     * @param key The name of the resource to return.
     * @return An <code>Color</code> created with the image
     *      identified by <code>resource</code> or
     *      a replacement color if there is no color identified
     *      by that name.
     */
    public static Color getColor(final String key) {
        final ColorResource r = getColorResource(key);
        return (r == null) ? ColorResource.REPLACEMENT_COLOR : r.getColor();
    }

    /**
     * Gets the font with the given name.
     * Trying to get a nonexisting font is an error and will result
     * in only getting the emergency font to prevent crashes when modding.
     *
     * @param key The name of the resource to query.
     * @return The <code>Font</code> found in a FontResource, which
     *     may default to the Java default font if the resource failed
     *     to load.
     */
    public static Font getFont(final String key) {
        final FontResource r = getFontResource(key);
        if (r == null) return FontResource.getEmergencyFont();
        return r.getFont();
    }

    /**
     * Gets an audio resource with the given name.
     * This can return null as there as not all necessary sounds have
     * been added to the game.
     * FIXME: Change calling code to check using hasResource, then
     *        replace null return with calling FreeColClient.fatal on error.
     *
     * @param key The name of the resource to query.
     * @return A <code>File</code> containing the audio data.
     */
    public static File getAudio(final String key) {
        final AudioResource r = getAudioResource(key);
        return (r == null) ? null : r.getAudio();
    }

    /**
     * Gets the <code>Video</code> represented by the given resource.
     * This can return null as there is only one video in FreeCol.
     * FIXME: Consider calling FreeColClient.fatal on error.
     * 
     * @param key The name of the resource to return.
     * @return The <code>Video</code> in it's original size.
     */
    public static Video getVideo(final String key) {
        final VideoResource r = getVideoResource(key);
        return (r != null) ? r.getVideo() : null;
    }

    /**
     * Gets a FAFile resource with the given name.
     * This can return null as there is only one FAFile in FreeCol.
     * FIXME: Consider calling FreeColClient.fatal on error.
     *
     * @param key The name of the resource to query.
     * @return The <code>FAFile</code> found in a FAFileResource.
     */
    public static FAFile getFAFile(final String key) {
        final FAFileResource r = getFAFileResource(key);
        return (r == null) ? null : r.getFAFile();
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

}
