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

    /**
     * The following fields are mappings from resource IDs to
     * resources.  A mapping is defined within a specific context.
     * See the comment on each field's setter for more information:
     */
    private static ResourceMapping baseMapping;
    private static ResourceMapping tcMapping;
    private static ResourceMapping scenarioMapping;
    // TODO: Check if mod resources are always added in a predetermined fixed order.
    private static List<ResourceMapping> modMappings = new ArrayList<>();

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
     * Do not access the mappings after the call.
     *
     * @param mappings A list of the mappings between IDs and files.
     */
    public static synchronized void setModMappings(final List<ResourceMapping> mappings) {
        logger.info("setModMappings size " + mappings.size() + " " + mappings.hashCode());
        modMappings = mappings;
        update(!mappings.isEmpty());
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
        if(modMappings != null) {
            for (ResourceMapping mapping : modMappings) {
                for (Map.Entry<String,ImageResource> entry
                    : mapping.getImageResources().entrySet()) {
                    ImageResource resource = entry.getValue();
                    resource.clean();
                }
            }
        }
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
        for (ResourceMapping rm : modMappings) mc.addAll(rm);
        mergedContainer = mc;
    }

    /**
     * Create and start a new background preload thread.
     */
    private static void startBackgroundPreloading() {
        if ("true".equals(System.getProperty("java.awt.headless", "false"))) {
            return; // Do not preload in headless mode
        }

        preloadThread = new Thread(FreeCol.CLIENT_THREAD
            + "-Resource loader") {
                @Override
                public void run() {
                    // Make a local list of the resources to load.
                    logger.info("Background thread started");
                    ArrayList<Resource> resources
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

    public static synchronized boolean hasResource(final String resourceId) {
        logger.finest("hasResource(" + resourceId + ")");
        return mergedContainer.containsKey(resourceId);
    }

    public static synchronized boolean hasImageResource(final String resourceId) {
        //logger.finest("hasImageResource(" + resourceId + ")");
        return mergedContainer.containsImageKey(resourceId);
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
    public static synchronized ArrayList<String> getImageKeys(String prefix) {
        logger.finest("getImageKeys(" + prefix + ")");
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
    public static synchronized ArrayList<String> getImageKeys(String prefix,
                                                              String suffix) {
        //logger.finest("getImageKeys(" + prefix + ", " + suffix + ")");
        return mergedContainer.getImageKeys(prefix, suffix);
    }

    /**
     * Returns a set of all image keys containing the infix and
     * ending with the given suffix.
     *
     * @param infix the infix string contained somewhere
     * @param suffix the suffix
     * @return a set of all keys with these characteristics
     */
    public static synchronized Set<String> getImageKeySet(String infix,
                                                          String suffix) {
        //logger.finest("getImageKeySet(" + infix + ", " + suffix + ")");
        return mergedContainer.getImageKeySet(infix, suffix);
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
        final ImageResource r = mergedContainer.getImageResource(key);
        if (r == null) {
            logger.warning("getImageResource(" + key + ") failed");
        }
        return r;
    }

    /**
     * Returns the image specified by the given name.
     *
     * @param resource The name of the resource to return.
     * @return The image identified by <code>resource</code>
     *      or <code>null</code> if there is no image
     *      identified by that name.
     */
    public static BufferedImage getImage(final String resource) {
        final ImageResource r = getImageResource(resource);
        return (r != null) ? r.getImage() : null;
    }

    /**
     * Returns the image specified by the given name.
     * Please, avoid using too many different scaling factors!
     * For each is a scaled image cached here for a long time,
     * which wastes memory if you are not careful.
     *
     * @param resource The name of the resource to return.
     * @param scale The size of the requested image (with 1 being normal size,
     *      2 twice the size, 0.5 half the size etc). Rescaling
     *      will be performed unless using 1.
     * @return The image identified by <code>resource</code>
     *      or <code>null</code> if there is no image
     *      identified by that name.
     */
    public static BufferedImage getImage(final String resource, final float scale) {
        final ImageResource r = getImageResource(resource);
        return (r != null) ? r.getImage(scale) : null;
    }

    /**
     * Returns the image specified by the given name.
     * Please, avoid using this, as for each size another scaled version
     * of the image is cached!
     *
     * @param resource The name of the resource to return.
     * @param size The size of the requested image. Rescaling
     *      will be performed if necessary.
     * @return The image identified by <code>resource</code>
     *      or <code>null</code> if there is no image
     *      identified by that name.
     */
    public static BufferedImage getImage(final String resource, final Dimension size) {
        final ImageResource r = getImageResource(resource);
        return (r != null) ? r.getImage(size) : null;
    }

    /**
     * Returns the a grayscale version of the image specified by
     * the given name.
     *
     * @param resource The name of the resource to return.
     * @param size The size of the requested image. Rescaling
     *      will be performed if necessary.
     * @return The image identified by <code>resource</code>
     *      or <code>null</code> if there is no image
     *      identified by that name.
     */
    public static BufferedImage getGrayscaleImage(final String resource, final Dimension size) {
        final ImageResource r = getImageResource(resource);
        return (r != null) ? r.getGrayscaleImage(size) : null;
    }

    /**
     * Returns the grayscale version of the image specified by the given name.
     *
     * @param resource The name of the resource to return.
     * @param scale The size of the requested image (with 1 being normal size,
     *      2 twice the size, 0.5 half the size etc). Rescaling
     *      will be performed unless using 1.
     * @return The image identified by <code>resource</code>
     *      or <code>null</code> if there is no image
     *      identified by that name.
     */
    public static BufferedImage getGrayscaleImage(final String resource, final float scale) {
        final ImageResource r = getImageResource(resource);
        return (r != null) ? r.getGrayscaleImage(scale) : null;
    }

    /**
     * Returns the animation specified by the given name.
     *
     * @param resource The name of the resource to return.
     * @return The animation identified by <code>resource</code>
     *      or <code>null</code> if there is no animation
     *      identified by that name.
     */
    public static SimpleZippedAnimation getSimpleZippedAnimation(final String resource) {
        final SZAResource r = getSZAResource(resource);
        return (r != null) ? r.getSimpleZippedAnimation() : null;
    }

    /**
     * Returns the animation specified by the given name.
     *
     * @param resource The name of the resource to return.
     * @param scale The size of the requested animation (with 1
     *      being normal size, 2 twice the size, 0.5 half the
     *      size etc). Rescaling will be performed unless using 1.
     * @return The animation identified by <code>resource</code>
     *      or <code>null</code> if there is no animation
     *      identified by that name.
     */
    public static SimpleZippedAnimation getSimpleZippedAnimation(final String resource, final float scale) {
        final SZAResource r = getSZAResource(resource);
        return (r != null) ? r.getSimpleZippedAnimation(scale) : null;
    }

    /**
     * Returns the <code>Color</code> with the given name.
     *
     * @param resource The name of the resource to return.
     * @return An <code>Color</code> created with the image
     *      identified by <code>resource</code> or
     *      <code>null</code> if there is no color identified
     *      by that name.
     * @see #getImage(String)
     */
    public static Color getColor(final String resource) {
        final ColorResource r = getColorResource(resource);
        return (r != null) ? r.getColor() : null;
    }

    /**
     * Gets the font with the given name.
     *
     * @param resource The name of the resource to query.
     * @return The <code>Font</code> found in a FontResource, which
     *     may default to the Java default font if the resource failed
     *     to load.
     */
    public static Font getFont(final String resource) {
        final FontResource r = getFontResource(resource);
        if (r == null) return FontResource.getEmergencyFont();
        return r.getFont();
    }

    /**
     * Gets an audio resource with the given name.
     *
     * @param resource The name of the resource to query.
     * @return A <code>File</code> containing the audio data.
     */
    public static File getAudio(final String resource) {
        final AudioResource r = getAudioResource(resource);
        return (r == null) ? null : r.getAudio();
    }

    /**
     * Gets the <code>Video</code> represented by the given resource.
     * @param resource The name of the resource to return.
     * @return The <code>Video</code> in it's original size.
     */
    public static Video getVideo(final String resource) {
        final VideoResource r = getVideoResource(resource);
        return (r != null) ? r.getVideo() : null;
    }

    /**
     * Gets a FAFile resource with the given name.
     *
     * @param resource The name of the resource to query.
     * @return The <code>FAFile</code> found in a FAFileResource.
     */
    public static FAFile getFAFile(final String resource) {
        final FAFileResource r = getFAFileResource(resource);
        return (r == null) ? null : r.getFAFile();
    }

    /**
     * Gets a string resource with the given name.
     *
     * @param resource The name of the resource to query.
     * @return The string value.
     */
    public static String getString(final String resource) {
        final StringResource r = getStringResource(resource);
        return (r == null) ? null : r.getString();
    }

}
