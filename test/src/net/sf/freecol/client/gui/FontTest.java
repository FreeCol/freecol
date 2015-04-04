/**
 *  Copyright (C) 2002-2015  The FreeCol Team
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
import java.io.File;
import java.io.FileFilter;
import java.util.HashMap;
import java.util.Map;

import net.sf.freecol.util.test.FreeColTestCase;


public class FontTest extends FreeColTestCase {

    private static FileFilter ttfFilter = new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.getName().endsWith(".ttf");
            }
        };

    private static final Map<Character, String> specialCharacters = new HashMap<>();
    static {
        specialCharacters.put('\u00D7', "MULTIPLICATION SIGN");
        specialCharacters.put('\u2192', "RIGHTWARDS ARROW");
        specialCharacters.put('\u271D', "LATIN CROSS");
        specialCharacters.put('\u271E', "SHADOWED WHITE LATIN CROSS");
    }
    
    
    public void testLogicalSerif() {

        try {
            Font font = new Font("Serif", Font.PLAIN, 1);
            assertNotNull(font);
            for (Map.Entry<Character, String> entry : specialCharacters.entrySet()) {
                assertTrue(font.getName() + " can not display " + entry.getValue(),
                           font.canDisplay(entry.getKey()));
            }
        } catch (Exception e) {
            fail(e.toString());
        }
    }

    /*
    public void testLiberationSerif() {

        File liberationSerif = new File("data/base/resources/fonts/LiberationSerif-Regular.ttf");
        try {
            Font font = Font.createFont(Font.TRUETYPE_FONT, liberationSerif);
            assertNotNull(font);
            for (Map.Entry<Character, String> entry : specialCharacters.entrySet()) {
                assertTrue(font.getName() + " can not display " + entry.getValue(),
                           font.canDisplay(entry.getKey()));
            }
        } catch (Exception e) {
            fail(e.toString());
        }
    }
    */

}
