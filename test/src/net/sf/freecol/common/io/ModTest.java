/**
 *  Copyright (C) 2002-2019  The FreeCol Team
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

package net.sf.freecol.common.io;

import java.util.HashSet;
import java.util.Set;

import net.sf.freecol.util.test.FreeColTestCase;


public class ModTest extends FreeColTestCase {

    public void testAllMods() {
        Set<String> ids = new HashSet<String>();
        // Check that all mods can be loaded
        for (FreeColModFile mod : FreeColModFile.getModsList()) {
            assertNotNull("Null identifier for " + mod, mod.getId());
            ids.add(mod.getId());
        }

        assertFalse(ids.isEmpty());
        assertTrue(ids.contains("example"));
    }

    public void testGetRuleSets() {
        // Check that all rule sets can be loaded
        Set<String> ids = new HashSet<String>();
        for (FreeColTcFile tc : FreeColTcFile.getRulesList()) {
            assertNotNull("Null identifier for " + tc, tc.getId());
            ids.add(tc.getId());
        }
            
        assertTrue(ids.contains("freecol"));
        assertTrue(ids.contains("classic"));
        // Testing has no mod descriptor
        assertFalse(ids.contains("testing"));
    }
}
