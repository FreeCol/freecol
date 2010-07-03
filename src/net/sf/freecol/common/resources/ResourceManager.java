/**
 *  Copyright (C) 2002-2008  The FreeCol Team
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
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.logging.Logger;

import javax.swing.ImageIcon;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.gui.FAFile;
import net.sf.freecol.client.gui.video.Video;
import net.sf.freecol.common.io.sza.SimpleZippedAnimation;


/**
 * Class for getting resources (images, audio etc).
 */
public class ResourceManager {
    
    private static final Logger logger = Logger.getLogger(ResourceManager.class.getName());

    // the number of different river styles
    public static final int RIVER_STYLES = 81;

    /*
     * The following fields are mappings from resource IDs
     * to resources. A mapping is defined within a specific
     * context. See the comment on each field's setter for
     * more information: 
     */
    private static ResourceMapping baseMapping;
    private static ResourceMapping tcMapping;
    private static ResourceMapping campaignMapping;
    private static ResourceMapping scenarioMapping;
    private static ResourceMapping gameMapping;
    private static List<ResourceMapping> modMappings = new LinkedList<ResourceMapping>();

    /*
     * All the mappings above merged into this single
     * ResourceMapping according to precendence.
     */
    private static ResourceMapping mergedContainer;
    
    private static volatile Thread preloadThread = null;
    
    private static volatile boolean dirty = false;
    
    private static Dimension lastWindowSize;
    
    
    /**
     * Sets the mappings specified in the date/base-directory
     * @param _baseMapping The mapping between IDs and files. 
     */
    public static void setBaseMapping(final ResourceMapping _baseMapping) {
        baseMapping = _baseMapping;
        dirty = true;
    }

    /**
     * Sets the mappings specified for a Total Conversion (TC).
     * @param _tcMapping The mapping between IDs and files. 
     */
    public static void setTcMapping(final ResourceMapping _tcMapping) {
        tcMapping = _tcMapping;
        dirty = true;
    }

    /**
     * Sets the mappings specified by mods.
     * @param _modMappings A list of the mapping between IDs and files. 
     */
    public static void setModMappings(final List<ResourceMapping> _modMappings) {
        modMappings = _modMappings;
        dirty = true;
    }
    
    /**
     * Sets the mappings specified in a campaign.
     * @param _campaignMapping The mapping between IDs and files. 
     */
    public static void setCampaignMapping(final ResourceMapping _campaignMapping) {
        campaignMapping = _campaignMapping;
        dirty = true;
    }

    /**
     * Sets the mappings specified in a scenario.
     * @param _scenarioMapping The mapping between IDs and files. 
     */
    public static void setScenarioMapping(final ResourceMapping _scenarioMapping) {
        scenarioMapping = _scenarioMapping;
        dirty = true;
    }

    /**
     * Sets the mappings specified in a game, such as the player colors.
     *
     * @param _gameMapping The mapping between IDs and resources. 
     */
    public static void setGameMapping(final ResourceMapping _gameMapping) {
        gameMapping = _gameMapping;
        dirty = true;
    }

    /**
     * Add more mappings to the game mapping.
     *
     * @param mapping The <code>ResourceMapping</code> to add.
     */
    public static void addGameMapping(final ResourceMapping mapping) {
        if (gameMapping == null) gameMapping = new ResourceMapping();
        gameMapping.addAll(mapping);
        dirty = true;
    }

    /**
     * Add a single mappings to the game mapping.
     *
     * @param key The key.
     * @param resource The resource to add.
     */
    public static void addGameMapping(String key, Resource resource) {
        if (gameMapping == null) gameMapping = new ResourceMapping();
        gameMapping.add(key, resource);
        mergedContainer.add(key, resource);
    }

    /**
     * Preload resources. This method is intended to
     * be called when starting the application, as
     * it blocks until resources needed for the first
     * panels have been loaded.
     * 
     * It also ensures that the {@link #startBackgroundPreloading(Dimension)
     * background preloading thread} is started.
     *  
     * @param windowSize
     */
    public static void preload(final Dimension windowSize) {
        if (lastWindowSize != windowSize) {
            dirty = true;
        }
        lastWindowSize = windowSize;
        updateIfDirty(); // starts: startBackgroundPreloading
    }
    
    /**
     * Starts background preloading of resources.
     * @param windowSize The window size to use when scaling
     *      full screen size images.
     */
    public static void startBackgroundPreloading(final Dimension windowSize) {
        lastWindowSize = windowSize;
        if (dirty) {
            updateIfDirty();
            return; // startBackgroundPreloading will be called from update
        }
        preloadThread = new Thread(FreeCol.CLIENT_THREAD+"Resource loader") {
            public void run() {
                for (Resource r : mergedContainer.getResources().values()) {
                    if (preloadThread != this) {
                        return;
                    }
                    r.preload();
                }
            }
        };
        preloadThread.setPriority(2);
        preloadThread.start();
    }
    
    /**
     * Updates the resource mappings after making changes.
     */
    private static void updateIfDirty() {
        if (dirty) {
            dirty = false;
            preloadThread = null;
            createMergedContainer();
            startBackgroundPreloading(lastWindowSize);
        }
    }

    /**
     * Creates a merged container for easy access to resources.
     */
    private static void createMergedContainer() {
        ResourceMapping _mergedContainer = new ResourceMapping();
        _mergedContainer.addAll(baseMapping);
        _mergedContainer.addAll(tcMapping);
        _mergedContainer.addAll(campaignMapping);
        _mergedContainer.addAll(scenarioMapping);
        ListIterator<ResourceMapping> it = modMappings.listIterator(modMappings.size()); 
        while (it.hasPrevious()) {
            _mergedContainer.addAll(it.previous());
        }
        _mergedContainer.addAll(gameMapping);
        mergedContainer = _mergedContainer;
    }
    
    /**
     * Returns the resource of the given type.
     * @param <T> The type of the resource to get.
     * @param resourceId The resource to get.
     * @param type The type of the resource to get.
     * @return The resource if there is one with the given
     *      resourceId and type, or else <code>null</code>.
     */
    public static <T> T getResource(final String resourceId, final Class<T> type) {
        final Resource r = mergedContainer.get(resourceId);
        if (type.isInstance(r)) {
            return type.cast(r);
        }
        
        if (r == null) { // Log only unexpected failures
            if (!resourceId.startsWith("dynamic.")) {
                logger.finest("getResource(" + resourceId
                              + ", " + type.getName() + ") failed");
            }
        } else { // Log type errors
            logger.finest("getResource(" + resourceId
                          + ", " + type.getName() + ") -> "
                          + r.getClass().getName());
        }
        return null;
    }

    public static boolean hasResource(final String resourceId) {
        return mergedContainer.containsKey(resourceId);
    }

    public static Map<String, Resource> getResources() {
        return mergedContainer.getResources();
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
        updateIfDirty();
        final SZAResource r = getResource(resource, SZAResource.class);
        return (r != null) ? r.getSimpleZippedAnimation() : null;
    }
    
    /**
     * Gets the <code>Video</code> represented by the given resource.
     * @return The <code>Video</code> in it's original size.
     */
    public static Video getVideo(final String resource) {
        updateIfDirty();
        final VideoResource r = getResource(resource, VideoResource.class);
        return (r != null) ? r.getVideo() : null;
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
    public static SimpleZippedAnimation getSimpleZippedAnimation(final String resource, final double scale) {
        updateIfDirty();
        final SZAResource r = getResource(resource, SZAResource.class);
        return (r != null) ? r.getSimpleZippedAnimation(scale) : null;
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
        updateIfDirty();
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
    public static Image getImage(final String resource, final double scale) {
        updateIfDirty();
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
        updateIfDirty();
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
        updateIfDirty();
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
    public static Image getGrayscaleImage(final String resource, final double scale) {
        updateIfDirty();
        final ImageResource r = getResource(resource, ImageResource.class);
        return (r != null) ? r.getGrayscaleImage(scale) : null;
    }

    /**
     * Creates an <code>ImageIcon</code> for the image of
     * the given name.
     * 
     * @param resource The name of the resource to return.
     * @return An <code>ImageIcon</code> created with the image
     *      identified by <code>resource</code> or
     *      <code>null</code> if there is no image identified
     *      by that name.
     * @see #getImage(String)
     */
    public static ImageIcon getImageIcon(final String resource) {
        updateIfDirty();
        final Image im = getImage(resource);
        return (im != null) ? new ImageIcon(im) : null;
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
        updateIfDirty();
        final ColorResource r = getResource(resource, ColorResource.class);
        return (r != null) ? r.getColor() : null;
    }

    /**
     * Returns the <code>Image</code> with the given name.
     * 
     * @param resource The name of the resource to return.
     * @return An <code>Image</code> created with the image
     *      identified by <code>resource</code> or
     *      <code>null</code> if there is no image identified
     *      by that name.
     * @see #getImage(String)
     */
    public static Image getChip(final String resource) {
        updateIfDirty();
        final ChipResource r = getResource(resource, ChipResource.class);
        return (r != null) ? r.getImage() : null;
    }


    public static Image getChip(final String resource, double scale) {
        updateIfDirty();
        final ChipResource r = getResource(resource, ChipResource.class);
        return (r != null) ? r.getImage(scale) : null;
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
        updateIfDirty();
        final FontResource r = getResource(resource, FontResource.class);
        if (r == null) return FontResource.getEmergencyFont();
        return r.getFont();
    }

    /**
     * Gets the font with the given name and applies a style change.
     *
     * @param resource The name of the resource to query.
     * @return The <code>Font</code> found in a FontResource, which
     *     may default to the Java default font if the resource failed
     *     to load.
     */
    public static Font getFont(final String resource, int style) {
        Font font = ResourceManager.getFont(resource);
        return font.deriveFont(style);
    }

    /**
     * Gets the font with the given name and applies a size change.
     *
     * @param resource The name of the resource to query.
     * @return The <code>Font</code> found in a FontResource, which
     *     may default to the Java default font if the resource failed
     *     to load.
     */
    public static Font getFont(final String resource, float size) {
        Font font = ResourceManager.getFont(resource);
        return font.deriveFont(size);
    }

    /**
     * Gets the font with the given name and applies style and size changes.
     *
     * @param resource The name of the resource to query.
     * @return The <code>Font</code> found in a FontResource, which
     *     may default to the Java default font if the resource failed
     *     to load.
     */
    public static Font getFont(final String resource, int style, float size) {
        Font font = ResourceManager.getFont(resource);
        return font.deriveFont(style, size);
    }

    /**
     * Gets a FAFile resource with the given name.
     *
     * @param resource The name of the resource to query.
     * @return The <code>FAFile</code> found in a FAFileResource.
     */
    public static FAFile getFAFile(final String resource) {
        final FAFileResource r = getResource(resource, FAFileResource.class);
        return r.getFAFile();
    }
}
