/**
 *  Copyright (C) 2002-2013  The FreeCol Team
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

package net.sf.freecol.common.model;

import net.sf.freecol.util.test.FreeColTestCase;

public class RoleTest extends FreeColTestCase {

    public void testComparators() {

        Role soldier = spec().getRole("model.role.soldier");
        Role dragoon = spec().getRole("model.role.dragoon");
        Role missionary = spec().getRole("model.role.missionary");

        assertEquals(0, Role.defensiveComparator.compare(soldier, soldier));
        assertEquals(0, Role.defensiveComparator.compare(dragoon, dragoon));
        assertEquals(0, Role.defensiveComparator.compare(missionary, missionary));

        assertEquals(-1, Role.defensiveComparator.compare(dragoon, soldier));
        assertEquals(1, Role.defensiveComparator.compare(soldier, dragoon));

        assertEquals(1,  Role.defensiveComparator.compare(missionary, soldier));
        assertEquals(1,  Role.defensiveComparator.compare(missionary, dragoon));

    }


}
