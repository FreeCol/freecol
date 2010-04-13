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
import java.awt.Graphics2D;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.net.URI;


/**
 * A <code>Resource</code> wrapping a "Chip", that is a small
 * <code>BufferedImage</code> indicating the owner of a unit or
 * mission, or the alarm of a native settlement. The <code>URI</code>
 * is expected to look like this "urn:chip:mission:red:black", for
 * example.
 * 
 * @see Resource
 * @see Color
 */
public class ChipResource extends Resource {

    public static final String SCHEME = "chip:";

    /**
     * Describe foreground here.
     */
    private Color foreground = Color.BLACK;

    /**
     * Describe background here.
     */
    private Color background = Color.WHITE;

    /**
     * Describe chip here.
     */
    private BufferedImage image = null;

    /**
     * Describe type here.
     */
    private final String type;

    /**
     * Do not use directly.
     * @param resourceLocator The <code>URI</code> used when loading this
     *      resource.
     * @see ResourceFactory#createResource(URI)
     */
    ChipResource(URI resourceLocator) {
        super(resourceLocator);
        String[] parts = resourceLocator.getSchemeSpecificPart().split(":");
        type = parts[1];
        background = ColorResource.getColor(parts[2]);
        foreground = ColorResource.getColor(parts[3]);
    }
    
    /**
     * Get the <code>Foreground</code> value.
     *
     * @return a <code>Color</code> value
     */
    public final Color getForeground() {
        return foreground;
    }

    /**
     * Set the <code>Foreground</code> value.
     *
     * @param newForeground The new Foreground value.
     */
    public final void setForeground(final Color newForeground) {
        this.foreground = newForeground;
        image = null;
    }

    /**
     * Get the <code>Background</code> value.
     *
     * @return a <code>Color</code> value
     */
    public final Color getBackground() {
        return background;
    }

    /**
     * Set the <code>Background</code> value.
     *
     * @param newBackground The new Background value.
     */
    public final void setBackground(final Color newBackground) {
        this.background = newBackground;
        image = null;
    }

    /**
     * Get the <code>Type</code> value.
     *
     * @return a <code>String</code> value
     */
    public final String getType() {
        return type;
    }

    /**
     * Get the <code>Image</code> value.
     *
     * @return a <code>BufferedImage</code> value
     */
    public final BufferedImage getImage() {
        if (image == null) {
            image = new BufferedImage(10, 17, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = (Graphics2D) image.getGraphics();
            createColorChip(g, background, foreground);

            if ("mission".equals(type)) {
                createMissionChip(g, foreground);
            } else if ("alarm.visited".equals(type)) {
                createVisitedAlarmChip(g, foreground);
            } else if ("alarm.unvisited".equals(type)) {
                createUnvisitedAlarmChip(g, foreground);
            }
        }
        return image;
    }

    private void createColorChip(Graphics2D g, Color background, Color foreground) {
        g.setColor(foreground);
        g.fillRect(0, 0, 10, 17);
        g.setColor(background);
        g.fillRect(1, 1, 8 , 15);
    }

    private void createMissionChip(Graphics2D g, Color foreground) {
        GeneralPath cross = new GeneralPath(GeneralPath.WIND_EVEN_ODD);
        cross.moveTo(4, 1);
        cross.lineTo(6, 1);
        cross.lineTo(6, 4);
        cross.lineTo(9, 4);
        cross.lineTo(9, 6);
        cross.lineTo(6, 6);
        cross.lineTo(6, 16);
        cross.lineTo(4, 16);
        cross.lineTo(4, 6);
        cross.lineTo(1, 6);
        cross.lineTo(1, 4);
        cross.lineTo(4, 4);
        cross.closePath();
 
        g.setColor(foreground);
        g.fill(cross);
    }

    private void createVisitedAlarmChip(Graphics2D g, Color foreground) {
        g.setColor(foreground);
        g.fillRect(4, 3, 2, 7);
        g.fillRect(4, 12, 2, 2);
    }

    private void createUnvisitedAlarmChip(Graphics2D g, Color foreground) {
        g.setColor(foreground);
        g.fillRect(3, 3, 4, 2);
        g.fillRect(6, 4, 2, 2);
        g.fillRect(4, 6, 3, 1);
        g.fillRect(4, 7, 2, 3);
        g.fillRect(4, 12, 2, 2);
    }

}
