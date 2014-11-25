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
        wagon.setLocation(tile1);

        // Create a trade route
        TradeRoute tr = new TradeRoute(game, "TR", player);
        assertNotNull(tr);
       
        // Build towards validity
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

        // Assign the trade route
        wagon.setTradeRoute(tr);

        // Check the import and export amounts
        final int ex = 50;
        assertEquals(GoodsContainer.CARGO_SIZE * 2,
                     wagon.getLoadableAmount(fursGoodsType));
        assertEquals(0, colony1.getGoodsCount(fursGoodsType));
        assertEquals(0, colony2.getGoodsCount(fursGoodsType));
        assertEquals(0, colony3.getGoodsCount(fursGoodsType));
        colony1.getExportData(fursGoodsType).setExportLevel(ex);
        colony2.getExportData(fursGoodsType).setExportLevel(ex);
        colony3.getExportData(fursGoodsType).setExportLevel(ex);
        assertEquals(-ex, colony1.getExportAmount(fursGoodsType));
        assertEquals(-ex, colony2.getExportAmount(fursGoodsType));
        assertEquals(-ex, colony3.getExportAmount(fursGoodsType));
        assertEquals(-ex, trs1.getExportAmount(fursGoodsType));
        assertEquals(-ex, trs2.getExportAmount(fursGoodsType));
        assertEquals(-ex, trs3.getExportAmount(fursGoodsType));
        assertEquals(GoodsContainer.CARGO_SIZE,
                     trs1.getImportAmount(fursGoodsType));
        assertEquals(GoodsContainer.CARGO_SIZE,
                     trs2.getImportAmount(fursGoodsType));
        assertEquals(GoodsContainer.CARGO_SIZE,
                     trs3.getImportAmount(fursGoodsType));
        assertTrue(trs1.getNetProductionOf(fursGoodsType) > 0);
        assertTrue(trs2.getNetProductionOf(fursGoodsType) > 0);
        assertTrue(trs3.getNetProductionOf(fursGoodsType) > 0);

        // Which stops have work?
        // Initially nothing to collect, and therefore nothing to deliver
        assertFalse(wagon.hasWorkAtStop(trs1, false));
        assertFalse(wagon.hasWorkAtStop(trs2, false));
        assertFalse(wagon.hasWorkAtStop(trs3, false));
        // Add some goods and delivery becomes valid
        wagon.addGoods(fursGoodsType, 10);
        assertFalse(wagon.hasWorkAtStop(trs1, false));
        assertFalse(wagon.hasWorkAtStop(trs2, false));
        assertTrue(wagon.hasWorkAtStop(trs3, false));
        wagon.removeGoods(fursGoodsType);
        // Now allow production check.  There will still be nothing to
        // collect because of the export level.
        assertFalse(wagon.hasWorkAtStop(trs1, true));
        assertFalse(wagon.hasWorkAtStop(trs2, true));
        assertFalse(wagon.hasWorkAtStop(trs3, true));
        // Zero the export levels.  Now first colony will still not
        // export because it has not produced anything yet, but the
        // second (for which turnsToReach() > 0) will have goods to
        // export.
        colony1.getExportData(fursGoodsType).setExportLevel(0);
        colony2.getExportData(fursGoodsType).setExportLevel(0);
        colony3.getExportData(fursGoodsType).setExportLevel(0);
        assertFalse(wagon.hasWorkAtStop(trs1, true));
        assertTrue(wagon.hasWorkAtStop(trs2, true));
        assertFalse(wagon.hasWorkAtStop(trs3, true));
    }
}
