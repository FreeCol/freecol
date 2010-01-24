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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.model.NationOptions;
import net.sf.freecol.server.model.ServerGame;
import net.sf.freecol.util.test.FreeColTestCase;
import net.sf.freecol.util.test.MockModelController;

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
        assertEquals(1, arawak.getNationType().getSettlementRadius());
        assertEquals(1, settlement.getRadius());
        settlement.dispose();

        builder.capital(true);
        settlement = builder.build();
        assertEquals(2, arawak.getNationType().getCapitalRadius());
        assertEquals(2, settlement.getRadius());
        settlement.dispose();

        Player inca = game.getPlayer("model.nation.inca"); 
        builder.player(inca);
        builder.capital(false);
        settlement = builder.build();
        assertEquals(2, inca.getNationType().getSettlementRadius());
        assertEquals(2, settlement.getRadius());
        settlement.dispose();

        builder.capital(true);
        settlement = builder.build();
        assertEquals(3, inca.getNationType().getCapitalRadius());
        assertEquals(3, settlement.getRadius());
        settlement.dispose();
    }

    public void testColonyRadius() {

        Game game = getGame();
        Map map = getTestMap();
        game.setMap(map);
        
        Colony colony = getStandardColony();
        assertEquals(1, colony.getOwner().getNationType().getSettlementRadius());
        assertEquals(2, colony.getOwner().getNationType().getCapitalRadius());
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
        for (Tile tile : map.getSurroundingTiles(settlement.getTile(), 1)) {
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
        for (Tile tile : map.getSurroundingTiles(colony.getTile(), 1)) {
            assertEquals(tile.getOwner(), colony.getOwner());
            assertEquals(tile.getOwningSettlement(), colony);
        }

    }

}
