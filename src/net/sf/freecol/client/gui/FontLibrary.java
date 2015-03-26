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

package net.sf.freecol.client.gui;

import java.awt.Font;
import java.util.logging.Logger;
import net.sf.freecol.common.resources.ResourceManager;

/**
 * Wraps anything Font related.
 *
 */
public class FontLibrary {

    private static final Logger logger = Logger.getLogger(FontLibrary.class.getName());
    
    /** Font to use for text areas. */
    public static final Font DEFAULT_FONT
        = ResourceManager.getFont("NormalFont").deriveFont(13f);
    /** Bold version of the default font. */
    public static final Font DEFAULT_BOLD_FONT
        = DEFAULT_FONT.deriveFont(Font.BOLD);
    /** Simple fonts for arrows, labels in panels etc. */
    public static final Font TINY_SIMPLE_FONT
        = ResourceManager.getFont("SimpleFont").deriveFont(Font.BOLD, 12f);
    public static final Font SMALLER_SIMPLE_FONT
        = ResourceManager.getFont("SimpleFont").deriveFont(Font.BOLD, 16f);
    public static final Font SMALL_SIMPLE_FONT
        = ResourceManager.getFont("SimpleFont").deriveFont(Font.BOLD, 24f);
    /** Header fonts for reports etc. */
    public static final Font SMALL_HEADER_FONT
        = ResourceManager.getFont("HeaderFont").deriveFont(24f);
    public static final Font MEDIUM_HEADER_FONT
        = ResourceManager.getFont("HeaderFont").deriveFont(36f);
    public static final Font BIG_HEADER_FONT
        = ResourceManager.getFont("HeaderFont").deriveFont(48f);

    public static enum FontType {
        NORMAL,
        SIMPLE,
        HEADER
    }

    public static enum FontSize {
        TINY,
        SMALLER,
        SMALL,
        MEDIUM,
        BIG
    }

    private final float scaleFactor;

    public FontLibrary() {
        this.scaleFactor = 1f;
    }

    public FontLibrary(float scaleFactor) {
        this.scaleFactor = scaleFactor;
    }

    static Font createMainFont(String fontName) {
        if (fontName != null) {
            Font font = Font.decode(fontName);
            if (font != null)
                return font;
            logger.warning("Font not found: " + fontName);
        }
        return ResourceManager.getFont("NormalFont");
    }

    public Font createScaledFont(FontType fontType, FontSize fontSize) {
        return createFont(fontType, fontSize, Font.PLAIN, scaleFactor);
    }

    public Font createScaledFont(FontType fontType, FontSize fontSize, int style) {
        return createFont(fontType, fontSize, style, scaleFactor);
    }

    public static Font createFont(FontType fontType, FontSize fontSize) {
        return createFont(fontType, fontSize, Font.PLAIN, 1f);
    }

    public static Font createFont(FontType fontType, FontSize fontSize, int style) {
        return createFont(fontType, fontSize, style, 1f);
    }

    public static Font createFont(FontType fontType, FontSize fontSize, float scaleFactor) {
        return createFont(fontType, fontSize, Font.PLAIN, scaleFactor);
    }

    public static Font createFont(FontType fontType, FontSize fontSize, int style, float scaleFactor) {
        String fontName;
        switch(fontType) {
            default:
                logger.warning("Unknown FontType");
            case NORMAL:
                fontName = "NormalFont";
                break;
            case SIMPLE:
                fontName = "SimpleFont";
                break;
            case HEADER:
                fontName = "HeaderFont";
        }
        float pixelSize;
        switch(fontSize) {
            default:
                logger.warning("Unknown FontSize");
            case TINY:
                pixelSize = 12f;
                break;
            case SMALLER:
                pixelSize = 16f;
                break;
            case SMALL:
                pixelSize = 24f;
                break;
            case MEDIUM:
                pixelSize = 36f;
                break;
            case BIG:
                pixelSize = 48f;
        }
        float scaledSize = pixelSize*scaleFactor;
        Font font = ResourceManager.getFont(fontName);
        font = font.deriveFont(style, scaledSize);
        return font;
    }

}
