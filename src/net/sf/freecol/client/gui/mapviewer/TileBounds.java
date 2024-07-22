/**
 *  Copyright (C) 2002-2024   The FreeCol Team
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
    
    TileBounds(Dimension tileSize, float scaleFactor) {
        this.tileHeight = tileSize.height;
        this.tileWidth = tileSize.width;
        this.halfHeight = tileSize.height / 2;
        this.halfWidth = tileSize.width / 2;
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
}
