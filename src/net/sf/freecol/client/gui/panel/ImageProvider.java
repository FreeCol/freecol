/**
 *  Copyright (C) 2002-2007  The FreeCol Team
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

package net.sf.freecol.client.gui.panel;

import java.awt.Color;
import java.awt.Image;

import javax.swing.ImageIcon;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.TileType;

import net.sf.freecol.common.model.Unit;

/**
 * An object that should be able to provide images upon request.
 */
public abstract class ImageProvider {




    /**
     * Should return the Image of the terrain with the given type.
     * 
     * @param type The type of the terrain of which we need the Image.
     * @param x The x-coordinate of the location of the tile that is being
     *            drawn.
     * @param y The x-coordinate of the location of the tile that is being
     *            drawn.
     * @return The Image of the terrain with the given type.
     */
    public abstract Image getTerrainImage(TileType type, int x, int y);

    /**
     * Should return the Image of the graphic with the given type.
     * 
     * @param type The type of the graphic of which we need the Image.
     * @return The Image of the graphic with the given type.
     */
    public abstract Image getGoodsImage(GoodsType type);

    /**
     * Should return the Image of the graphic with the given type.
     * 
     * @param type The type of the graphic of which we need the Image.
     * @return The Image of the graphic with the given type.
     */
    public abstract ImageIcon getGoodsImageIcon(GoodsType type);

    /**
     * Should return the Image of the graphic with the given type.
     * 
     * @param type The type of the graphic of which we need the Image.
     * @return The Image of the graphic with the given type.
     */
    public abstract Image getMiscImage(int type);

    /**
     * Should return the Image of the color chip with the given color.
     * 
     * @param color The color of the color chip of which we need the Image.
     * @return The Image of the color chip with the given color.
     */
    public abstract Image getColorChip(Color color);

    /**
     * Returns the unit-image at the given index.
     * 
     * @param index The index of the unit-image to return.
     * @return The unit-image at the given index.
     */
    public abstract Image getUnitImage(int index);

    /**
     * Returns the unit-image at the given index.
     * 
     * @param index The index of the unit-image to return.
     * @param grayscale If <code>true</code> return the image in grayscale
     * @return The unit-image at the given index.
     */
    public abstract Image getUnitImage(int index, boolean grayscale);

    /**
     * Returns the unit-ImageIcon at the given index.
     * 
     * @param type The type of the Unit of which we need the ImageIcon.
     * @return The unit-ImageIcon of the Unit with the given type.
     */
    public abstract ImageIcon getUnitImageIcon(int type);

    /**
     * Should return the ImageIcon of the Unit with the given type.
     * 
     * @param type The type of the Unit of which we need the ImageIcon.
     * @param grayscale If <code>true</code> return the image in grayscale
     * @return The ImageIcon of the Unit with the given type.
     */
    public abstract ImageIcon getUnitImageIcon(int type, boolean grayscale);

    /**
     * Should return the ImageIcon of the Unit Button with the given index.
     * 
     * @param index The index of the image to return.
     * @param state The state (normal, highlighted, pressed, disabled)
     * @return The image pointer
     */
    public abstract ImageIcon getUnitButtonImageIcon(int index, int state);

    /**
     * Should return the width of the terrain-image at the given index.
     * 
     * @param type The type of the terrain-image.
     * @return The width of the terrain-image at the given index.
     */
    public abstract int getTerrainImageWidth(TileType type);

    /**
     * Should return the height of the terrain-image at the given index.
     * 
     * @param type The type of the terrain-image.
     * @return The height of the terrain-image at the given index.
     */
    public abstract int getTerrainImageHeight(TileType type);

    /**
     * Should return the width of the unit-image at the given index.
     * 
     * @param index The index of the unit-image.
     * @return The width of the unit-image at the given index.
     */
    public abstract int getUnitImageWidth(int index);

    /**
     * Should return the height of the unit-image at the given index.
     * 
     * @param index The index of the unit-image.
     * @return The height of the unit-image at the given index.
     */
    public abstract int getUnitImageHeight(int index);

    /**
     * Should return the graphic type that can be used to represent the given
     * Unit visually.
     * 
     * @param unit The unit for whom we need a graphic type.
     * @return The graphic type that can be used to represent the given Unit
     *         visually.
     */
    public abstract int getUnitGraphicsType(Unit unit);
}
