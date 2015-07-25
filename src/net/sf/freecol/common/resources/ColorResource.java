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
import java.lang.reflect.Field;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * A <code>Resource</code> wrapping a <code>Color</code>.
 * 
 * @see Resource
 * @see Color
 */
public class ColorResource extends Resource {

    private static final Logger logger = Logger.getLogger(ColorResource.class.getName());

    public static final Color REPLACEMENT_COLOR = Color.MAGENTA;

    public static final String SCHEME = "color:";

    private final Color color;


    public ColorResource(Color color) {
        this.color = color;
    }

    /**
     * Do not use directly.
     *
     * @param resourceLocator The <code>URI</code> used when loading this
     *     resource.
     */
    public ColorResource(URI resourceLocator) throws Exception {
        super(resourceLocator);

        String colorName = resourceLocator.getSchemeSpecificPart()
            .substring(SCHEME.length());
        this.color = getColor(colorName);
    }


    /**
     * Gets the <code>Color</code> represented by this resource.
     *
     * @return The <code>Color</code> in it's original size.
     */
    public Color getColor() {
        return this.color;
    }

    private static boolean isHexString(String str) {
        if (str == null
            || !(str.startsWith("0x") || str.startsWith("0X"))
            || str.length() <= 2) return false;
        for (int i = 2; i < str.length(); i++) {
            if (!"0123456789ABCDEFabcdef".contains(str.substring(i, i + 1))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the <code>Color</code> identified by the given
     * string. This is either a hexadecimal integer prefixed with
     * "0x", or the name of a field of the Color class.
     *
     * @param colorName a <code>String</code> value
     * @return a <code>Color</code> value
     */
    public static Color getColor(String colorName) {
        if (isHexString(colorName)) {
            try {
                int col = Integer.decode(colorName);
                return new Color(col, colorName.length() > 8);
            } catch (NumberFormatException e) {
                logger.warning("Failed to decode hex colour string: "
                    + colorName);
            }
        } else {
            try {
                Field field = Color.class.getField(colorName);
                return (Color) field.get(null);
            } catch (IllegalAccessException | IllegalArgumentException
                    | NoSuchFieldException | SecurityException e) {
                // probably a non-standard color name
                logger.log(Level.WARNING, "Failed to decode colour", e);
            }
        }
        // Fall back, as there are places where a null colour
        // can cause crashes.
        return REPLACEMENT_COLOR;
    }
}
