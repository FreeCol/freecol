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
 * Wraps anything <code>Font</code> related and contains a scale factor.
 * 
 * Should be used for getting a <code>Font</code> everywhere it is needed.
 */
public class FontLibrary {

    private static final Logger logger = Logger.getLogger(FontLibrary.class.getName());

    /**
     * FontType is used for choosing the typeface of the <code>Font</code>.
     * 
     * Choices are:
     * <ul>
     * <li>NORMAL -- a normal system typeface</li>
     * <li>SIMPLE -- a simple typeface</li>
     * <li>HEADER -- a stylized old-fashioned typeface for headers</li>
     * </ul>
     */
    public static enum FontType {
        NORMAL,
        SIMPLE,
        HEADER
    }

    /**
     * FontSize allows for choosing the relative size of the <code>Font</code>.
     * 
     * Choices are:
     * <ul>
     * <li>TINY -- used for normal text</li>
     * <li>SMALLER -- used for subsubheaders</li>
     * <li>SMALL -- used for subheaders</li>
     * <li>MEDIUM -- used for some headers</li>
     * <li>BIG -- used for panel headers</li>
     * </ul>
     */
    public static enum FontSize {
        TINY,
        SMALLER,
        SMALL,
        MEDIUM,
        BIG
    }

    /**
     * How much the font size is scaled.
     */
    private final float scaleFactor;

    /**
     * Create a <code>FontLibrary</code> without scaling.
     */
    public FontLibrary() {
        this.scaleFactor = 1f;
    }

    /**
     * Create a <code>FontLibrary</code> with scaling.
     * 
     * @param scaleFactor How much scaling should be applied.
     *                    Typically the same value as in ImageLibrary.
     */
    public FontLibrary(float scaleFactor) {
        this.scaleFactor = scaleFactor;
    }

    /**
     * Create a default <code>Font</code> set on initialization of the GUI.
     * 
     * @param fontName Can be used to choose a different font from a
     *                 user-provided name.
    */
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

    /**
     * Create a scaled <code>Font</code>.
     * 
     * @param fontType How the font should look like.
     * @param fontSize Its relative size.
     * @param style The font style for choosing plain, bold or italic.
     * @return The created Font.
     */
    public Font createScaledFont(FontType fontType, FontSize fontSize,
                                 int style) {
        return createFont(fontType, fontSize, style, scaleFactor);
    }

    public static Font createFont(FontType fontType, FontSize fontSize) {
        return createFont(fontType, fontSize, Font.PLAIN, 1f);
    }

    /**
     * Create a <code>Font</code> in rare case one is needed without scaling.
     * 
     * @param fontType How the font should look like.
     * @param fontSize Its size.
     * @param style The font style for choosing plain, bold or italic.
     * @return The created Font.
     */
    public static Font createFont(FontType fontType, FontSize fontSize,
                                  int style) {
        return createFont(fontType, fontSize, style, 1f);
    }

    public static Font createFont(FontType fontType, FontSize fontSize,
                                  float scaleFactor) {
        return createFont(fontType, fontSize, Font.PLAIN, scaleFactor);
    }

    /**
     * Create a scaled <code>Font</code> whe the scale factor is provided
     * explicitly in the parameter.
     * 
     * @param fontType How the font should look like.
     * @param fontSize Its relative size.
     * @param style The font style for choosing plain, bold or italic.
     * @param scaleFactor The applied scale factor.
     * @return The created Font.
     */
    public static Font createFont(FontType fontType, FontSize fontSize,
                                  int style, float scaleFactor) {
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
        float scaledSize = pixelSize * scaleFactor;
        Font font = ResourceManager.getFont(fontName);
        font = font.deriveFont(style, scaledSize);
        return font;
    }

}
