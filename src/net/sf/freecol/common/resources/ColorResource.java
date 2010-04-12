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
import java.lang.reflect.Field;
import java.net.URI;
import java.util.logging.Logger;


/**
 * A <code>Resource</code> wrapping a <code>Color</code>.
 * 
 * @see Resource
 * @see Color
 */
public class ColorResource extends Resource {

    private static final Logger logger = Logger.getLogger(ColorResource.class.getName());

    public static final String SCHEME = "color:";

    private Color color;
    
    /**
     * Do not use directly.
     * @param resourceLocator The <code>URI</code> used when loading this
     *      resource.
     * @see ResourceFactory#createResource(URI)
     */
    ColorResource(URI resourceLocator) throws Exception {
        super(resourceLocator);
        if (resourceLocator.getFragment() == null) {
            String colorName = resourceLocator.getSchemeSpecificPart().substring(SCHEME.length());
            try {
                Field field = Color.class.getField(colorName);
                color = (Color) field.get(null);
            } catch(Exception e) {
                // probably a non-standard color name
                logger.warning(e.toString());
            }
        } else {
            color = new Color(Integer.decode("0x" + resourceLocator.getFragment()));
        }
    }
    
    
    /**
     * Gets the <code>Color</code> represented by this resource.
     * @return The <code>Color</code> in it's original size.
     */
    public Color getColor() {
        return color;
    }
}
