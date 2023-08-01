/**
 *  Copyright (C) 2002-2022   The FreeCol Team
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

package net.sf.freecol.client.gui.mapviewer;

import java.awt.Dimension;
import java.awt.Point;

import javax.swing.JLabel;

/**
 * Bounds of the tiles to be rendered. These bounds are scaled according to the
 * zoom level of the map.
 */
public final class TileBounds {
    
    /** The height offset to paint a Unit at (in pixels). */
    public static final int UNIT_OFFSET = 20,
        OTHER_UNITS_OFFSET_X = -5, // Relative to the state indicator.
        OTHER_UNITS_OFFSET_Y = 1,
        OTHER_UNITS_WIDTH = 3,
        MAX_OTHER_UNITS = 10;
    

    /** Tile width and height, and half values thereof. */
    private int tileHeight, tileWidth, halfHeight, halfWidth;
    
    private float scaleFactor;
    
    TileBounds(Dimension tileSize, float scaleFactor) {
        this.tileHeight = tileSize.height;
        this.tileWidth = tileSize.width;
        this.halfHeight = tileSize.height / 2;
        this.halfWidth = tileSize.width / 2;
        this.scaleFactor = scaleFactor;
    }
    
    public int getWidth() {
        return tileWidth;
    }
    
    public int getHalfWidth() {
        return halfWidth;
    }
    
    public int getHeight() {
        return tileHeight;
    }
    
    public int getHalfHeight() {
        return halfHeight;
    }  
    
    /**
     * Gets the position where a unitLabel located at tile should be drawn.
     *
     * @param unitLabel The unit label.
     * @param tileP The position of the {@code Tile} on the screen.
     * @return The position where to put the label, null if tileP is null.
     */
    Point calculateUnitLabelPositionInTile(JLabel unitLabel,
                                                  Point tileP) {
        if (tileP == null) return null;
        int labelX = tileP.x + tileWidth / 2 - unitLabel.getWidth() / 2;
        int labelY = tileP.y + tileHeight / 2 - unitLabel.getHeight() / 2;
        return new Point(labelX, labelY);
    }
}
