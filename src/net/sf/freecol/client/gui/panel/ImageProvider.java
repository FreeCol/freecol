
package net.sf.freecol.client.gui.panel;

import java.awt.Color;
import java.awt.Image;

import javax.swing.ImageIcon;

import net.sf.freecol.common.model.Unit;

/**
* An object that should be able to provide images upon request.
*/
public abstract class ImageProvider {
    public static final String  COPYRIGHT = "Copyright (C) 2003-2004 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";
    
    /**
    * Should return the Image of the Unit with the given type.
    * @param index The type of the Unit of which we need the Image.
    * @return The Image of the Unit with the given type.
    */
    public abstract Image getUnitImage(int type);
    
    /**
    * Should return the Image of the terrain with the given type.
    * @param index The type of the terrain of which we need the Image.
    * @param x The x-coordinate of the location of the tile that is being drawn.
    * @param y The x-coordinate of the location of the tile that is being drawn.
    * @return The Image of the terrain with the given type.
    */
    public abstract Image getTerrainImage(int type, int x, int y);
    
    /**
    * Should return the Image of the graphic with the given type.
    * @param index The type of the graphic of which we need the Image.
    * @return The Image of the graphic with the given type.
    */
    public abstract Image getGoodsImage(int type);

    /**
    * Should return the Image of the graphic with the given type.
    * @param index The type of the graphic of which we need the Image.
    * @return The Image of the graphic with the given type.
    */
    public abstract ImageIcon getGoodsImageIcon(int type);

    /**
    * Should return the Image of the graphic with the given type.
    * @param index The type of the graphic of which we need the Image.
    * @return The Image of the graphic with the given type.
    */
    public abstract Image getMiscImage(int type);
    
    /**
    * Should return the Image of the color chip with the given color.
    * @param index The color of the color chip of which we need the Image.
    * @return The Image of the color chip with the given color.
    */
    public abstract Image getColorChip(Color color);
    
    /**
    * Should return the ImageIcon of the Unit with the given type.
    * @param index The type of the Unit of which we need the ImageIcon.
    * @return The ImageIcon of the Unit with the given type.
    */
    public abstract ImageIcon getUnitImageIcon(int type);
    
    /**
    * Should return the ImageIcon of the Unit Button with the given index.
    * @param index The index of the image to return.
    * @param state The state (normal, highlighted, pressed, disabled)
    * @return The image pointer
    */
    public abstract ImageIcon getUnitButtonImageIcon(int index, int state);
    
    /**
    * Should return the width of the terrain-image at the given index.
    * @param index The index of the terrain-image.
    * @return The width of the terrain-image at the given index.
    */
    public abstract int getTerrainImageWidth(int index);
    
    /**
    * Should return the height of the terrain-image at the given index.
    * @param index The index of the terrain-image.
    * @return The height of the terrain-image at the given index.
    */
    public abstract int getTerrainImageHeight(int index);
    
    /**
    * Should return the width of the unit-image at the given index.
    * @param index The index of the unit-image.
    * @return The width of the unit-image at the given index.
    */
    public abstract int getUnitImageWidth(int index);
    
    /**
    * Should return the height of the unit-image at the given index.
    * @param index The index of the unit-image.
    * @return The height of the unit-image at the given index.
    */
    public abstract int getUnitImageHeight(int index);
    
    /**
    * Should return the graphic type that can be used to represent the given Unit visually.
    * @param unit The unit for whom we need a graphic type.
    * @return The graphic type that can be used to represent the given Unit visually.
    */
    public abstract int getUnitGraphicsType(Unit unit);
}
