/**
 *  Copyright (C) 2002-2015  The FreeCol Team
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

import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Stance;
import net.sf.freecol.common.model.Tension;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.util.LogBuilder;
import net.sf.freecol.common.networking.NetworkConstants;
import net.sf.freecol.server.ServerTestHelper;
import net.sf.freecol.server.control.InGameController;
import net.sf.freecol.server.model.ServerPlayer;
import net.sf.freecol.server.model.ServerUnit;
import net.sf.freecol.util.test.FreeColTestCase;


public class TensionTest extends FreeColTestCase {

    private static final TileType plainsType
        = spec().getTileType("model.tile.plains");


    @Override
    public void tearDown() throws Exception {
        ServerTestHelper.stopServerGame();
        super.tearDown();
    }


    public void testTension()  {
        Game game = ServerTestHelper.startServerGame(getTestMap(plainsType));
        Map map = game.getMap();
        InGameController igc = ServerTestHelper.getInGameController();
        AIMain aiMain = ServerTestHelper.getServer().getAIMain();

        ServerPlayer european = (ServerPlayer) game.getPlayerByNationId("model.nation.dutch");
        ServerPlayer indian = (ServerPlayer) game.getPlayerByNationId("model.nation.tupi");
        NativeAIPlayer indianAI = (NativeAIPlayer)aiMain.getAIPlayer(indian);

        // initially, the players are unknown to each other
        Tension tension = indian.getTension(european);
        Stance stance = indian.getStance(european);
        assertTrue(tension != null && tension.getValue() == 0);
        assertEquals(Stance.UNCONTACTED, stance);
        assertFalse(indian.hasContacted(european));

        // create an Indian settlement
        Tile tile = map.getTile(6, 9);

        FreeColTestCase.IndianSettlementBuilder builder = new FreeColTestCase.IndianSettlementBuilder(game);
        IndianSettlement settlement = builder.player(indian).settlementTile(tile).skillToTeach(null).capital(true).build();

        int unitCount = settlement.getType().getMaximumSize();
        for (int i = 0; i < unitCount; i++) {
            UnitType unitType = spec().getDefaultUnitType(indian);
            Unit unit = new ServerUnit(game, settlement, indian, unitType);
            unit.setHomeIndianSettlement(settlement);
            if (i == 0) {
                unit.setLocation(tile);
            } else {
                unit.setLocation(settlement);
            }
        }
        Player.makeContact(indian, european);
        tension = indian.getTension(european);
        stance = indian.getStance(european);
        assertNotNull(tension);
        assertEquals(Stance.PEACE, stance);

        // create 2 unarmed european units next to the indianSettlement
        Tile tile2 = map.getTile(6,8);
        UnitType unitType = spec().getUnitType("model.unit.hardyPioneer");
        Unit unit1 = new ServerUnit(game, tile2, european, unitType);
        european.exploreForUnit(unit1);

        // the european player steals 1 tile from the indians
        assertEquals(indian, tile2.getOwner());
        
        igc.claimLand(european, tile2, null, NetworkConstants.STEAL_LAND);
        assertEquals(european, tile2.getOwner());

        // check the tension and stance have expected values
        tension = indian.getTension(european);
        stance = indian.getStance(european);
        assertNotNull(tension);
        assertEquals(Tension.TENSION_ADD_LAND_TAKEN, tension.getValue());
        assertEquals(Stance.PEACE, stance);

        // ask the AI to secure the settlement
        indianAI.secureIndianSettlement(settlement, new LogBuilder(0));

        // the tension and stance have not changed
        tension = indian.getTension(european);
        stance = indian.getStance(european);
        assertNotNull(tension);
        assertEquals(Tension.TENSION_ADD_LAND_TAKEN, tension.getValue());
        assertEquals(Stance.PEACE, stance);
    }
}
