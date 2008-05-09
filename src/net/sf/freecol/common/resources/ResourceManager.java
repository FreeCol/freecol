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

import java.awt.Dimension;
import java.awt.Image;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import javax.swing.ImageIcon;


/**
 * Class for getting resources (images, audio etc).
 */
public class ResourceManager {
    
    private static ResourceMapping baseMapping;
    private static ResourceMapping tcMapping;
    private static ResourceMapping campaignMapping;
    private static ResourceMapping scenarioMapping;
    private static List<ResourceMapping> modMappings = new LinkedList<ResourceMapping>();

    private static ResourceMapping mergedContainer;
    

    /**
     * Updates the resource mappings after making changes.
     */
    public static void update() {
        createMergedContainer();
    }
    
    /**
     * Creates a merged container for easy access to resources.
     */
    private static void createMergedContainer() {
        mergedContainer = new ResourceMapping();
        mergedContainer.addAll(baseMapping);
        mergedContainer.addAll(tcMapping);
        mergedContainer.addAll(campaignMapping);
        mergedContainer.addAll(scenarioMapping);
        ListIterator<ResourceMapping> it = modMappings.listIterator();
        while (it.hasPrevious()) {
            mergedContainer.addAll(it.previous());
        }
    }

    /**
     * Sets the mappings specified in the date/base-directory
     * @param _baseMapping The mapping between IDs and files. 
     */
    public static void setBaseMapping(ResourceMapping _baseMapping) {
        baseMapping = _baseMapping;
    }

    /**
     * Sets the mappings specified for a Total Conversion (TC).
     * @param _tcMapping The mapping between IDs and files. 
     */
    public static void setTcMapping(ResourceMapping _tcMapping) {
        tcMapping = _tcMapping;
    }

    /**
     * Sets the mappings specified in a campaign.
     * @param _campaignMapping The mapping between IDs and files. 
     */
    public static void setCampaignMapping(ResourceMapping _campaignMapping) {
        campaignMapping = _campaignMapping;
    }

    /**
     * Sets the mappings specified in a scenario.
     * @param _scenarioMapping The mapping between IDs and files. 
     */
    public static void setScenarioMapping(ResourceMapping _scenarioMapping) {
        scenarioMapping = _scenarioMapping;
    }


    /**
     * Returns the image specified by the given name.
     * 
     * @param resource The name of the resource to return.
     * @return The image identified by <code>resource</code>
     *      or <code>null</code> if there is no image
     *      identified by that name.
     */
    public static Image getImage(String resource) {
        final Resource r = mergedContainer.get(resource);
        if (!(r instanceof ImageResource)) {
            return null;
        }
        return ((ImageResource) r).getImage();
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
    public static Image getImage(String resource, double scale) {        
        final Resource r = mergedContainer.get(resource);
        if (!(r instanceof ImageResource)) {
            return null;
        }
        return ((ImageResource) r).getImage(scale);
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
    public static Image getImage(String resource, Dimension size) {
        final Resource r = mergedContainer.get(resource);
        if (!(r instanceof ImageResource)) {
            return null;
        }
        return ((ImageResource) r).getImage(size);
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
    public static Image getGrayscaleImage(String resource, Dimension size) {
        final Resource r = mergedContainer.get(resource);
        if (!(r instanceof ImageResource)) {
            return null;
        }
        return ((ImageResource) r).getGrayscaleImage(size);
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
    public static Image getGrayscaleImage(String resource, double scale) {        
        final Resource r = mergedContainer.get(resource);
        if (!(r instanceof ImageResource)) {
            return null;
        }
        return ((ImageResource) r).getGrayscaleImage(scale);
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
    public static ImageIcon getImageIcon(String resource) {
        Image im = getImage(resource);
        return (im != null) ? new ImageIcon(im) : null;
    }
}