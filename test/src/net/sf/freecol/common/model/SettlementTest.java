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

import net.sf.freecol.common.FreeColException;
import net.sf.freecol.util.test.FreeColTestCase;

public class SettlementTest extends FreeColTestCase {

    public void testSettlementRadius() throws FreeColException {

        Game game = getStandardGame();
        Map map = getTestMap();
        game.setMap(map);

        FreeColTestCase.IndianSettlementBuilder builder =
            new FreeColTestCase.IndianSettlementBuilder(game);

        Player arawak = game.getPlayer("model.nation.arawak");
        builder.player(arawak);
        IndianSettlement settlement = builder.build();
        assertEquals(1, arawak.getNationType().getSettlementType(false).getClaimableRadius());
        assertEquals(1, settlement.getType().getClaimableRadius());
        settlement.dispose();

        builder.capital(true);
        settlement = builder.build();
        assertEquals(2, arawak.getNationType().getCapitalType().getClaimableRadius());
        assertEquals(2, settlement.getType().getClaimableRadius());
        settlement.dispose();

        Player inca = game.getPlayer("model.nation.inca");
        builder.player(inca);
        builder.capital(false);
        settlement = builder.build();
        assertEquals(2, inca.getNationType().getSettlementType(false).getClaimableRadius());
        assertEquals(2, settlement.getType().getClaimableRadius());
        settlement.dispose();

        builder.capital(true);
        settlement = builder.build();
        assertEquals(3, inca.getNationType().getCapitalType().getClaimableRadius());
        assertEquals(3, settlement.getType().getClaimableRadius());
        settlement.dispose();
    }

    public void testColonyRadius() {

        Game game = getGame();
        Map map = getTestMap();
        game.setMap(map);

        Colony colony = getStandardColony();
        assertEquals(1, colony.getOwner().getNationType().getSettlementType(false).getClaimableRadius());
        //assertEquals(1, colony.getOwner().getNationType().getCapitalType().getClaimableRadius());
        assertEquals(1, colony.getRadius());
        assertFalse(colony.isCapital());

    }

    public void testSettlementDoesNotClaimWater() {

        Game game = getStandardGame();
        Map map = getTestMap();
        game.setMap(map);
        map.getTile(4, 8).setType(spec().getTileType("model.tile.ocean"));
        assertFalse(map.getTile(4, 8).isLand());

        FreeColTestCase.IndianSettlementBuilder builder =
            new FreeColTestCase.IndianSettlementBuilder(game);

        IndianSettlement settlement = builder.build();
        for (Tile tile : settlement.getTile().getSurroundingTiles(1)) {
            assertEquals(tile.isLand(), tile.getOwner() == settlement.getOwner());
            assertEquals(tile.isLand(), tile.getOwningSettlement() == settlement);
        }
    }

    public void testColonyClaimsWater() {

        Game game = getGame();
        Map map = getTestMap();
        game.setMap(map);
        map.getTile(4, 8).setType(spec().getTileType("model.tile.ocean"));
        assertFalse(map.getTile(4, 8).isLand());

        Colony colony = getStandardColony();
        for (Tile tile : colony.getTile().getSurroundingTiles(1)) {
            assertEquals("Tile " + tile.toString()
                         + " should be owned by " + colony.getOwner().getId(),
                         tile.getOwner(), colony.getOwner());
            assertEquals("Tile " + tile.toString()
                         + " should be owned by " + colony.getId(),
                         tile.getOwningSettlement(), colony);
        }

    }

}
