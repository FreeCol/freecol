/**
 *  Copyright (C) 2002-2011  The FreeCol Team
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

import net.sf.freecol.common.model.Specification;
import net.sf.freecol.server.model.ServerUnit;
import net.sf.freecol.util.test.FreeColTestCase;

public class EuropeTest extends FreeColTestCase {

    public void testMissionary() {

        Game game = getGame();
        Player dutch = game.getPlayer("model.nation.dutch");
        Europe amsterdam = dutch.getEurope();

        UnitType colonistType = spec().getUnitType("model.unit.freeColonist");
        Unit colonist = new ServerUnit(game, amsterdam, dutch, colonistType,
                                       Unit.UnitState.ACTIVE);

        assertTrue(amsterdam.hasAbility("model.ability.dressMissionary"));
        assertTrue(colonist.hasAbility("model.ability.dressMissionary"));

    }


}
