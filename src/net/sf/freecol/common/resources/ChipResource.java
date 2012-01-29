/**
 *  Copyright (C) 2002-2012   The FreeCol Team
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
import java.awt.Image;
import java.awt.Graphics2D;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;


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

    public static final int WIDTH = 10;
    public static final int HEIGHT = 17;

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


    private Map<Double, Image> scaledImages = new HashMap<Double, Image>();

    private ChipResource(String type, Color bg, Color fg) {
        this.type = type;
        this.background = bg;
        this.foreground = fg;
    }

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
     * Preloading is a noop for this resource type.
     */
    public void preload() {}

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
    public final Image getImage() {
        if (image == null) {
            image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
            if ("color".equals(type)) {
                createColorChip();
            } else if ("mission".equals(type)) {
                createMissionChip();
            } else if ("alarm.visited".equals(type)) {
                createAlarmChip(true);
            } else if ("alarm.unvisited".equals(type)) {
                createAlarmChip(false);
            }
        }
        return image;
    }

    public final Image getImage(double scale) {
        Image scaledImage = scaledImages.get(scale);
        if (scaledImage == null) {
            Image image = getImage();
            int width = (int) (WIDTH * scale);
            int height = (int) (HEIGHT * scale);
            scaledImage = image.getScaledInstance(width, height, Image.SCALE_SMOOTH);
            scaledImages.put(scale, scaledImage);
        }
        return scaledImage;
    }

    private void createColorChip() {
        Graphics2D g = (Graphics2D) image.getGraphics();
        int bw = WIDTH / 10;
        g.setColor(foreground);
        g.fillRect(0, 0, WIDTH, HEIGHT);
        g.setColor(background);
        g.fillRect(bw, bw, WIDTH - 2*bw , HEIGHT - 2*bw);
    }

    private void createMissionChip() {
        Graphics2D g = (Graphics2D) image.getGraphics();
        int bw = WIDTH / 10;
        int dw = WIDTH / 5;
        g.setColor(background);
        g.fillRect(0, 0, WIDTH, HEIGHT);
        GeneralPath cross = new GeneralPath(GeneralPath.WIND_EVEN_ODD);
        cross.moveTo(2*dw, bw);
        cross.lineTo(3*dw, bw);
        cross.lineTo(3*dw, 2*dw);
        cross.lineTo(WIDTH - bw, 2*dw);
        cross.lineTo(WIDTH - bw, 3*dw);
        cross.lineTo(3*dw, 3*dw);
        cross.lineTo(3*dw, HEIGHT - bw);
        cross.lineTo(2*dw, HEIGHT - bw);
        cross.lineTo(2*dw, 3*dw);
        cross.lineTo(bw, 3*dw);
        cross.lineTo(bw, 2*dw);
        cross.lineTo(2*dw, 2*dw);
        cross.closePath();
 
        g.setColor(foreground);
        g.fill(cross);
    }

    private void createAlarmChip(boolean visited) {
        Graphics2D g = (Graphics2D) image.getGraphics();
        int bw = WIDTH / 10;
        int dw = WIDTH / 5;
        createColorChip();
        g.setColor(foreground);
        if (visited) {
            g.fillRect(2*dw, bw+dw, dw, 3*dw+bw);
        } else {
            g.fillRect(bw+dw, bw+dw, 2*dw, dw);
            g.fillRect(3*dw, 2*dw, dw, dw);
            g.fillRect(2*dw, 3*dw, bw+dw, bw);
            g.fillRect(2*dw, 3*dw+bw, dw, bw+dw);
        }
        g.fillRect(2*dw, HEIGHT - 3*dw, dw, dw);
    }

    // Factory methods

    public static ChipResource colorChip(Color color) {
        return new ChipResource("color", color, Color.BLACK);
    }

    public static ChipResource missionChip(Color color, boolean expert) {
        return new ChipResource("mission",
                                (expert ? ResourceManager.getColor("expertMission.background.color") :
                                 ResourceManager.getColor("mission.background.color")),
                                color);
    }

}
