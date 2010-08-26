/**
 *  Copyright (C) 2002-2008  The FreeCol Team
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

import java.util.Iterator;

import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.Tension;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.Player.Stance;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.common.networking.ClaimLandMessage;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.ServerTestHelper;
import net.sf.freecol.server.control.Controller;
import net.sf.freecol.server.control.PreGameController;
import net.sf.freecol.server.model.ServerPlayer;
import net.sf.freecol.server.ai.mission.UnitSeekAndDestroyMission;
import net.sf.freecol.util.test.FreeColTestCase;
import net.sf.freecol.util.test.MockMapGenerator;

public class TensionTest extends FreeColTestCase {

    public void testTension()
    {
        // start a server
        FreeColServer server = null;
        
        try {
            server = ServerTestHelper.startServer(false, true);

            // generate a test map
            TileType plainsType = spec().getTileType("model.tile.plains");
            Map map = getTestMap(plainsType);
            assertNotNull(map);
            server.setMapGenerator(new MockMapGenerator(map));

            Controller c = server.getController();
            assertNotNull(c);
            assertTrue(c instanceof PreGameController);
            PreGameController pgc = (PreGameController)c;
            try {
                pgc.startGame();
            } catch (FreeColException e) {
                fail();
            }
            assertEquals(FreeColServer.GameState.IN_GAME, server.getGameState());
            Game game = server.getGame();
            assertNotNull(game);
            AIMain aiMain = server.getAIMain();
            assertNotNull(aiMain);

            ServerPlayer european = (ServerPlayer) game.getPlayer("model.nation.dutch");
            ServerPlayer indian = (ServerPlayer) game.getPlayer("model.nation.tupi");
            StandardAIPlayer indianAI = (StandardAIPlayer)aiMain.getAIObject(indian.getId());

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

            tile.setOwningSettlement(settlement);
            for (Tile newTile: tile.getSurroundingTiles(settlement.getRadius())) {
                newTile.setOwningSettlement(settlement);
                newTile.setOwner(indian);
            }
            int unitCount = settlement.getGeneratedUnitCount();
            for (int i = 0; i < unitCount; i++) {
                UnitType unitType = spec().getUnitType("model.unit.brave");
                Unit unit = new Unit(game, settlement, indian, unitType, UnitState.ACTIVE,
                                     unitType.getDefaultEquipment());
                unit.setIndianSettlement(settlement);
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
            UnitType unitType = spec().getUnitType("model.unit.hardyPioneer");
            @SuppressWarnings("unused") Unit unit1 = new Unit(game, map.getTile(7, 9), european, unitType, UnitState.ACTIVE);
            @SuppressWarnings("unused") Unit unit2 = new Unit(game, map.getTile(5, 9), european, unitType, UnitState.ACTIVE);

            // the european player steals 1 tile from the indians
            Tile tile2 = map.getTile(6,8);
            assertEquals(indian, tile2.getOwner());
            server.getInGameController().claimLand(european, tile2, null, ClaimLandMessage.STEAL_LAND);
            assertEquals(european, tile2.getOwner());

            // check the tension and stance have expected values
            tension = indian.getTension(european);
            stance = indian.getStance(european);
            assertNotNull(tension);
            assertEquals(Tension.TENSION_ADD_LAND_TAKEN, tension.getValue());
            assertEquals(Stance.PEACE, stance);

            // ask the AI to secure the settlement
            indianAI.secureIndianSettlement(settlement);

            // the tension and stance have not changed
            tension = indian.getTension(european);
            stance = indian.getStance(european);
            assertNotNull(tension);
            assertEquals(Tension.TENSION_ADD_LAND_TAKEN, tension.getValue());
            assertEquals(Stance.PEACE, stance);

            // but one brave will go on a rampage and declare war.
            Iterator<Unit> iter = settlement.getOwnedUnitsIterator();
            while (iter.hasNext()) {
                Unit brave = iter.next();
                AIUnit aiBrave = (AIUnit) aiMain.getAIObject(brave);
                if (aiBrave.getMission() instanceof UnitSeekAndDestroyMission) {
                    fail();
                }
            }
            // => Stealing a single tile of land with unarmed units (which pose not much threat)
            // results in Indian declaration of war.
            // this doesn't seem right, since TENSION_ADD_LAND_TAKEN < Level.CONTENT
            // Perhaps we should improve secureIndianSettlement() to better check the stance/tension
            // before jumping the gun and start with hostilities?
            // or should we adjust the tension values so that this doesn't happen?
        } finally {
            ServerTestHelper.stopServer(server);
        }
    }
}