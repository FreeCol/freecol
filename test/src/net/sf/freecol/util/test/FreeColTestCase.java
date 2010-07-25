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

package net.sf.freecol.util.test;

import java.io.FileInputStream;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import junit.framework.TestCase;
import net.sf.freecol.FreeCol;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Nation;
import net.sf.freecol.common.model.NationOptions;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Region;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.server.model.ServerGame;

/**
 * The base class for all FreeCol tests. Contains useful methods used by the
 * individual tests.
 */
public class FreeColTestCase extends TestCase {

    /**
     * use getGame to access this.
     */
    static Game game;

    static boolean updateLocale = true;
    
    @Override
    protected void setUp() throws Exception {
        if (updateLocale) {
            updateLocale = false;
            Messages.setMessageBundle(Locale.US);
        }
    }

    @Override
    protected void tearDown() throws Exception {
    	// If a game has been created destroy it.
    	game = null;
    }

    /**
     * Get a game pseudo-singleton, i.e. the same instance will be returned
     * until getStandardGame() is called, which resets the singleton to a new
     * value.
     * 
     * Calling this method repetitively without calling getStandardGame() will
     * result in the same Game being returned.
     * 
     * @return The game singleton.
     */
    public static Game getGame() {
        if (game == null) {
            game = getStandardGame();
        }
        return game;
    }
    
    /**
     * Specifically sets the game instance to run with
     * Necessary for server tests that create their own game instances
     * Allows for same interface for accessing the game instance for all types of tests
     * @param newGame Game instance to work with
     */
    public static void setGame(Game newGame) {
        game = newGame;
    }
    
    public static Specification spec() {
        Specification result = Specification.getSpecification();
        if (result == null) {
            try {
                Specification.createSpecification(new FileInputStream("data/freecol/specification.xml"));
                return Specification.getSpecification();
            } catch(Exception e) {
                System.out.println(e);
                return null;
            }
        } else {
            return result;
        }

    }

    /**
     * Returns a new game, with all players set.
     * 
     * As a side effect this call will reset the singleton game value that can
     * be accessed using getGame().
     * 
     * @return A new game with with players for each nation added.
     */
    public static Game getStandardGame() {
        game = new ServerGame(new MockModelController());
        game.setNationOptions(NationOptions.getDefaults());

        Specification.getSpecification().applyDifficultyLevel("model.difficulty.medium");
        for (Nation n : Specification.getSpecification().getNations()) {
            Player p;
            if (n.getType().isEuropean() && !n.getType().isREF()){
                p = new Player(game, n.getRulerNameKey(), false, n);
            } else {
                p = new Player(game, n.getRulerNameKey(), false, true, n);
            }
            game.addPlayer(p);
        }
        return game;
    }
    
    /**
     * Creates a standardized map on which all fields have the plains type.
     * 
     * Uses the getGame() method to access the currently running game.
     * 
     * Does not call Game.setMap(Map) with the returned map. The map is unexplored.
     * 
     * @return The map created as described above.
     */
    public static Map getTestMap() {
        MapBuilder builder = new MapBuilder(getGame());
        return builder.build();
    }

    /**
     * Creates a standardized map on which all fields have the same given type.
     * 
     * Uses the getGame() method to access the currently running game.
     * 
     * Does not call Game.setMap(Map) with the returned map. The map is unexplored.
     * 
     * @param type The type of land with which to initialize the map.
     * 
     * @return The map created as described above.
     */
    public static Map getTestMap(TileType tileType) {
        MapBuilder builder = new MapBuilder(getGame());
        builder.setBaseTileType(tileType);
        return builder.build();
    }
    
    /**
     * Creates a standardized map on which all fields have the same given type.
     * 
     * Uses the getGame() method to access the currently running game.
     * 
     * Does not call Game.setMap(Map) with the returned map.
     * 
     * @param type The type of land with which to initialize the map.
     * 
     * @param explored Set to true if you want all the tiles on the map to have been explored by all players.
     * 
     * @return The map created as described above.
     */
    public static Map getTestMap(TileType tileType, boolean explored) {
        MapBuilder builder = new MapBuilder(getGame());
        builder.setBaseTileType(tileType).setExploredByAll(explored);
        return builder.build();
    }
    
    public static Map getTestMap(boolean explored) {
        MapBuilder builder = new MapBuilder(getGame());
        builder.setExploredByAll(explored);
        return builder.build();
    }
    
    public static Map getCoastTestMap(TileType landTileType) {
        return getCoastTestMap(landTileType, false);
    }

    /**
     * Creates a standardized map, half land (left), half sea (right)
     * 
     * The land half has the same given type.
     * 
     * Uses the getGame() method to access the currently running game.
     * 
     * Does not call Game.setMap(Map) with the returned map.
     * 
     * @param type The type of land with which to initialize the map.
     * 
     * @param explored Set to true if you want all the tiles on the map to have been explored by all players.
     * 
     * @return The map created as described above.
     */
    public static Map getCoastTestMap(TileType landTileType, boolean explored) {
        int totalWidth = 20;
        int totalHeight = 15;
        TileType oceanType = spec().getTileType("model.tile.ocean");
        
        MapBuilder builder = new MapBuilder(getGame());
        builder.setDimensions(totalWidth, totalHeight).setBaseTileType(oceanType);
        if (explored) {
            builder.setExploredByAll(true);
        }

        // Fill half with land, the builder will fill the rest with ocean
        int landWidth = (int) Math.floor(totalWidth/2);
        for (int x = 0; x < landWidth; x++) {
            for (int y = 0; y < totalHeight; y++) {
                builder.setTile(x, y, landTileType);
            }
        }

        return builder.build();
    }
    
    /**
     * Get a standard colony at the location 5,8 with one free colonist
     * 
     * @return
     */
    public Colony getStandardColony() {
        return getStandardColony(1, 5, 8);
    }

    /**
     * Get a colony with the given number of settlers
     * 
     * @param numberOfSettlers The number of settlers to put into the colony.
     *            Must be >= 1.
     * 
     * @return
     */
    public Colony getStandardColony(int numberOfSettlers) {
        return getStandardColony(numberOfSettlers, 5, 8);
    }

    /**
     * Get a colony with the given number of settlers
     * 
     * @param numberOfSettlers The number of settlers to put into the colony.
     *            Must be >= 1.
     * @param tileX Coordinate of tile for the colony.
     * @param tileY Coordinate of tile for the colony.
     * 
     * @return
     */
    public Colony getStandardColony(int numberOfSettlers, int tileX, int tileY) {
        Game game = getGame();

        Map map = game.getMap();

        Tile tile = map.getTile(tileX, tileY);

        FreeColTestUtils.ColonyBuilder builder = FreeColTestUtils.getColonyBuilder();
        builder.colonyTile(tile).initialColonists(numberOfSettlers);
        
        return builder.build();
    }
    
    public static class MapBuilder{
        
        // Required parameter
        private final Game game;
        
        private TileType[][] tiles = null; 
        private int width;
        private int height;
        private TileType baseTile;
        private boolean exploredByAll;
        private boolean initiated;
        
        public MapBuilder(Game game){
            this.game = game;
            setStartingParams();
        }
        
        private void setStartingParams(){
            width = 20;
            height = 15;
            baseTile = spec().getTileType("model.tile.plains");
            exploredByAll = false;
            initiated = false;
            // set empty grid
            if(tiles == null){
                tiles = new TileType[width][height];
            }
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    tiles[x][y] = null;
                }
            }
        }
        
        public MapBuilder setBaseTileType(TileType baseType){
            if(baseType == null){
                throw new NullPointerException("Base tile type cannot be null");
            }
            this.baseTile = baseType;
            return this;
        }
        
        public MapBuilder setDimensions(int width, int heigth){
            if(width <= 0){
                throw new IllegalArgumentException("Width must be positive");
            }
            if(heigth <= 0){
                throw new IllegalArgumentException("Heigth must be positive");
            }
            if(initiated){
                throw new IllegalStateException("Cannot resize map after setting a tile");
            }
            this.width = width;
            this.height = heigth;
            return this;
        }
        
        public MapBuilder setExploredByAll(boolean exploredByAll){
            this.exploredByAll = exploredByAll;
            return this;
        }
        
        public MapBuilder setTile(int x, int y, TileType tileType){
            if(x < 0 || y < 0){
                throw new IllegalArgumentException("Coordenates cannot be negative");
            }
            if(x >= width || y >= height ){
                throw new IllegalArgumentException("Coordenate out of bounds");
            }
            if(tileType == null){
                throw new NullPointerException("Tile type cannot be null");
            }
            
            tiles[x][y]= tileType;
            initiated = true;
            
            return this;
        }
        
        // Implementation method, completes grid by setting uninitialized tiles
        //to the base tile type
        private void completeWorkingGrid(){      
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    // Already manually set by the tester
                    if(tiles[x][y] != null){
                        continue;
                    }
                    tiles[x][y] = baseTile;
                }
            }
            initiated=true;
        }
        
        public Map build(){
            completeWorkingGrid();
            Tile[][] mapTiles = new Tile[width][height];
            Region region = new Region(game);

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    TileType tileType = tiles[x][y];
                    mapTiles[x][y] = new Tile(game, tileType, x, y);
                    mapTiles[x][y].setRegion(region);
                }
            }
            
            Map m = new Map(game, mapTiles);
            if (exploredByAll) {
                for (Player player : game.getPlayers()) {
                    for (Tile tile : m.getAllTiles()) {
                        tile.setExploredBy(player, true);
                    }
                }
            }
            return m;
        }
        
        public MapBuilder reset() {
            setStartingParams();
            
            return this;
        }
    }

    public static class IndianSettlementBuilder{
    	
    	// Required parameter
    	private final Game game;
    	
    	private Player indianPlayer;
    	private final String defaultIndianPlayer = "model.nation.tupi";
    	private String skillTaught;
    	private int initialBravesInCamp;
    	private Tile settlementTile;
      private static int settlementNumber = 1;

    	private boolean isCapital;
        private Set<Player> isVisited;
        private Unit residentMissionary;
    	
    	public IndianSettlementBuilder(Game game){
    		this.game = game;
    		setStartingParams();
    	}
    	
    	private void setStartingParams(){
    		// Some params can only be set in build(), because the default values 
    		//may not be valid for the game set
    		// However, the tester himself may set them to valid values later, 
    		//so they are set to null for now
    		indianPlayer = null;
        	initialBravesInCamp = 1;
        	settlementTile = null;
        	skillTaught = "model.unit.masterCottonPlanter";
        	isCapital = false;
            isVisited = new HashSet<Player>();
            residentMissionary = null;
    	}
    	
    	public IndianSettlementBuilder player(Player player){
    		this.indianPlayer = player;
    		
			if(indianPlayer == null || !game.getPlayers().contains(player)){
				throw new IllegalArgumentException("Indian player not in game");
			}
    		
    		return this;
    	}
    	
    	public IndianSettlementBuilder initialBravesInCamp(int nBraves){
    		if(nBraves <= 0){
    			throw new IllegalArgumentException("Number of braves must be positive");
    		}
    		this.initialBravesInCamp = nBraves;
    		return this;
    	}
    	
    	public IndianSettlementBuilder settlementTile(Tile tile){
    		Tile tileOnMap = this.game.getMap().getTile(tile.getPosition());
    		if(tile != tileOnMap){
    			throw new IllegalArgumentException("Given tile not on map");
    		}
    		this.settlementTile = tile;
    		return this;
    	}
    	
    	public IndianSettlementBuilder capital(boolean isCapital){
    		this.isCapital = isCapital;
    		
    		return this;
    	}

    	public IndianSettlementBuilder isVisitedByPlayer(Player player, boolean isVisited){
    		if (player != null) {
                    if (isVisited) {
                        this.isVisited.add(player);
                    } else {
                        this.isVisited.remove(player);
                    }
    		}
    		
    		return this;
    	}
    	
    	public IndianSettlementBuilder missionary(Unit missionary){
    		this.residentMissionary = missionary;
    		
    		return this;
    	}
    	
    	public IndianSettlementBuilder skillToTeach(String skill){
    		this.skillTaught = skill;
    		
    		return this;
    	}

      private String getSimpleName(Player player, boolean isCapital) {
          return (isCapital) ? player.getName() + "-capital"
              : "Settlement-" + settlementNumber++;
      }

    	public IndianSettlement build(){
    		UnitType skillToTeach = null;
    		
    		if(skillTaught != null){
    			skillToTeach = Specification.getSpecification().getUnitType(skillTaught);
    		}
    			
    		UnitType indianBraveType = Specification.getSpecification().getUnitType("model.unit.brave");
    		
    		// indianPlayer not set, get default
    		if(indianPlayer == null){
    			indianPlayer = game.getPlayer(defaultIndianPlayer);
    			if(indianPlayer == null){
    				throw new IllegalArgumentException("Default Indian player " + defaultIndianPlayer + " not in game");
    			}
    		}
    		
    		// settlement tile no set, get default
    		if(settlementTile == null){
    			settlementTile = game.getMap().getTile(5, 8);
    			if(settlementTile == null){
    				throw new IllegalArgumentException("Default tile not in game");
    			}
    		}
    		
    		IndianSettlement camp =
                    new IndianSettlement(game, indianPlayer, settlementTile,
                                         getSimpleName(indianPlayer, isCapital),
                                         isCapital, skillToTeach, isVisited, residentMissionary);
            
    		// Add braves
            for(int i=0; i < initialBravesInCamp; i++){
            	Unit brave = new Unit(game, camp, indianPlayer, indianBraveType, UnitState.ACTIVE,
                    indianBraveType.getDefaultEquipment());
            	camp.addOwnedUnit(brave);
            }
            camp.placeSettlement();
            return camp;
    	}
    	
    	public IndianSettlementBuilder reset() {
    		setStartingParams();
    		
    		return this;
    	}
    }


    /**
     * Set the production bonus of the given colony to the given
     * value.
     *
     * @param colony a <code>Colony</code> value
     * @param value an <code>int</code> value
     */
    public void setProductionBonus(Colony colony, int value) {
        try {
            Field productionBonus = Colony.class.getDeclaredField("productionBonus");
            productionBonus.setAccessible(true);
            productionBonus.setInt(colony, value);
        } catch (Exception e) {
            // do nothing
        }
    }

    /**
     * Build/place a colony with a unit, without requiring the server.
     */
    public void nonServerBuildColony(Unit builder, Colony colony) {
        colony.placeSettlement();
        nonServerJoinColony(builder, colony);
    }

    /**
     * Join a colony with a unit, without requiring the server.
     */
    public void nonServerJoinColony(Unit builder, Colony colony) {
        builder.setState(UnitState.IN_COLONY);
        builder.setLocation(colony);
        builder.setMovesLeft(0);
    }
}
