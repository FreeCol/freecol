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
import java.util.HashMap;
import java.util.Map;

import net.sf.freecol.common.resources.ResourceManager;
import static net.sf.freecol.common.util.CollectionUtils.*;
import static net.sf.freecol.common.util.StringUtils.*;


/**
 * Wraps anything {@code Font} related.
 * 
 * Should be used for getting a {@code Font} everywhere it is needed.
 */
public class FontLibrary {

    private static final Logger logger = Logger.getLogger(FontLibrary.class.getName());

    /** Default size, used for the main-font. */
    public static final float DEFAULT_FONT_SIZE = 12f;

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
     * Scale to use if otherwise unspecified.
     * May be redundant but we avoid magic numbers.
     */
    private static final float DEFAULT_SCALE = 1f;
    
    /** Conversion map for getFontKey. */
    private static final Map<FontType, String> keyMap = makeUnmodifiableMap(
            new FontType[] { FontType.NORMAL, FontType.SIMPLE,
                             FontType.HEADER },
            new String[] { "font.normal", "font.simple", "font.header" });
    
    /** Cache for the (optional) custom main Font. */
    private static Font mainFont = null;

    /** The font cache. */
    private static final Map<String, Font> fontCache = new HashMap<>();


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
     * Convert a font size and scale factor to float.
     *
     * @param fontSize The font size expressed as a {@code Size}.
     * @param scaleFactor A secondary scaling.
     * @return The conversion result.
     */
    private static float getScaledSize(Size fontSize, float scaleFactor) {
        return fontSize.forFont() * scaleFactor;
    }
    
    /**
     * Create a default {@code Font} set on initialization of the GUI.
     * 
     * @param fontName Can be used to choose a different font from a
     *     user-provided name.
     * @return The new {@code Font}.
     */
    public static Font createMainFont(String fontName) {
        Font font = null;
        if (fontName != null) font = Font.decode(fontName);
        if (font == null) font = ResourceManager.getFont("font.normal");
        mainFont = font = font.deriveFont(DEFAULT_FONT_SIZE);
        return font;
    }

    /**
     * Create a font of given type, size, style and scale factor.
     * 
     * @param fontType How the font should look like.
     * @param fontSize Its relative size.
     * @param style The font style for choosing plain, bold or italic.
     * @return The created Font.
     */
    public static Font createFont(FontType fontType, Size fontSize, int style) {
        String fontKey = getFontKey(fontType);
        Font font = (fontKey == null) ? mainFont
            : ResourceManager.getFont(fontKey);
        return font.deriveFont(style, fontSize.forFont());
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
                                            Size fontSize) {
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
                                            Size fontSize, int style) {
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
                                            Size fontSize,
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
                                             Size fontSize,
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

    /**
     * Get an unscaled font with a simple text specification.
     *
     * The spec is a '-' delimited string with three parts.
     *   1. The type, a case-independent value of the FontType enum.
     *   2. The style, '+' delimited strings in ["plain", "bold", "italic"]
     *   3. The (absolute) size, a case-independent value of the Size enum.
     *
     * This routine *should* always return a font, but it is allowed to
     * throw exceptions if the font spec is bad.  It should not take too
     * long to find bad font specs.
     *
     * Synchronized so as to be able to use a cache.
     *
     * @param spec The font specification.
     * @return The {@code Font} found.
     */
    public static synchronized Font getUnscaledFont(String spec) {
        Font ret = FontLibrary.fontCache.get(spec);
        if (ret != null) return ret;
        
        String[] a = spec.split("-");
        if (a.length != 3) throw new RuntimeException("Bad font spec: " + spec);
        FontType type = Enum.valueOf(FontType.class, upCase(a[0]));
        String[] styles = upCase(a[1]).split("\\+");
        int style = 0;
        for (String s : styles) {
            int x = ("PLAIN".equals(s)) ? Font.PLAIN
                : ("BOLD".equals(s)) ? Font.BOLD
                : ("ITALIC".equals(s)) ? Font.ITALIC
                : -1;
            if (x < 0) throw new RuntimeException("Bad font style: " + s);
            style |= x;
        }
        Size size = Enum.valueOf(Size.class, upCase(a[2]));
        ret = createFont(type, size, style);
        FontLibrary.fontCache.put(spec, ret);
        return ret;
    }

    /**
     * Get a scaled font with a simple text specification.
     *
     * Beware the null return here.  Callers need to handle potential failure.
     *
     * @param spec The font specification.
     * @param scale The font scale (in addition to that in the specification).
     * @return The {@code Font} found, or null if scaling fails.
     */
    public static Font getScaledFont(String spec, float scale) {
        String[] a = spec.split("-");
        if (a.length != 3) throw new RuntimeException("Bad font spec: " + spec);
        Size size = Enum.valueOf(Size.class, upCase(a[2]));
        Size newSize = size.scaled(scale);
        if (newSize == null) return null;
        String newSpec = join("-", a[0], a[1], downCase(newSize.toString()));
        return getUnscaledFont(newSpec);
    }
}
