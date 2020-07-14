/**
 *  Copyright (C) 2002-2020   The FreeCol Team
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
import java.util.Map;

import net.sf.freecol.common.resources.ResourceManager;
import static net.sf.freecol.common.util.CollectionUtils.*;


/**
 * Wraps anything {@code Font} related.
 * 
 * Should be used for getting a {@code Font} everywhere it is needed.
 */
public class FontLibrary {

    private static final Logger logger = Logger.getLogger(FontLibrary.class.getName());

    /**
     * FontType is used for choosing the typeface of the {@code Font}.
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
     * FontSize allows for choosing the relative size of the {@code Font}.
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
     * Scale to use if otherwise unspecified.
     * May be redundant but we avoid magic numbers.
     */
    private static final float DEFAULT_SCALE = 1f;
    
    /** Conversion map for getFontKey. */
    private static final Map<FontType, String> keyMap = makeUnmodifiableMap(
            new FontType[] { FontType.NORMAL, FontType.SIMPLE,
                             FontType.HEADER },
            new String[] { "font.normal", "font.simple", "font.header" });
    
    /** Conversion map for getScaledSize. */
    private static final Map<FontSize, Float> scaleMap = makeUnmodifiableMap(
            new FontSize[] { FontSize.TINY, FontSize.SMALLER, FontSize.SMALL,
                             FontSize.MEDIUM, FontSize.BIG },
            new Float[] { 12f, 16f, 24f, 36f, 48f });

    /** Cache for the (optional) custom main Font. */
    private static Font mainFont = null;


    /**
     * Convert a {@code FontType} to a resource key.
     *
     * @param fontType The {@code FontType} to convert.
     * @return The resource key, or null if the main font should be used.
     */
    private static String getFontKey(FontType fontType) {
        return (fontType == FontType.NORMAL && mainFont != null) ? null
            : FontLibrary.keyMap.getOrDefault(fontType, (String)null);
    }

    /**
     * Convert a {@code FontSize} and scale factor to float.
     *
     * @return The conversion result.
     */
    private static float getScaledSize(FontSize fontSize, float scaleFactor) {
        return FontLibrary.scaleMap.get(fontSize) * scaleFactor;
    }

    /**
     * Create a default {@code Font} set on initialization of the GUI.
     * 
     * @param fontName Can be used to choose a different font from a
     *     user-provided name.
     * @param scaleFactor The applied scale factor.
     * @return The new {@code Font}.
     */
    public static Font createMainFont(String fontName, float scaleFactor) {
        final float defaultSize = 12f * scaleFactor;
        Font font = null;
        if (fontName != null) font = Font.decode(fontName);
        if (font == null) font = ResourceManager.getFont("font.normal");
        mainFont = font = font.deriveFont(defaultSize);
        return font;
    }

    /**
     * Create a font of given type and size.
     *
     * @param fontType How the font should look like.
     * @param fontSize Its size.
     * @return The font created.
     */
    public static Font createFont(FontType fontType, FontSize fontSize) {
        return createFont(fontType, fontSize, Font.PLAIN, DEFAULT_SCALE);
    }

    /**
     * Create a font of given type, size and style.
     * 
     * @param fontType How the font should look like.
     * @param fontSize Its size.
     * @param style The font style for choosing plain, bold or italic.
     * @return The created Font.
     */
    public static Font createFont(FontType fontType, FontSize fontSize,
                                  int style) {
        return createFont(fontType, fontSize, style, DEFAULT_SCALE);
    }

    /**
     * Create a font of given type, size and scale factor.
     * 
     * @param fontType How the font should look like.
     * @param fontSize Its size.
     * @param scaleFactor The applied scale factor.
     * @return The created Font.
     */
    public static Font createFont(FontType fontType, FontSize fontSize,
                                  float scaleFactor) {
        return createFont(fontType, fontSize, Font.PLAIN, scaleFactor);
    }

    /**
     * Create a font of given type, size, style and scale factor.
     * 
     * @param fontType How the font should look like.
     * @param fontSize Its relative size.
     * @param style The font style for choosing plain, bold or italic.
     * @param scaleFactor The applied scale factor.
     * @return The created Font.
     */
    public static Font createFont(FontType fontType, FontSize fontSize,
                                  int style, float scaleFactor) {
        float scaledSize = getScaledSize(fontSize, scaleFactor);
        String fontKey = getFontKey(fontType);
        Font font = (fontKey == null) ? mainFont
            : ResourceManager.getFont(fontKey);
        return font.deriveFont(style, scaledSize);
    }

    /**
     * Create a scaled {@code Font} which can display all characters
     * inside the given text string.
     * 
     * @param string The text to find a compatible font for.
     * @param fontType How the font should look like.
     * @param fontSize Its relative size.
     * @return The created Font.
     */
    public static Font createCompatibleFont(String string, FontType fontType,
                                            FontSize fontSize) {
        return createCompatibleFont(string, fontType, fontSize, Font.PLAIN,
                                    DEFAULT_SCALE);
    }

    /**
     * Create a scaled {@code Font} which can display all characters
     * inside the given text string.
     * 
     * @param string The text to find a compatible font for.
     * @param fontType How the font should look like.
     * @param fontSize Its relative size.
     * @param style The font style for choosing plain, bold or italic.
     * @return The created Font.
     */
    public static Font createCompatibleFont(String string, FontType fontType,
                                            FontSize fontSize, int style) {
        return createCompatibleFont(string, fontType, fontSize, style,
                                    DEFAULT_SCALE);
    }

    /**
     * Create a scaled {@code Font} which can display all characters
     * inside the given text string.
     * 
     * @param string The text to find a compatible font for.
     * @param fontType How the font should look like.
     * @param fontSize Its relative size.
     * @param scaleFactor The applied scale factor.
     * @return The created Font.
     */
    public static Font createCompatibleFont(String string, FontType fontType,
                                            FontSize fontSize,
                                            float scaleFactor) {
        return createCompatibleFont(string, fontType, fontSize, Font.PLAIN,
                                    scaleFactor);
    }

    /**
     * Create a scaled {@code Font} which can display all characters
     * inside the given text string.
     *
     * This is mostly necessary for the header font because the currently
     * used ShadowedBlack is missing support for CJK and others. Even some
     * special glyphs for European languages like the triple-dot are missing.
     * 
     * @param string The text to find a compatible font for.
     * @param fontType How the font should look like.
     * @param fontSize Its relative size.
     * @param style The font style for choosing plain, bold or italic.
     * @param scaleFactor The applied scale factor.
     * @return The created Font.
     */
    private static Font createCompatibleFont(String string, FontType fontType,
                                             FontSize fontSize,
                                             int style, float scaleFactor) {
        // TODO: Consider testing the normal font for compatibility and try
        //       some or all other available fonts for complete/longest match:
        //       header/simple->main->normal->simple/header->emergency
        Font font = null;
        // Try testing several font types
        for (FontType ft : new FontType[] { fontType, FontType.NORMAL,
                                            FontType.SIMPLE }) {
            String fontKey = getFontKey(ft);
            if (fontKey == null) continue;
            font = ResourceManager.getFont(fontKey);
            // If the font was found and there are no characters in the
            // test string that it can not display, we have succeeded
            if (font != null && font.canDisplayUpTo(string) < 0) break;
        }
        // Fall back to the main font, even if it is not necessarily
        // compatible.  We are out of options here.
        if (font == null) {
            logger.warning("No compatible fonts found: " + string);
            font = mainFont;
        }
        return font.deriveFont(style, getScaledSize(fontSize, scaleFactor));
    }
}
