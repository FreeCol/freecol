/**
 *  Copyright (C) 2002-2022   The FreeCol Team
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

import static net.sf.freecol.common.util.CollectionUtils.makeUnmodifiableMap;
import static net.sf.freecol.common.util.StringUtils.upCase;

import java.awt.Font;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import net.sf.freecol.common.resources.ResourceManager;


/**
 * Wraps anything {@code Font} related.
 * 
 * Should be used for getting a {@code Font} everywhere it is needed.
 */
public class FontLibrary {

    private static final Logger logger = Logger.getLogger(FontLibrary.class.getName());

    public static final float DEFAULT_UNSCALED_MAIN_FONT_SIZE = 12f;
    
    /** Default size, used for the main-font. */
    private static float mainFontSize = 12f;

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
    
    public static void setMainFontSize(float newMainFontSize) {
        mainFontSize = newMainFontSize;
    }
    
    public static float getMainFontSize() {
        return mainFontSize;
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
        mainFont = font = font.deriveFont(mainFontSize);
        return font;
    }
    
    public static Font getMainFont() {
        return mainFont.deriveFont(mainFontSize);
    }

    /**
     * Is a given font non-null and is it able to display some optional text?
     *
     * @param font The {@code Font} to check.
     * @param text Optional text to that the font must be able to represent.
     * @return True if the tests pass.
     */
    private static boolean displayTest(Font font, String text) {
        return font != null && (text == null || font.canDisplayUpTo(text) < 0);
    }

    /**
     * Try to find a basic font that of a given type that can display some text.
     *
     * @param type The {@code FontType} to try.
     * @param text Optional text that the font must be able to represent.
     * @return The {@code Font} found.
     */
    private static Font tryFont(FontType type, String text) {        
        String fontKey = getFontKey(type);
        if (fontKey == null) return null;
        Font ret = ResourceManager.getFont(fontKey);
        return (displayTest(ret, text)) ? ret : null;
    }

    /**
     * Get an unscaled font with a simple text specification.
     *
     * @param spec The font specification.
     * @return The {@code Font} found.
     */
    public static Font getUnscaledFont(String spec) {
        return getUnscaledFont(spec, null);
    }

    /**
     * Get an unscaled font with a simple text specification and optional
     * test string.
     *
     * The spec is a '-' delimited string with three parts.
     *   1. The type, a case-independent value of the FontType enum.
     *   2. The style, '+' delimited strings in ["plain", "bold", "italic"]
     *   3. The (absolute) size, a case-independent name of a Size enum.
     *
     * This routine *should* always return a font, but it is allowed to
     * throw exceptions if the font spec is bad.  It should not take too
     * long to find bad font specs.  AFAICT the only way there can be a
     * null result is if Font.deriveFont were to fail, which it is not
     * documented to do.
     *
     * Synchronized so as to be able to use a cache.
     *
     * @param spec The font specification.
     * @param text Optional text that the font must be able to represent.
     * @return The {@code Font} found.
     */
    public static synchronized Font getUnscaledFont(String spec, String text) {
        Font ret = FontLibrary.fontCache.get(spec);
        if (ret != null) {
            if (displayTest(ret, text)) return ret;
            // Failed the display test.  Try to fix these by changing the
            // spec at the call site.
            logger.warning("Fontlibrary cached font for " + spec
                + " failed to display: " + text);
        }

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
        // Try the given font type, then NORMAL and SIMPLE if distinct,
        // finally falling back to mainFont.
        ret = tryFont(type, text);
        if (ret == null && type != FontType.NORMAL) {
            ret = tryFont(FontType.NORMAL, text);
        }
        if (ret == null && type != FontType.SIMPLE) {
            ret = tryFont(FontType.SIMPLE, text);
        }
        if (ret == null) {
            if (text != null) {
                // Fall back to the main font, we are out of options.
                // This is bad, because every time we try *text* we
                // will end up here again.  Fix this warning if it happens!
                logger.warning("FontLibrary found no font for: " + text);
            }
            ret = mainFont;
        }
        ret = ret.deriveFont(style, size.forFont());
        if (ret == null) {
            logger.warning("FontLibrary could not derive font for: " + spec);
        } else {
            FontLibrary.fontCache.put(spec, ret);
        }
        return ret;
    }

    /**
     * Get a scaled font with a simple text specification.
     *
     * Beware the null return here.  Callers need to handle potential failure.
     *
     * @param spec The font specification.
     * @return The {@code Font} found, or null if scaling fails.
     */
    public static Font getScaledFont(String spec) {
        return getScaledFont(spec, null);
    }
    
    /**
     * Get a scaled font with a simple text specification.
     *
     * Beware the null return here.  Callers need to handle potential failure.
     *
     * @param spec The font specification.
     * @param text Optional text that the font must be able to represent.
     * @return The {@code Font} found, or null if scaling fails.
     */
    public static Font getScaledFont(String spec, String text) {
        final float scaleFactor = getFontScaling(); 
        return getScaledFont(spec, scaleFactor, text);
    }

    /**
     * Gets the scaling factor for the main font.
     * 
     * @return A scaling factor that can be used when scaling resources
     *      that should scale together with the font.
     */
    public static float getFontScaling() {
        return mainFontSize / DEFAULT_UNSCALED_MAIN_FONT_SIZE;
    }
    
    /**
     * Get a scaled font with a simple text specification.
     *
     * Beware the null return here.  Callers need to handle potential failure.
     *
     * @param spec The font specification.
     * @param scale The font scale (in addition to that in the specification).
     * @param text Optional text that the font must be able to represent.
     * @return The {@code Font} found, or null if scaling fails.
     */
    public static Font getScaledFont(String spec, float scale, String text) {
        String[] a = spec.split("-");
        if (a.length != 3) throw new RuntimeException("Bad font spec: " + spec);
        
        final Font unscaledFont = getUnscaledFont(spec, text);
        final Size size = Enum.valueOf(Size.class, upCase(a[2]));
        final float fontSize = Math.round(size.forFont() * scale);
        return unscaledFont.deriveFont(fontSize);
    }
}
