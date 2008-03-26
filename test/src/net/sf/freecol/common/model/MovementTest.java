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
import java.util.Set;

import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.util.test.FreeColTestCase;

public class MovementTest extends FreeColTestCase {


    TileType plains = spec().getTileType("model.tile.plains");
    TileType hills = spec().getTileType("model.tile.hills");
    TileType ocean = spec().getTileType("model.tile.ocean");

    UnitType galleonType = spec().getUnitType("model.unit.galleon");
    UnitType colonistType = spec().getUnitType("model.unit.freeColonist");

    public void testMoveFromPlainsToPlains() throws Exception {

        Game game = getStandardGame();
        Player dutch = game.getPlayer("model.nation.dutch");
        Map map = getTestMap(plains);
        game.setMap(map);
        Tile tile1 = map.getTile(5, 8);
        Tile tile2 = map.getTile(4, 8);
        tile1.setExploredBy(dutch, true);
        tile2.setExploredBy(dutch, true);

        Unit colonist = new Unit(game, tile1, dutch, colonistType, UnitState.ACTIVE);

        int moveCost = plains.getBasicMoveCost();
        assertEquals(moveCost, tile2.getMoveCost(tile1));
        assertEquals(Math.min(moveCost, colonistType.getMovement()),
                     colonist.getMoveCost(tile2));

    }

    public void testMoveFromPlainsToHills() throws Exception {

        Game game = getStandardGame();
        Player dutch = game.getPlayer("model.nation.dutch");
        Map map = getTestMap(plains);
        game.setMap(map);
        Tile tile1 = map.getTile(5, 8);
        Tile tile2 = map.getTile(4, 8);
        tile2.setType(hills);
        tile1.setExploredBy(dutch, true);
        tile2.setExploredBy(dutch, true);

        Unit colonist = new Unit(game, tile1, dutch, colonistType, UnitState.ACTIVE);

        int moveCost = hills.getBasicMoveCost();
        assertEquals(moveCost, tile2.getMoveCost(tile1));
        assertEquals(Math.min(moveCost, colonistType.getMovement()),
                     colonist.getMoveCost(tile2));

    }

    public void testMoveAlongRoad() throws Exception {

        Game game = getStandardGame();
        Player dutch = game.getPlayer("model.nation.dutch");
        Map map = getTestMap(plains);
        game.setMap(map);
        Tile tile1 = map.getTile(5, 8);
        Tile tile2 = map.getTile(4, 8);
        tile1.setExploredBy(dutch, true);
        tile2.setExploredBy(dutch, true);

        TileImprovementType roadType = spec().getTileImprovementType("model.improvement.Road");
        tile1.getTileItemContainer().addTileItem(new TileImprovement(game, tile1, roadType));
        tile2.getTileItemContainer().addTileItem(new TileImprovement(game, tile2, roadType));

        Unit colonist = new Unit(game, tile1, dutch, colonistType, UnitState.ACTIVE);

        int moveCost = 1;
        assertEquals(moveCost, tile2.getMoveCost(tile1));
        assertEquals(Math.min(moveCost, colonistType.getMovement()),
                     colonist.getMoveCost(tile2));

    }

}