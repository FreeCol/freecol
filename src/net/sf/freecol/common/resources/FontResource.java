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

import java.awt.Font;
import java.net.URI;
import java.net.URL;

/**
 * A <code>Resource</code> wrapping a <code>Font</code>.
 *
 * @see Resource
 * @see Font
 */
public class FontResource extends Resource {

    public static final String SCHEME = "font:";

    private Font font;


    public FontResource(Font font) {
        this.font = font;
    }

    /**
     * Do not use directly.
     * @param resourceLocator The <code>URI</code> used when loading this
     *      resource.
     * @see ResourceFactory#createResource(URI)
     */
    FontResource(URI resourceLocator) throws Exception {
        super(resourceLocator);
        font = null;
        if (resourceLocator.getPath() != null
            && resourceLocator.getPath().endsWith(".ttf")) {
            URL url = resourceLocator.toURL();
            font = Font.createFont(Font.TRUETYPE_FONT, url.openStream());
        } else {
            String name = resourceLocator.getSchemeSpecificPart();
            font = Font.decode(name.substring(SCHEME.length()));
        }
    }

    /**
     * Gets the <code>Font</code> represented by this resource.  As
     * failure to load a critical font might remove the ability to
     * even display an error message, it is too risky to allow this
     * routine to return null.  Thus if the desired font was not
     * found, return the default Java font, not matter how ugly.
     *
     * @return The <code>Font</code> for this resource, or the default
     *     Java font if none found.
     */
    public Font getFont() {
        return (font == null) ? Font.decode(null) : font;
    }
}
