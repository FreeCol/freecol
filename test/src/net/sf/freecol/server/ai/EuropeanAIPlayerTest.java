/**
 *  Copyright (C) 2002-2024  The FreeCol Team
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

package net.sf.freecol.server.ai;

import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.GoodsContainer;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.server.ServerTestHelper;
import net.sf.freecol.server.model.ServerPlayer;
import net.sf.freecol.server.model.ServerUnit;
import net.sf.freecol.util.test.FreeColTestCase;


public class EuropeanAIPlayerTest extends FreeColTestCase {

    private static final GoodsType fursType = spec().getGoodsType("model.goods.furs");
    private static final TileType savannahType = spec().getTileType("model.tile.savannah");
    private static final UnitType caravelType = spec().getUnitType("model.unit.caravel");

    
    @Override
    public void tearDown() throws Exception {
        ServerTestHelper.stopServerGame();
        super.tearDown();
    }

    
    /**
     * Verifies that a ship exports goods on the same turn as it was added (provided there are
     * no other transportation tasks). 
     */
    public void testGoodsTransportHappensTheSameTurn() {
        final Game game = ServerTestHelper.startServerGame(getCoastTestMap(savannahType));
        final Map map = game.getMap();
        final AIMain aiMain = ServerTestHelper.getServer().getAIMain();
        
        final ServerPlayer player = getServerPlayer(game, "model.nation.dutch");
        final EuropeanAIPlayer aiPlayer = (EuropeanAIPlayer)aiMain.getAIPlayer(player);
        player.exploreMap(true);
        
        // create a coastal colony
        final Tile colonyTile = map.getTile(9, 9);
        final Colony colony = createStandardColony(1, colonyTile.getX(), colonyTile.getY());
       
        // Create a unit that can transport the exported goods
        final Unit caravel = new ServerUnit(game, colony.getTile(), player, caravelType);
        
        game.setCurrentPlayer(colony.getOwner());

        // Give colony more than 100 furs - should trigger an export
        colony.addGoods(fursType, 110);
        // Run one turn
        aiPlayer.startWorking();
        
        // Verify furs are exported and loaded onto caravel
        final GoodsContainer gc = caravel.getGoodsContainer();
        final int numberOfFurs = gc.getGoodsCount(fursType);
        assertEquals(110, numberOfFurs);
    }
}
