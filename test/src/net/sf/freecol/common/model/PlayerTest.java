/**
 *  Copyright (C) 2002-2007  The FreeCol Team
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

import java.util.Iterator;
import net.sf.freecol.util.test.FreeColTestCase;

public class PlayerTest extends FreeColTestCase {



    public void testGetREF(){
    	
    	Game g = getStandardGame();
    	
    	// Every european non ref player should have a REF player. 
    	for (Player p : g.getPlayers()){
            assertEquals(p.isEuropean() && !p.isREF(), p.getREFPlayer() != null);
    	}
    }


    public void testUnits() {

        Game game = getStandardGame();
        Player dutch = game.getPlayer("model.nation.dutch");
        Player french = game.getPlayer("model.nation.french");
        Map map = getTestMap(spec().getTileType("model.tile.plains"));
        game.setMap(map);
        map.getTile(4, 7).setExploredBy(dutch, true);
        map.getTile(4, 8).setExploredBy(dutch, true);
        map.getTile(5, 7).setExploredBy(dutch, true);
        map.getTile(5, 8).setExploredBy(dutch, true);

        UnitType freeColonist = spec().getUnitType("model.unit.freeColonist");

        Unit unit1 = new Unit(game, map.getTile(4, 7), dutch, freeColonist, Unit.ACTIVE);
        Unit unit2 = new Unit(game, map.getTile(4, 8), dutch, freeColonist, Unit.ACTIVE);
        Unit unit3 = new Unit(game, map.getTile(5, 7), dutch, freeColonist, Unit.ACTIVE);
        Unit unit4 = new Unit(game, map.getTile(5, 8), dutch, freeColonist, Unit.ACTIVE);

        int count = 0;
        Iterator<Unit> unitIterator = dutch.getUnitIterator();
        while (unitIterator.hasNext()) {
            unitIterator.next();
            count++;
        }
        assertTrue(count == 4);

        assertTrue(dutch.getUnit(unit1.getId()) == unit1);
        assertTrue(dutch.getUnit(unit2.getId()) == unit2);
        assertTrue(dutch.getUnit(unit3.getId()) == unit3);
        assertTrue(dutch.getUnit(unit4.getId()) == unit4);

        String id = unit1.getId();
        unit1.dispose();
        assertTrue(dutch.getUnit(id) == null);

        unit2.setOwner(french);
        assertTrue(dutch.getUnit(unit2.getId()) == null);
        assertTrue(french.getUnit(unit2.getId()) == unit2);

    }

    
}
