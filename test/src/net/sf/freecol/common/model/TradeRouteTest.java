/**
 *  Copyright (C) 2002-2014  The FreeCol Team
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

import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Player.NoClaimReason;
import net.sf.freecol.common.model.Role;
import net.sf.freecol.server.model.ServerUnit;
import net.sf.freecol.server.model.ServerPlayer;
import net.sf.freecol.util.test.FreeColTestCase;


public class TradeRouteTest extends FreeColTestCase {

    private static final GoodsType fursGoodsType
        = spec().getGoodsType("model.goods.furs");

    private static final TileType mixedForestType
        = spec().getTileType("model.tile.mixedForest");

    private static final UnitType caravel
        = spec().getUnitType("model.unit.caravel");
    private static final UnitType wagonTrainType
        = spec().getUnitType("model.unit.wagonTrain");


    public void testTradeRoute() {
        Game game = getGame();
        game.setMap(getTestMap(mixedForestType, true));

        // Set up three colonies
        Colony colony1 = getStandardColony(4, 1, 1);
        Colony colony2 = getStandardColony(4, 3, 1);
        Colony colony3 = getStandardColony(4, 5, 1);
        Tile tile1 = colony1.getTile();
        Tile tile2 = colony2.getTile();
        Tile tile3 = colony3.getTile();
        assertEquals(mixedForestType, tile1.getType());
        assertEquals(mixedForestType, tile2.getType());
        assertEquals(mixedForestType, tile3.getType());
        Player player = colony1.getOwner();

        // Create a wagon
        Unit wagon = new ServerUnit(game, tile1, player, wagonTrainType);

        wagon.setLocation(tile1);
        assertEquals(2, wagon.getTurnsToReach(colony2));
        assertEquals(4, wagon.getTurnsToReach(colony3));
        wagon.setLocation(tile2);
        assertEquals(2, wagon.getTurnsToReach(colony1));
        assertEquals(2, wagon.getTurnsToReach(colony3));
        wagon.setLocation(tile3);
        assertEquals(4, wagon.getTurnsToReach(colony1));
        assertEquals(2, wagon.getTurnsToReach(colony2));

        // Create a trade route and assign to the wagon
        TradeRoute tr = new TradeRoute(game, "TR", player);
        assertNotNull(tr);
        assertNotNull(tr.verify()); // Invalid, no stops
        assertTrue(tr.getStops().isEmpty());
        TradeRouteStop trs1 = new TradeRouteStop(game, colony1);
        assertTrue(trs1.isValid(player));
        tr.addStop(trs1);
        assertNotNull(tr.verify()); // Invalid, one stop is not enough
        TradeRouteStop trs2 = new TradeRouteStop(game, colony2);
        assertTrue(trs2.isValid(player));
        tr.addStop(trs2);
        assertNotNull(tr.verify()); // Invalid, all stops are empty
        trs1.addCargo(fursGoodsType);
        assertNull(tr.verify()); // Now finally valid
        trs2.addCargo(fursGoodsType);
        assertNotNull(tr.verify()); // Invalid again, furs always present
        TradeRouteStop trs3 = new TradeRouteStop(game, colony3);
        assertTrue(trs3.isValid(player));
        tr.addStop(trs3);
        assertNull(tr.verify()); // Valid again, furs dumped at colony3
    }
}
