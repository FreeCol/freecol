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
import java.awt.Image;
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

    /**
     * The following fields are mappings from resource IDs to
     * resources.  A mapping is defined within a specific context.
     * See the comment on each field's setter for more information:
     */
    // TODO: Combine these (array or just update one directly), merge the setters.
    // It may be a good idea to keep all on combining and choose through a
    // priority value that could be added on to each key/value pair.
    private static ResourceMapping baseMapping;
    private static ResourceMapping tcMapping;
    private static ResourceMapping scenarioMapping;
    // TODO: Check if mod resources are always added in a predetermined fixed order.
    private static List<ResourceMapping> modMappings
        = new ArrayList<>();

    /**
     * All the mappings above merged into this single ResourceMapping
     * according to precendence.
     */
    private static ResourceMapping mergedContainer;

    private static volatile Thread preloadThread = null;


    /**
     * Sets the mappings specified in the date/base-directory.
     *
     * @param mapping The mapping between IDs and files.
     */
    public static void setBaseMapping(final ResourceMapping mapping) {
        logger.info("setBaseMapping " + mapping);
        baseMapping = mapping;
        update(mapping != null);
    }

    /**
     * Sets the mappings specified for a Total Conversion (TC).
     *
     * @param mapping The mapping between IDs and files.
     */
    public static void setTcMapping(final ResourceMapping mapping) {
        logger.info("setTcMapping " + mapping);
        tcMapping = mapping;
        update(mapping != null);
    }

    /**
     * Sets the mappings specified by mods.
     *
     * @param mappings A list of the mappings between IDs and files.
     */
    public static void setModMappings(final List<ResourceMapping> mappings) {
        logger.info("setModMappings size " + mappings.size() + " " + mappings.hashCode());
        modMappings = mappings;
        update(!mappings.isEmpty());
    }

    /**
     * Sets the mappings specified in a scenario.
     *
     * @param mapping The mapping between IDs and files.
     */
    public static void setScenarioMapping(final ResourceMapping mapping) {
        logger.info("setScenarioMapping " + mapping);
        scenarioMapping = mapping;
        update(mapping != null);
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
    private static synchronized void createMergedContainer() {
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
                    // TODO: There are no obvious flaws currently, but this
                    // needs deeper verification, including checking all
                    // Resource classes another time, by someone knowledgeable
                    // in thread safety issues in Java. 
                    // Could lead to a race condition in case a Resource class
                    // is not completely thread safe, as references are shared.
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
        //logger.finest("hasResource(" + resourceId + ")");
        return mergedContainer.containsKey(resourceId);
    }

    private static synchronized Resource getResource(final String resourceId) {
        return mergedContainer.get(resourceId);
    }

    public static synchronized Map<String, Resource> getResources() {
        logger.finest("getResources");
        return mergedContainer.getResources();
    }

    /**
     * Returns a list of all keys starting with the given prefix.
     *
     * @param prefix the prefix
     * @return a list of all keys starting with the given prefix
     */
    public static synchronized ArrayList<String> getKeys(String prefix) {
        logger.finest("getKeys(" + prefix + ")");
        return mergedContainer.getKeys(prefix);
    }

    /**
     * Returns a list of all keys starting with the given prefix and
     * ending with the given suffix.
     *
     * @param prefix the prefix
     * @param suffix the suffix
     * @return a list of all resulting keys
     */
    public static synchronized ArrayList<String> getKeys(String prefix,
                                                         String suffix) {
        //logger.finest("getKeys(" + prefix + ", " + suffix + ")");
        return mergedContainer.getKeys(prefix, suffix);
    }

    /**
     * Returns a set of all keys containing the infix and
     * ending with the given suffix.
     *
     * @param infix the infix string contained somewhere
     * @param suffix the suffix
     * @return a set of all keys with these characteristics
     */
    public static synchronized Set<String> getFilteredKeys(String infix,
                                                           String suffix) {
        //logger.finest("getFilteredKeys(" + infix + ", " + suffix + ")");
        return mergedContainer.getFilteredKeys(infix, suffix);
    }

    /**
     * Gets the resource of the given type.
     *
     * @param <T> The type of the resource to get.
     * @param resourceId The resource to get.
     * @param type The type of the resource to get.
     * @return The resource if there is one with the given
     *     resourceId and type, or else <code>null</code>.
     */
    public static <T> T getResource(final String resourceId,
                                    final Class<T> type) {
        //logger.finest("getResource(" + resourceId + ", " + type.getName() + ")");
        final Resource r = getResource(resourceId);
        if (r == null) { // Log only unexpected failures
            if (!resourceId.startsWith("dynamic.")) {
                logger.warning("getResource(" + resourceId
                              + ", " + type.getName() + ") failed");
            }
            return null;
        }
        if (!type.isInstance(r)) { // Log type errors
            logger.warning("getResource(" + resourceId
                           + ", " + type.getName() + ") -> "
                           + r.getClass().getName());
            return null;
        }
        return type.cast(r);
    }

    /**
     * Returns the image specified by the given name.
     *
     * @param resource The name of the resource to return.
     * @return The image identified by <code>resource</code>
     *      or <code>null</code> if there is no image
     *      identified by that name.
     */
    public static Image getImage(final String resource) {
        final ImageResource r = getResource(resource, ImageResource.class);
        return (r != null) ? r.getImage() : null;
    }

    /**
     * Returns the image specified by the given name.
     *
     * @param resource The name of the resource to return.
     * @param scale The size of the requested image (with 1 being normal size,
     *      2 twice the size, 0.5 half the size etc). Rescaling
     *      will be performed unless using 1.
     * @return The image identified by <code>resource</code>
     *      or <code>null</code> if there is no image
     *      identified by that name.
     */
    public static Image getImage(final String resource, final float scale) {
        final ImageResource r = getResource(resource, ImageResource.class);
        return (r != null) ? r.getImage(scale) : null;
    }

    /**
     * Returns the image specified by the given name.
     *
     * @param resource The name of the resource to return.
     * @param size The size of the requested image. Rescaling
     *      will be performed if necessary.
     * @return The image identified by <code>resource</code>
     *      or <code>null</code> if there is no image
     *      identified by that name.
     */
    public static Image getImage(final String resource, final Dimension size) {
        final ImageResource r = getResource(resource, ImageResource.class);
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
    public static Image getGrayscaleImage(final String resource, final Dimension size) {
        final ImageResource r = getResource(resource, ImageResource.class);
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
    public static Image getGrayscaleImage(final String resource, final float scale) {
        final ImageResource r = getResource(resource, ImageResource.class);
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
        final SZAResource r = getResource(resource, SZAResource.class);
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
        final SZAResource r = getResource(resource, SZAResource.class);
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
        final ColorResource r = getResource(resource, ColorResource.class);
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
        final FontResource r = getResource(resource, FontResource.class);
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
        final AudioResource r = getResource(resource, AudioResource.class);
        return (r == null) ? null : r.getAudio();
    }

    /**
     * Gets the <code>Video</code> represented by the given resource.
     * @param resource The name of the resource to return.
     * @return The <code>Video</code> in it's original size.
     */
    public static Video getVideo(final String resource) {
        final VideoResource r = getResource(resource, VideoResource.class);
        return (r != null) ? r.getVideo() : null;
    }

    /**
     * Gets a FAFile resource with the given name.
     *
     * @param resource The name of the resource to query.
     * @return The <code>FAFile</code> found in a FAFileResource.
     */
    public static FAFile getFAFile(final String resource) {
        final FAFileResource r = getResource(resource, FAFileResource.class);
        return (r == null) ? null : r.getFAFile();
    }

    /**
     * Gets a string resource with the given name.
     *
     * @param resource The name of the resource to query.
     * @return The string value.
     */
    public static String getString(final String resource) {
        final StringResource r = getResource(resource, StringResource.class);
        return (r == null) ? null : r.getString();
    }
}
