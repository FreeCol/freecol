package net.sf.freecol.common.model;

import net.sf.freecol.util.test.FreeColTestCase;

public class UnitTest extends FreeColTestCase {

    /**
     * Test Plowing with a hardy pioneer
     *
     */
    public void testDoAssignedWorkHardyPioneerPlowPlain() {

        Game game = getStandardGame();

        Player dutch = game.getPlayer(Player.DUTCH);

        Tile plain = new Tile(game, Tile.PLAINS, 5, 8);
        
        Unit hardyPioneer = new Unit(game, plain, dutch, Unit.HARDY_PIONEER, Unit.ACTIVE, false, false, 100,
                false);

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
    
    /**
     * Test Building a road with a hardy pioneer.
     * 
     * The road is available directly, but the pioneer can only move on the next turn.
     *
     */
    public void testDoAssignedWorkHardyPioneerBuildRoad() {

        Game game = getStandardGame();

        Player dutch = game.getPlayer(Player.DUTCH);

        Tile plain = new Tile(game, Tile.PLAINS, 5, 8);
        
        Unit hardyPioneer = new Unit(game, plain, dutch, Unit.HARDY_PIONEER, Unit.ACTIVE, false, false, 100,
                false);

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

    public static int getWorkLeftForPioneerWork(int unitType, int tileType, boolean forrest, int whichWork){
        
        Game game = getStandardGame();

        Player dutch = game.getPlayer(Player.DUTCH);

            Tile tile = new Tile(game, tileType, 0, 0);
            
           tile.setForested(forrest);
               
           Unit unit = new Unit(game, tile, dutch, unitType, Unit.ACTIVE, false, false, 100,
                        false);
        unit.setState(whichWork);
        
        return unit.getWorkLeft();
    }
         
    /**
     * Check for basic time requirements...
     *
     */
    public void testDoAssignedWorkAmateurAndHardyPioneer() {

        { // Savanna

            assertEquals(7, getWorkLeftForPioneerWork(Unit.FREE_COLONIST, Tile.SAVANNAH, true, Unit.PLOW));
            assertEquals(5, getWorkLeftForPioneerWork(Unit.FREE_COLONIST, Tile.SAVANNAH, true, Unit.BUILD_ROAD));
            assertEquals(4, getWorkLeftForPioneerWork(Unit.FREE_COLONIST, Tile.SAVANNAH, false, Unit.PLOW));
            assertEquals(2, getWorkLeftForPioneerWork(Unit.FREE_COLONIST, Tile.SAVANNAH, false, Unit.BUILD_ROAD));
            
            assertEquals(3, getWorkLeftForPioneerWork(Unit.HARDY_PIONEER, Tile.SAVANNAH, true, Unit.PLOW));
            assertEquals(2, getWorkLeftForPioneerWork(Unit.HARDY_PIONEER, Tile.SAVANNAH, true, Unit.BUILD_ROAD));
            assertEquals(1, getWorkLeftForPioneerWork(Unit.HARDY_PIONEER, Tile.SAVANNAH, false, Unit.PLOW));
            assertEquals(0, getWorkLeftForPioneerWork(Unit.HARDY_PIONEER, Tile.SAVANNAH, false, Unit.BUILD_ROAD));
        }

        { // Tundra

            assertEquals(5, getWorkLeftForPioneerWork(Unit.FREE_COLONIST, Tile.TUNDRA, true, Unit.PLOW));
            // assertEquals(???, getWorkLeftForPioneerWork(Unit.FREE_COLONIST, Tile.TUNDRA, true, Unit.BUILD_ROAD));
            assertEquals(5, getWorkLeftForPioneerWork(Unit.FREE_COLONIST, Tile.TUNDRA, false, Unit.PLOW));
            assertEquals(3, getWorkLeftForPioneerWork(Unit.FREE_COLONIST, Tile.TUNDRA, false, Unit.BUILD_ROAD));
            
            assertEquals(2, getWorkLeftForPioneerWork(Unit.HARDY_PIONEER, Tile.TUNDRA, true, Unit.PLOW));
            // assertEquals(???, getWorkLeftForPioneerWork(Unit.HARDY_PIONEER, Tile.TUNDRA, true, Unit.BUILD_ROAD));
            assertEquals(2, getWorkLeftForPioneerWork(Unit.HARDY_PIONEER, Tile.TUNDRA, false, Unit.PLOW));
            assertEquals(1, getWorkLeftForPioneerWork(Unit.HARDY_PIONEER, Tile.TUNDRA, false, Unit.BUILD_ROAD));
        }
        
        { // Plain

            assertEquals(5, getWorkLeftForPioneerWork(Unit.FREE_COLONIST, Tile.TUNDRA, true, Unit.PLOW));
            // assertEquals(???, getWorkLeftForPioneerWork(Unit.FREE_COLONIST, Tile.TUNDRA, true, Unit.BUILD_ROAD));
            assertEquals(4, getWorkLeftForPioneerWork(Unit.FREE_COLONIST, Tile.TUNDRA, false, Unit.PLOW));
            assertEquals(2, getWorkLeftForPioneerWork(Unit.FREE_COLONIST, Tile.TUNDRA, false, Unit.BUILD_ROAD));
            
            assertEquals(2, getWorkLeftForPioneerWork(Unit.HARDY_PIONEER, Tile.TUNDRA, true, Unit.PLOW));
            // assertEquals(???, getWorkLeftForPioneerWork(Unit.HARDY_PIONEER, Tile.TUNDRA, true, Unit.BUILD_ROAD));
            assertEquals(1, getWorkLeftForPioneerWork(Unit.HARDY_PIONEER, Tile.TUNDRA, false, Unit.PLOW));
            assertEquals(0, getWorkLeftForPioneerWork(Unit.HARDY_PIONEER, Tile.TUNDRA, false, Unit.BUILD_ROAD));
        }
    }
}
