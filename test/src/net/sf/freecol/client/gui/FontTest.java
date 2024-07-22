/**
 *  Copyright (C) 2002-2024  The FreeCol Team
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

import static net.sf.freecol.common.util.CollectionUtils.forEachMapEntry;

import java.awt.Font;
import java.util.HashMap;
import java.util.Map;

import net.sf.freecol.util.test.FreeColTestCase;


public class FontTest extends FreeColTestCase {

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
            forEachMapEntry(specialCharacters, e ->
                assertTrue(font.getName() + " can not display " + e.getValue(),
                           font.canDisplay(e.getKey())));
        } catch (Exception e) {
            fail(e.toString());
        }
    }
}
