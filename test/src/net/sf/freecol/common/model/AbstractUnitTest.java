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

import java.util.List;
import net.sf.freecol.util.test.FreeColTestCase;


public class AbstractUnitTest extends FreeColTestCase {

    public void testEquipment() {
        AbstractUnit newUnit
            = new AbstractUnit("newUnit", "model.role.pioneer", 1);
        List<EquipmentType> equipment = spec().getRoleEquipment(newUnit.getRoleId(), true);
        assertEquals(5, equipment.size());
        assertEquals("model.equipment.tools", equipment.get(0).getId());
        assertEquals("model.equipment.tools", equipment.get(1).getId());
        assertEquals("model.equipment.tools", equipment.get(2).getId());
        assertEquals("model.equipment.tools", equipment.get(3).getId());
        assertEquals("model.equipment.tools", equipment.get(4).getId());
    }

}
