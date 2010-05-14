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

package net.sf.freecol.server.control;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.BuildingType;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Tension;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.Player.Stance;
import net.sf.freecol.common.model.Tension.Level;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.ServerTestHelper;
import net.sf.freecol.server.model.ServerPlayer;
import net.sf.freecol.util.test.FreeColTestCase;
import net.sf.freecol.util.test.MockMapGenerator;

public class InGameControllerTest extends FreeColTestCase {
    BuildingType schoolHouseType = spec().getBuildingType("model.building.Schoolhouse");
    TileType plains = spec().getTileType("model.tile.plains");
    UnitType colonistType = spec().getUnitType("model.unit.freeColonist");
    UnitType hardyPioneerType = spec().getUnitType("model.unit.hardyPioneer");
    UnitType wagonTrainType = spec().getUnitType("model.unit.wagonTrain");
    UnitType caravelType = spec().getUnitType("model.unit.caravel");
    UnitType galleonType = spec().getUnitType("model.unit.galleon");
    UnitType missionaryType = spec().getUnitType("model.unit.jesuitMissionary");
    UnitType treasureTrainType = spec().getUnitType("model.unit.treasureTrain");
	
    FreeColServer server = null;
    
    public void tearDown() throws Exception {
        if(server != null) {
            // must make sure that the server is stopped
            ServerTestHelper.stopServer(server);
            server = null;
        }
        super.tearDown();
    }

    private Game start(Map map) {
        if (server == null) {
            server = ServerTestHelper.startServer(false, true);
        }
        server.setMapGenerator(new MockMapGenerator(map));
        PreGameController pgc = (PreGameController) server.getController();
        try {
            pgc.startGame();
        } catch (FreeColException e) {
            fail("Failed to start game");
        }
        Game game = server.getGame();
        FreeColTestCase.setGame(game);
        return game;
    }

    public void testDeclarationOfWarFromPeace() {
        String errMsg = "";
        Game game = start(getTestMap());
        InGameController igc = (InGameController) server.getController();

        Player dutch = game.getPlayer("model.nation.dutch");
        Player french = game.getPlayer("model.nation.french");

        int initialTensionValue = 500;

        // setup
        dutch.setStance(french, Stance.PEACE);
        french.setStance(dutch, Stance.PEACE);
        dutch.setTension(french, new Tension(initialTensionValue));
        french.setTension(dutch, new Tension(initialTensionValue));

        // verify initial conditions
        int initialDutchTension = dutch.getTension(french).getValue();
        int initialFrenchTension = french.getTension(dutch).getValue();

        errMsg ="The Dutch must be at peace with the French";
        assertEquals(errMsg,Stance.PEACE,dutch.getStance(french));
        errMsg ="The French must be at peace with the Dutch";
        assertEquals(errMsg,Stance.PEACE,french.getStance(dutch));
        errMsg = "Wrong initial dutch tension";
        assertEquals(errMsg, initialTensionValue, initialDutchTension);
        errMsg = "Wrong initial french tension";
        assertEquals(errMsg, initialTensionValue, initialFrenchTension);

        // French declare war
        igc.sendChangeStance(french, Stance.WAR, dutch);

        // verify results
        errMsg ="The Dutch should be at war with the French";
        assertTrue(errMsg,dutch.getStance(french) == Stance.WAR);
        errMsg ="The French should be at war with the Dutch";
        assertTrue(errMsg,french.getStance(dutch) == Stance.WAR);

        int currDutchTension = dutch.getTension(french).getValue();
        int currFrenchTension = french.getTension(dutch).getValue();

        int expectedDutchTension = Math.min(Tension.TENSION_MAX,
            initialDutchTension + Tension.WAR_MODIFIER);
        int expectedFrenchTension = Math.min(Tension.TENSION_MAX,
            initialFrenchTension + Tension.WAR_MODIFIER);

        errMsg = "Wrong dutch tension";
        assertEquals(errMsg, expectedDutchTension, currDutchTension);
        errMsg = "Wrong french tension";
        assertEquals(errMsg, expectedFrenchTension, currFrenchTension);
    }

    public void testCreateMissionFailed() {
        Game game = start(getTestMap());
        Map map = game.getMap();

        FreeColTestCase.IndianSettlementBuilder builder = new FreeColTestCase.IndianSettlementBuilder(game);
        IndianSettlement camp = builder.build();

        Player dutchPlayer = game.getPlayer("model.nation.dutch");
        Player indianPlayer = camp.getOwner();

        Tile tile = map.getNeighbourOrNull(Map.Direction.N, camp.getTile());

        Unit jesuit = new Unit(game, tile, dutchPlayer, missionaryType, UnitState.ACTIVE);

        // set tension to hateful
        Tension tension = new Tension(Level.HATEFUL.getLimit());
        camp.setAlarm(dutchPlayer, tension);
        assertEquals("Wrong camp alarm", tension, camp.getAlarm(dutchPlayer));

        InGameController igc = (InGameController) server.getController();
        igc.establishMission((ServerPlayer) dutchPlayer, jesuit, camp);
        boolean result = !jesuit.isDisposed();
        assertFalse("Mission creation should have failed",result);
        assertNull("Indian settlement should not have a mission",camp.getMissionary());
    }

    public void testDumpGoods() {
        final TileType ocean = spec().getTileType("model.tile.ocean");
        final GoodsType cottonType = spec().getGoodsType("model.goods.cotton");
        final UnitType privateerType = spec().getUnitType("model.unit.privateer");
        Game game = start(getTestMap(ocean));
        Map map = game.getMap();

        Player dutchPlayer = game.getPlayer("model.nation.dutch");
        Tile tile = map.getTile(1, 1);
        Unit privateer = new Unit(game, tile, dutchPlayer, privateerType, UnitState.ACTIVE);
        assertEquals("Setup Error, privateer should not carry anything", 0, privateer.getGoodsCount());
        Goods cotton = new Goods(game,privateer,cottonType,100);
        privateer.add(cotton);
        assertEquals("Setup Error, privateer should carry cotton", 1, privateer.getGoodsCount());
        assertTrue("Setup Error, cotton should be aboard privateer",cotton.getLocation() == privateer);

        InGameController controller = (InGameController) server.getController();
        // moving a good to a null location means dumping the good
        controller.moveGoods(cotton, null);

        assertEquals("Privateer should no longer carry cotton", 0, privateer.getGoodsCount());
        assertNull("Cotton should have no location",cotton.getLocation());
    }

    /*
     * Tests worker allocation regarding building tasks
     */
    public void testCashInTreasure() {
        Game game = start(getCoastTestMap(plains, true));
        Map map = game.getMap();

        ServerPlayer dutch = (ServerPlayer) game.getPlayer("model.nation.dutch");
        Tile tile = map.getTile(10, 4);
        Unit ship = new Unit(game, tile, dutch, galleonType, UnitState.ACTIVE, galleonType.getDefaultEquipment());
        Unit treasure = new Unit(game, tile, dutch, treasureTrainType, UnitState.ACTIVE, treasureTrainType.getDefaultEquipment());
        assertTrue(treasure.canCarryTreasure());
        treasure.setTreasureAmount(100);

        InGameController igc = (InGameController) server.getController();
        assertFalse(treasure.canCashInTreasureTrain()); // from a tile
        treasure.setLocation(ship);
        assertFalse(treasure.canCashInTreasureTrain()); // from a ship
        ship.setLocation(dutch.getEurope());
        assertTrue(treasure.canCashInTreasureTrain()); // from a ship in Europe
        int fee = treasure.getTransportFee();
        assertEquals(0, fee);
        int oldGold = dutch.getGold();
        igc.cashInTreasureTrain(dutch, treasure);
        assertEquals(100, dutch.getGold() - oldGold);

        // Succeed from a port with a connection to Europe
        Colony port = getStandardColony(1, 9, 4);
        assertFalse(port.isLandLocked());
        assertTrue(port.isConnected());
        treasure.setLocation(port);
        assertTrue(treasure.canCashInTreasureTrain());

        // Fail from a landlocked colony
        Colony inland = getStandardColony(1, 7, 7);
        assertTrue(inland.isLandLocked());
        assertFalse(inland.isConnected());
        treasure.setLocation(inland);
        assertFalse(treasure.canCashInTreasureTrain());

        // Fail from a colony with a port but no connection to Europe
        map.getTile(5, 5).setType(FreeCol.getSpecification().getTileType("model.tile.lake"));
        Colony lake = getStandardColony(1, 4, 5);
        assertFalse(lake.isLandLocked());
        assertFalse(lake.isConnected());
        treasure.setLocation(lake);
        assertFalse(treasure.canCashInTreasureTrain());
    }

    public void testEmbark() {
        Game game = start(getCoastTestMap(plains));
        Map map = game.getMap();

        //Game game = getStandardGame();
        //Map map = getTestMap();
        //Tile tile = map.getTile(6, 8);
        //game.setMap(map);

        InGameController igc = (InGameController) server.getController();
        Tile landTile = map.getTile(9, 9);
        Tile seaTile = map.getTile(10, 9);
        ServerPlayer dutch = (ServerPlayer) game.getPlayer("model.nation.dutch");
        Unit colonist = new Unit(game, landTile, dutch, colonistType, UnitState.ACTIVE);
        Unit galleon = new Unit(game, seaTile, dutch, galleonType, UnitState.ACTIVE);
        Unit caravel = new Unit(game, seaTile, dutch, caravelType, UnitState.ACTIVE);
        caravel.getType().setSpaceTaken(2);
        Unit wagon = new Unit(game, landTile, dutch, wagonTrainType, UnitState.ACTIVE);

        // can not put ship on carrier
        igc.embarkUnit(dutch, caravel, galleon);
        assertTrue("caravel can not be put on galleon",
                   caravel.getLocation() == seaTile);

        // can not put wagon on galleon at its normal size
        wagon.getType().setSpaceTaken(12);
        igc.embarkUnit(dutch, wagon, galleon);
        assertTrue("large wagon can not be put on galleon",
                   wagon.getLocation() == landTile);

        // but we can if it is made smaller
        wagon.getType().setSpaceTaken(2);
        igc.embarkUnit(dutch, wagon, galleon);
        assertTrue("wagon should now fit on galleon",
                   wagon.getLocation() == galleon);
        assertEquals(UnitState.SENTRY, wagon.getState());

        // can put colonist on carrier
        igc.embarkUnit(dutch, colonist, caravel);
        assertTrue("colonist should embark on caravel",
                   colonist.getLocation() == caravel);
        assertEquals(UnitState.SENTRY, colonist.getState());
    }

    public void testClearSpecialty() {
        Game game = start(getTestMap());
        Map map = game.getMap();

        ServerPlayer dutch = (ServerPlayer) game.getPlayer("model.nation.dutch");
        Unit unit = new Unit(game, map.getTile(5, 8), dutch, hardyPioneerType,
                             UnitState.ACTIVE);
        assertTrue("Unit should be a hardy pioneer",
                   unit.getType() == hardyPioneerType);
        InGameController igc = (InGameController) server.getController();

        // Basic function
        igc.clearSpeciality(dutch, unit);
        assertFalse("Unit was not cleared of its specialty",
                    unit.getType() == hardyPioneerType);

        // Can not clear speciality while teaching
        Colony colony = getStandardColony();
        Building school = new Building(game, colony, schoolHouseType);
        colony.addBuilding(school);
        Unit teacher = new Unit(game, school, colony.getOwner(),
                                hardyPioneerType, UnitState.ACTIVE);
        assertTrue("Unit should be a hardy pioneer",
                   teacher.getType() == hardyPioneerType);
        igc.clearSpeciality(dutch, teacher);
        assertTrue("Teacher specialty cannot be cleared, should still be hardy pioneer",
                   teacher.getType() == hardyPioneerType);
    }
}
