package net.sf.freecol.common.model;

import net.sf.freecol.util.test.FreeColTestCase;

public class UnitTest extends FreeColTestCase {

    public static final String COPYRIGHT = "Copyright (C) 2003-2007 The FreeCol Team";

    public static final String LICENSE = "http://www.gnu.org/licenses/gpl.html";

    public static final String REVISION = "$Revision$";


    /**
     * Test Plowing with a hardy pioneer
     * 
     */
    public void testDoAssignedWorkHardyPioneerPlowPlain() {

        Game game = getStandardGame();
        Player dutch = game.getPlayer(Player.DUTCH);
        Map map = getTestMap(Tile.PLAINS);
        game.setMap(map);
        Tile plain = map.getTile(5, 8);
        map.getTile(5, 8).setExploredBy(dutch, true);

        Unit hardyPioneer = new Unit(game, plain, dutch, Unit.HARDY_PIONEER, Unit.ACTIVE, false, false, 100, false);

        // Before
        assertEquals(3, hardyPioneer.getMovesLeft());
        assertEquals(Unit.ACTIVE, hardyPioneer.getState());
        assertEquals(-1, hardyPioneer.getWorkLeft());
        assertEquals(100, hardyPioneer.getNumberOfTools());
        assertEquals(false, plain.isPlowed());

        hardyPioneer.setState(Unit.PLOW);

        assertEquals(0, hardyPioneer.getMovesLeft());
        assertEquals(1, hardyPioneer.getWorkLeft());
        assertEquals(100, hardyPioneer.getNumberOfTools());
        assertEquals(false, plain.isPlowed());

        // Advance 1 turn
        game.newTurn();

        // Pioneer finished work but can only move on next turn
        assertEquals(0, hardyPioneer.getMovesLeft());
        assertEquals(Unit.ACTIVE, hardyPioneer.getState());
        assertEquals(-1, hardyPioneer.getWorkLeft());
        assertEquals(80, hardyPioneer.getNumberOfTools());
        assertEquals(true, plain.isPlowed());

        // Advance last turn
        game.newTurn();

        assertEquals(3, hardyPioneer.getMovesLeft());
        assertEquals(Unit.ACTIVE, hardyPioneer.getState());
        assertEquals(-1, hardyPioneer.getWorkLeft());
        assertEquals(80, hardyPioneer.getNumberOfTools());
        assertEquals(true, plain.isPlowed());
    }

    public void testColonyProfitFromEnhancement() {

        Game game = getStandardGame();
        Player dutch = game.getPlayer(Player.DUTCH);
        Map map = getTestMap(Tile.PLAINS);
        game.setMap(map);
        map.getTile(5, 8).setExploredBy(dutch, true);
        map.getTile(6, 8).setExploredBy(dutch, true);
        Tile plain58 = map.getTile(5, 8);

        // Found colony on 6,8
        Unit soldier = new Unit(game, map.getTile(6, 8), dutch, Unit.VETERAN_SOLDIER, Unit.ACTIVE, true, false, 0,
                false);

        Colony colony = new Colony(game, dutch, "New Amsterdam", soldier.getTile());
        soldier.setWorkType(Goods.FOOD);
        soldier.buildColony(colony);

        soldier.setLocation(colony.getColonyTile(plain58));

        Unit hardyPioneer = new Unit(game, plain58, dutch, Unit.HARDY_PIONEER, Unit.ACTIVE, false, false, 100, false);

        // Before
        assertEquals(0, colony.getGoodsCount(Goods.FOOD));
        assertEquals(2, colony.getFoodConsumption());
        assertEquals(5 + 5, colony.getFoodProduction());
        assertEquals(false, plain58.isPlowed());
        assertEquals("" + soldier.getLocation(), colony.getColonyTile(map.getTile(5, 8)), soldier.getLocation());

        // One turn to check production
        game.newTurn();

        assertEquals(false, plain58.isPlowed());
        assertEquals(8, colony.getGoodsCount(Goods.FOOD));
        assertEquals(2, colony.getFoodConsumption());
        assertEquals(5 + 5, colony.getFoodProduction());

        // Start Plowing
        hardyPioneer.setState(Unit.PLOW);

        game.newTurn();

        assertEquals(true, plain58.isPlowed());
        // Production for next turn is updated
        assertEquals(5 + 6, colony.getFoodProduction());
        // But in only 10 - 2 == 8 are added from last turn
        assertEquals(8 + 8, colony.getGoodsCount(Goods.FOOD));
        assertEquals(2, colony.getFoodConsumption());

        // Advance last turn
        game.newTurn();

        assertEquals(16 + 9, colony.getGoodsCount(Goods.FOOD));
        assertEquals(2, colony.getFoodConsumption());
        assertEquals(5 + 6, colony.getFoodProduction());
        assertEquals(true, plain58.isPlowed());
    }

    /**
     * Test Building a road with a hardy pioneer.
     * 
     * The road is available directly, but the pioneer can only move on the next
     * turn.
     * 
     */
    public void testDoAssignedWorkHardyPioneerBuildRoad() {

        Game game = getStandardGame();
        Player dutch = game.getPlayer(Player.DUTCH);
        Map map = getTestMap(Tile.PLAINS);
        game.setMap(map);
        Tile plain = map.getTile(5, 8);
        map.getTile(5, 8).setExploredBy(dutch, true);

        Unit hardyPioneer = new Unit(game, plain, dutch, Unit.HARDY_PIONEER, Unit.ACTIVE, false, false, 100, false);

        // Before
        assertEquals(3, hardyPioneer.getMovesLeft());
        assertEquals(-1, hardyPioneer.getWorkLeft());
        assertEquals(100, hardyPioneer.getNumberOfTools());
        assertEquals(false, plain.hasRoad());
        assertEquals(Unit.ACTIVE, hardyPioneer.getState());

        // Now do it
        hardyPioneer.setState(Unit.BUILD_ROAD);

        // After
        assertEquals(0, hardyPioneer.getMovesLeft());
        assertEquals(-1, hardyPioneer.getWorkLeft());
        assertEquals(80, hardyPioneer.getNumberOfTools());
        assertEquals(true, plain.hasRoad());
        assertEquals(Unit.ACTIVE, hardyPioneer.getState());

        // Advance 1 turn
        game.newTurn();

        assertEquals(3, hardyPioneer.getMovesLeft());
        assertEquals(Unit.ACTIVE, hardyPioneer.getState());
        assertEquals(-1, hardyPioneer.getWorkLeft());
        assertEquals(80, hardyPioneer.getNumberOfTools());
    }

    public static int getWorkLeftForPioneerWork(int unitType, int tileType, boolean forrest, int whichWork, int addition) {

        Game game = getStandardGame();

        Player dutch = game.getPlayer(Player.DUTCH);

        Tile tile = new Tile(game, tileType, 0, 0);
        tile.setAddition(addition);
        tile.setForested(forrest);

        Unit unit = new Unit(game, tile, dutch, unitType, Unit.ACTIVE, false, false, 100, false);
        unit.setState(whichWork);

        return unit.getWorkLeft();
    }

    /**
     * Check for basic time requirements...
     * 
     */
    public void testDoAssignedWorkAmateurAndHardyPioneer() {
        
        { // Savanna
            assertEquals(7,
                    getWorkLeftForPioneerWork(Unit.FREE_COLONIST, Tile.SAVANNAH, true, Unit.PLOW, Tile.ADD_NONE));
            assertEquals(5, getWorkLeftForPioneerWork(Unit.FREE_COLONIST, Tile.SAVANNAH, true, Unit.BUILD_ROAD,
                    Tile.ADD_NONE));
            assertEquals(4, getWorkLeftForPioneerWork(Unit.FREE_COLONIST, Tile.SAVANNAH, false, Unit.PLOW,
                    Tile.ADD_NONE));
            assertEquals(2, getWorkLeftForPioneerWork(Unit.FREE_COLONIST, Tile.SAVANNAH, false, Unit.BUILD_ROAD,
                    Tile.ADD_NONE));

            assertEquals(3,
                    getWorkLeftForPioneerWork(Unit.HARDY_PIONEER, Tile.SAVANNAH, true, Unit.PLOW, Tile.ADD_NONE));
            assertEquals(2, getWorkLeftForPioneerWork(Unit.HARDY_PIONEER, Tile.SAVANNAH, true, Unit.BUILD_ROAD,
                    Tile.ADD_NONE));
            assertEquals(1, getWorkLeftForPioneerWork(Unit.HARDY_PIONEER, Tile.SAVANNAH, false, Unit.PLOW,
                    Tile.ADD_NONE));
            assertEquals(-1, getWorkLeftForPioneerWork(Unit.HARDY_PIONEER, Tile.SAVANNAH, false, Unit.BUILD_ROAD,
                    Tile.ADD_NONE));
        }

        { // Tundra
            assertEquals(5, getWorkLeftForPioneerWork(Unit.FREE_COLONIST, Tile.TUNDRA, true, Unit.PLOW, Tile.ADD_NONE));
            assertEquals(3, getWorkLeftForPioneerWork(Unit.FREE_COLONIST, Tile.TUNDRA, true, Unit.BUILD_ROAD,
                    Tile.ADD_NONE));
            assertEquals(5, getWorkLeftForPioneerWork(Unit.FREE_COLONIST, Tile.TUNDRA, false, Unit.PLOW, Tile.ADD_NONE));
            assertEquals(3, getWorkLeftForPioneerWork(Unit.FREE_COLONIST, Tile.TUNDRA, false, Unit.BUILD_ROAD,
                    Tile.ADD_NONE));

            assertEquals(2, getWorkLeftForPioneerWork(Unit.HARDY_PIONEER, Tile.TUNDRA, true, Unit.PLOW, Tile.ADD_NONE));
            assertEquals(1, getWorkLeftForPioneerWork(Unit.HARDY_PIONEER, Tile.TUNDRA, true, Unit.BUILD_ROAD,
                    Tile.ADD_NONE));
            assertEquals(2, getWorkLeftForPioneerWork(Unit.HARDY_PIONEER, Tile.TUNDRA, false, Unit.PLOW, Tile.ADD_NONE));
            assertEquals(1, getWorkLeftForPioneerWork(Unit.HARDY_PIONEER, Tile.TUNDRA, false, Unit.BUILD_ROAD,
                    Tile.ADD_NONE));
        }

        { // Plains
            assertEquals(5, getWorkLeftForPioneerWork(Unit.FREE_COLONIST, Tile.PLAINS, true, Unit.PLOW, Tile.ADD_NONE));
            assertEquals(3, getWorkLeftForPioneerWork(Unit.FREE_COLONIST, Tile.PLAINS, true, Unit.BUILD_ROAD,
                    Tile.ADD_NONE));
            assertEquals(4, getWorkLeftForPioneerWork(Unit.FREE_COLONIST, Tile.PLAINS, false, Unit.PLOW, Tile.ADD_NONE));
            assertEquals(2, getWorkLeftForPioneerWork(Unit.FREE_COLONIST, Tile.PLAINS, false, Unit.BUILD_ROAD,
                    Tile.ADD_NONE));

            assertEquals(2, getWorkLeftForPioneerWork(Unit.HARDY_PIONEER, Tile.PLAINS, true, Unit.PLOW, Tile.ADD_NONE));
            assertEquals(1, getWorkLeftForPioneerWork(Unit.HARDY_PIONEER, Tile.PLAINS, true, Unit.BUILD_ROAD,
                    Tile.ADD_NONE));
            assertEquals(1, getWorkLeftForPioneerWork(Unit.HARDY_PIONEER, Tile.PLAINS, false, Unit.PLOW, Tile.ADD_NONE));
            assertEquals(-1, getWorkLeftForPioneerWork(Unit.HARDY_PIONEER, Tile.PLAINS, false, Unit.BUILD_ROAD,
                    Tile.ADD_NONE));
        }

        { // Hill
            assertEquals(3, getWorkLeftForPioneerWork(Unit.FREE_COLONIST, Tile.TUNDRA, true, Unit.BUILD_ROAD,
                    Tile.ADD_HILLS));
            assertEquals(1, getWorkLeftForPioneerWork(Unit.HARDY_PIONEER, Tile.TUNDRA, true, Unit.BUILD_ROAD,
                    Tile.ADD_HILLS));
        }

        { // Mountain
            assertEquals(6, getWorkLeftForPioneerWork(Unit.FREE_COLONIST, Tile.TUNDRA, true, Unit.BUILD_ROAD,
                    Tile.ADD_MOUNTAINS));
            assertEquals(2, getWorkLeftForPioneerWork(Unit.HARDY_PIONEER, Tile.TUNDRA, true, Unit.BUILD_ROAD,
                    Tile.ADD_MOUNTAINS));
        }

        { // Marsh
            assertEquals(7, getWorkLeftForPioneerWork(Unit.FREE_COLONIST, Tile.MARSH, true, Unit.PLOW, Tile.ADD_NONE));
            assertEquals(5, getWorkLeftForPioneerWork(Unit.FREE_COLONIST, Tile.MARSH, true, Unit.BUILD_ROAD,
                    Tile.ADD_NONE));
            assertEquals(6, getWorkLeftForPioneerWork(Unit.FREE_COLONIST, Tile.MARSH, false, Unit.PLOW, Tile.ADD_NONE));
            assertEquals(4, getWorkLeftForPioneerWork(Unit.FREE_COLONIST, Tile.MARSH, false, Unit.BUILD_ROAD,
                    Tile.ADD_NONE));

            assertEquals(3, getWorkLeftForPioneerWork(Unit.HARDY_PIONEER, Tile.MARSH, true, Unit.PLOW, Tile.ADD_NONE));
            assertEquals(2, getWorkLeftForPioneerWork(Unit.HARDY_PIONEER, Tile.MARSH, true, Unit.BUILD_ROAD,
                    Tile.ADD_NONE));
            assertEquals(2, getWorkLeftForPioneerWork(Unit.HARDY_PIONEER, Tile.MARSH, false, Unit.PLOW, Tile.ADD_NONE));
            assertEquals(1, getWorkLeftForPioneerWork(Unit.HARDY_PIONEER, Tile.MARSH, false, Unit.BUILD_ROAD,
                    Tile.ADD_NONE));
        }

        { // Desert
            assertEquals(5, getWorkLeftForPioneerWork(Unit.FREE_COLONIST, Tile.DESERT, true, Unit.PLOW, Tile.ADD_NONE));
            assertEquals(3, getWorkLeftForPioneerWork(Unit.FREE_COLONIST, Tile.DESERT, true, Unit.BUILD_ROAD,
                    Tile.ADD_NONE));
            assertEquals(4, getWorkLeftForPioneerWork(Unit.FREE_COLONIST, Tile.DESERT, false, Unit.PLOW, Tile.ADD_NONE));
            assertEquals(2, getWorkLeftForPioneerWork(Unit.FREE_COLONIST, Tile.DESERT, false, Unit.BUILD_ROAD,
                    Tile.ADD_NONE));

            assertEquals(2, getWorkLeftForPioneerWork(Unit.HARDY_PIONEER, Tile.DESERT, true, Unit.PLOW, Tile.ADD_NONE));
            assertEquals(1, getWorkLeftForPioneerWork(Unit.HARDY_PIONEER, Tile.DESERT, true, Unit.BUILD_ROAD,
                    Tile.ADD_NONE));
            assertEquals(1, getWorkLeftForPioneerWork(Unit.HARDY_PIONEER, Tile.DESERT, false, Unit.PLOW, Tile.ADD_NONE));
            assertEquals(-1, getWorkLeftForPioneerWork(Unit.HARDY_PIONEER, Tile.DESERT, false, Unit.BUILD_ROAD,
                    Tile.ADD_NONE));
        }

        { // Swamp
            assertEquals(8, getWorkLeftForPioneerWork(Unit.FREE_COLONIST, Tile.SWAMP, true, Unit.PLOW, Tile.ADD_NONE));
            assertEquals(6, getWorkLeftForPioneerWork(Unit.FREE_COLONIST, Tile.SWAMP, true, Unit.BUILD_ROAD,
                    Tile.ADD_NONE));
            assertEquals(8, getWorkLeftForPioneerWork(Unit.FREE_COLONIST, Tile.SWAMP, false, Unit.PLOW, Tile.ADD_NONE));
            assertEquals(6, getWorkLeftForPioneerWork(Unit.FREE_COLONIST, Tile.SWAMP, false, Unit.BUILD_ROAD,
                    Tile.ADD_NONE));

            assertEquals(3, getWorkLeftForPioneerWork(Unit.HARDY_PIONEER, Tile.SWAMP, true, Unit.PLOW, Tile.ADD_NONE));
            assertEquals(2, getWorkLeftForPioneerWork(Unit.HARDY_PIONEER, Tile.SWAMP, true, Unit.BUILD_ROAD,
                    Tile.ADD_NONE));
            assertEquals(3, getWorkLeftForPioneerWork(Unit.HARDY_PIONEER, Tile.SWAMP, false, Unit.PLOW, Tile.ADD_NONE));
            assertEquals(2, getWorkLeftForPioneerWork(Unit.HARDY_PIONEER, Tile.SWAMP, false, Unit.BUILD_ROAD,
                    Tile.ADD_NONE));
        }
    }

    /**
     * Make sure that a colony can only be build by a worker on the same tile as
     * the colony to be build.
     * 
     */
    public void testBuildColonySameTile() {
        Game game = getStandardGame();
        Player dutch = game.getPlayer(Player.DUTCH);
        Map map = getTestMap(Tile.PLAINS, true);
        game.setMap(map);

        Unit soldier = new Unit(game, map.getTile(6, 8), dutch, Unit.VETERAN_SOLDIER, Unit.ACTIVE, true, false, 0,
                false);

        Colony colony = new Colony(game, dutch, "New Amsterdam", map.getTile(6, 9));
        soldier.setWorkType(Goods.FOOD);

        try {
            soldier.buildColony(colony);
            fail();
        } catch (IllegalStateException e) {
        }

        soldier.setLocation(map.getTile(6, 9));
        soldier.buildColony(colony);

        assertEquals(colony, map.getTile(6, 9).getSettlement());
    }
}
