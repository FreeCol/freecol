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

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.net.URI;
import java.net.URL;
import java.util.logging.Level;
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
     *
     * @param resourceLocator The <code>URI</code> used when loading this
     *      resource.
     */
    public FontResource(URI resourceLocator) throws Exception {
        super(resourceLocator);
        font = null;
        try {
            if (resourceLocator.getPath() != null
                && resourceLocator.getPath().endsWith(".ttf")) {
                URL url = resourceLocator.toURL();
                font = Font.createFont(Font.TRUETYPE_FONT, url.openStream());
            } else {
                String name = resourceLocator.getSchemeSpecificPart();
                font = Font.decode(name.substring(SCHEME.length()));
            }

            if (font != null) {
                GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .registerFont(font);
            }

            logger.info("Loaded font: " + font.getFontName()
                + " from: " + resourceLocator);
        } catch(Exception e) {
            logger.log(Level.WARNING,
                "Failed loading font from: " + resourceLocator, e);
            throw e;
        }
    }


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
            logger.warning("Font is null");
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
        logger.warning("Using emergency font");
        return Font.decode(null);
    }
}
