/**
 *  Copyright (C) 2002-2011  The FreeCol Team
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
import java.awt.GraphicsEnvironment;
import java.net.URI;
import java.net.URL;
import java.util.logging.Logger;


/**
 * A <code>Resource</code> wrapping a <code>Font</code>.
 *
 * @see Resource
 * @see Font
 */
public class FontResource extends Resource {

    private static final Logger logger = Logger.getLogger(FontResource.class.getName());

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

        /** registerFont was only introduced in Java 1.6
        if (font != null) {
            GraphicsEnvironment.getLocalGraphicsEnvironment()
                .registerFont(font);
        }
        */

        logger.info("Loaded font: "
                    + ((font==null) ? "(null)" : font.getFontName())
                    + " from: " + resourceLocator);
    }

    /**
     * Preloading is a noop for this resource type.
     */
    public void preload() {}

    /**
     * Gets the <code>Font</code> represented by this resource.  As
     * failure to load a critical font might remove the ability to
     * even display an error message, it is too risky to allow this
     * routine to return null.  Hence the emergency font use.
     *
     * @return The <code>Font</code> for this resource, or the default
     *     Java font if none found.
     */
    public Font getFont() {
        if (font == null) {
            font = FontResource.getEmergencyFont();
        }
        return font;
    }

    /**
     * Gets a font of last resort, as finding fonts must not fail!
     * Currently using the default Java font, not matter how ugly.
     *
     * @return The default Java font.
     */
    public static Font getEmergencyFont() {
        return Font.decode(null);
    }

}
