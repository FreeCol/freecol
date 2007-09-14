package net.sf.freecol.common.model;

import java.util.HashMap;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.Specification;
import net.sf.freecol.util.test.FreeColTestCase;

public class TileTest extends FreeColTestCase {

    public static final String COPYRIGHT = "Copyright (C) 2003-2007 The FreeCol Team";

    public static final String LICENSE = "http://www.gnu.org/licenses/gpl.html";

    public static final String REVISION = "$Revision$";

    public void testGetWorkAmount() {

        Game game = getStandardGame();
        
        Specification s = FreeCol.getSpecification();

        TileType plains = s.getTileType("model.tile.plains");
        TileType desert = s.getTileType("model.tile.desert");
        TileType grassland = s.getTileType("model.tile.grassland");
        TileType prairie = s.getTileType("model.tile.prairie");
        TileType tundra = s.getTileType("model.tile.tundra");
        TileType savannah = s.getTileType("model.tile.savannah");
        TileType marsh = s.getTileType("model.tile.marsh");
        TileType swamp = s.getTileType("model.tile.swamp");
        TileType arctic = s.getTileType("model.tile.arctic");
        
        TileType plainsForest = s.getTileType("model.tile.mixedForest");
        TileType desertForest = s.getTileType("model.tile.scrubForest");
        TileType grasslandForest = s.getTileType("model.tile.coniferForest");
        TileType prairieForest = s.getTileType("model.tile.broadleafForest");
        TileType tundraForest = s.getTileType("model.tile.borealForest");
        TileType savannahForest = s.getTileType("model.tile.tropicalForest");
        TileType marshForest = s.getTileType("model.tile.wetlandForest");
        TileType swampForest = s.getTileType("model.tile.rainForest");

        assertNotNull( plains );
        assertNotNull( desert );
        assertNotNull( grassland );
        assertNotNull( prairie );
        assertNotNull( tundra );
        assertNotNull( savannah );
        assertNotNull( marsh );
        assertNotNull( swamp );
        assertNotNull( arctic );
        
        assertNotNull( plainsForest );
        assertNotNull( desertForest );
        assertNotNull( grasslandForest );
        assertNotNull( prairieForest );
        assertNotNull( tundraForest );
        assertNotNull( savannahForest );
        assertNotNull( marshForest );
        assertNotNull( swampForest );
        
        TileImprovementType plow = s.getTileImprovementType("model.improvement.Plow");
        TileImprovementType buildRoad = s.getTileImprovementType("model.improvement.Road");
        TileImprovementType clearForrest = s.getTileImprovementType("model.improvement.ClearForest");
        
        assertEquals(2, plow.getAddWorkTurns());
        assertEquals(0, buildRoad.getAddWorkTurns());
        assertEquals(2, clearForrest.getAddWorkTurns());
        
        
        assertNotNull(plow);
        assertNotNull(buildRoad);
        assertNotNull(clearForrest);
        
        java.util.Map<TileType, int[]> cost = new HashMap<TileType, int[]>();
        cost.put(plains, new int[] { 5, 3 });
        cost.put(desert, new int[] { 5, 3 });
        cost.put(grassland, new int[] { 5, 3 });
        cost.put(prairie, new int[] { 5, 3 });
        cost.put(tundra, new int[] { 6, 4 });
        cost.put(savannah, new int[] { 5, 3 });
        cost.put(marsh, new int[] { 7, 5 });
        cost.put(swamp, new int[] { 9, 7 });
        cost.put(arctic, new int[] { 6, 4 });
        
        for (java.util.Map.Entry<TileType, int[]> entry : cost.entrySet()){
            Tile tile = new Tile(game, entry.getKey(), 0, 0);
            assertTrue(tile.getType().getName(), plow.isTileAllowed(tile));
            assertTrue(tile.getType().getName(), buildRoad.isTileAllowed(tile));
            assertFalse(tile.getType().getName(), clearForrest.isTileAllowed(tile));
            
            assertEquals(tile.getType().getName(), entry.getValue()[0], tile.getWorkAmount(plow));
            assertEquals(tile.getType().getName(), entry.getValue()[1], tile.getWorkAmount(buildRoad));
        }
        
        // Now check the forests
        cost.clear();
        cost.put(tundraForest, new int[] { 6, 4 });
        cost.put(grasslandForest, new int[] { 6, 4 });
        cost.put(desertForest, new int[] { 6, 4});
        cost.put(prairieForest, new int[] { 6, 4 });
        cost.put(savannahForest, new int[] { 8, 6 });
        cost.put(marshForest, new int[] { 8, 6 });
        cost.put(swampForest, new int[] { 9, 7});
        cost.put(plainsForest, new int[] { 6, 4});
        
        for (java.util.Map.Entry<TileType, int[]> entry : cost.entrySet()){
            Tile tile = new Tile(game, entry.getKey(), 0, 0);
            assertFalse(tile.getType().getName(), plow.isTileAllowed(tile));
            assertTrue(tile.getType().getName(), buildRoad.isTileAllowed(tile));
            assertTrue(tile.getType().getName(), clearForrest.isTileAllowed(tile));
            
            assertEquals(tile.getType().getName(), entry.getValue()[0], tile.getWorkAmount(clearForrest));
            assertEquals(tile.getType().getName(), entry.getValue()[1], tile.getWorkAmount(buildRoad));
        }
        
    }
    
    public void testAdditions(){
        
        Game game = getStandardGame();
        
        Tile tile = new Tile(game, Tile.SAVANNAH, 0, 0);
        
        // 0.) Tiles start with none
        assertEquals(Tile.ADD_NONE, tile.getAddition());
        
        // 1.) Let's add forrest 
        tile.setForested(true);
        assertEquals(true, tile.isForested());
        
        // 2.) If we now add a hill, the forest is gone
        tile.setAddition(Tile.ADD_HILLS);
        assertEquals(false, tile.isForested());
        assertEquals(Tile.ADD_HILLS, tile.getAddition());
        
        // 3.) If we now set forrest again to true, the HILLS disappear
        tile.setForested(true);
        assertEquals(true, tile.isForested());
        assertEquals(Tile.ADD_NONE, tile.getAddition());
        
        // 4.) If we now add a MOUNTAIN (same as 2.), the forst is gone
        tile.setAddition(Tile.ADD_MOUNTAINS);
        assertEquals(false, tile.isForested());
        assertEquals(Tile.ADD_MOUNTAINS, tile.getAddition());
        
        // 5.) If we now set forrest again to true, the MOUNTAINS disappear
        tile.setForested(true);
        assertEquals(true, tile.isForested());
        assertEquals(Tile.ADD_NONE, tile.getAddition());
        
        // 6.) If we add a MOUNTAIN and then setForested to false the mountains should stay
        tile.setAddition(Tile.ADD_MOUNTAINS);
        tile.setForested(false);
        assertEquals(false, tile.isForested());
        assertEquals(Tile.ADD_MOUNTAINS, tile.getAddition());
        
        // 7.) Same for HILLS
        tile.setAddition(Tile.ADD_HILLS);
        tile.setForested(false);
        assertEquals(false, tile.isForested());
        assertEquals(Tile.ADD_HILLS, tile.getAddition());
   
    }
}
