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

import java.util.Locale;

import junit.framework.TestCase;
import net.sf.freecol.FreeCol;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.Specification;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.GameOptions;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Nation;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.Unit.UnitState;

/**
 * The base class for all FreeCol tests. Contains useful methods used by the
 * individual tests.
 * 
 * @author $Author$
 * @version $Revision$ ($Date$)
 * 
 */
public class FreeColTestCase extends TestCase {

    public static final TileType plainsType = FreeCol.getSpecification().getTileType("model.tile.plains");

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
    
    public static Specification spec(){
    	return FreeCol.getSpecification();
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
        game = new Game(new MockModelController());
        game.setMaximumPlayers(8);

        for (Nation n : FreeCol.getSpecification().getNations()) {
            Player p;
            if (n.getType().isEuropean() && !n.getType().isREF()){
                p = new Player(game, n.getRulerName(), false, n);
            } else {
                p = new Player(game, n.getRulerName(), false, true, n);
            }
            game.addPlayer(p);
        }
        Specification.getSpecification().applyDifficultyLevel(game.getGameOptions().getInteger(GameOptions.DIFFICULTY));
        return game;
    }

    /**
     * Returns an empty Map object, i.e. one that does not yet contain
     * any tiles.
     *
     * @return a <code>Map</code> value
     */
    public static Map getEmptyMap() {
        return new Map(getGame(), getTestTiles(game, 30, 64, plainsType));
    }

    public static Tile[][] getTestTiles(Game game, int width, int height, TileType tileType) {
        Tile[][] tiles = new Tile[width][height];

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                tiles[x][y] = new Tile(game, tileType, x, y);
            }
        }
        return tiles;
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
        return getTestMap(plainsType, false);
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
        return getTestMap(tileType, false);
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
        Map m = new Map(getGame(), getTestTiles(getGame(), 20, 15, tileType));
        if (explored) {
            for (Player player : getGame().getPlayers()) {
                for (Tile tile : m.getAllTiles()) {
                    tile.setExploredBy(player, true);
                }
            }
        }
        return m;
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
    public static Map getCoastTestMap(TileType tileType) {
    	int totalWidth = 20;
    	int totalHeight = 15;
    	
    	int landWidth = (int) Math.floor(totalWidth/2);
    	int landHeight = (int) Math.floor(totalHeight/2);
    	
    	Tile[][] tiles = new Tile[totalWidth][totalHeight];

        for (int x = 0; x < landWidth; x++) {
            for (int y = 0; y < landHeight; y++) {
                tiles[x][y] = new Tile(getGame(), tileType, x, y);
            }
        }
        
        TileType oceanType = spec().getTileType("model.tile.ocean");
        for (int x = landWidth ; x < totalWidth; x++) {
            for (int y = landHeight ; y < totalHeight; y++) {
                tiles[x][y] = new Tile(getGame(), oceanType, x, y);
            }
        }
    	
    	Map m = new Map(getGame(), tiles);
        
        return m;
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

        if (numberOfSettlers < 1)
            throw new IllegalArgumentException();

        Game game = getGame();
        // TODO not sure if this is correct
        Player dutch = game.getPlayer("model.nation.dutch");

        Map map = getTestMap(FreeCol.getSpecification().getTileType("model.tile.plains"), true);
        game.setMap(map);

        Tile tile = map.getTile(tileX, tileY);
        Colony colony = new Colony(game, dutch, "New Amsterdam", tile);

        UnitType unitType = FreeCol.getSpecification().getUnitType("model.unit.freeColonist");
        Unit soldier = new Unit(game, tile, dutch, unitType, UnitState.ACTIVE, unitType.getDefaultEquipment());

        soldier.buildColony(colony);

        for (int i = 1; i < numberOfSettlers; i++) {
            Unit settler = new Unit(game, tile, dutch, unitType, UnitState.ACTIVE, unitType.getDefaultEquipment());
            settler.setLocation(colony);
        }

        assertEquals(numberOfSettlers, colony.getUnitCount());

        return colony;
    }

    public static class IndianSettlementBuilder{
    	
    	// Required parameter
    	private final Game game;
    	
    	private Player indianPlayer;
    	private final String defaultIndianPlayer = "model.nation.tupi";
    	private String skillTaught;
    	private int initialBravesInCamp;
    	private Tile settlementTile;
    	
    	private boolean isCapital;
        private boolean isVisited;
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
            isVisited = false;
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
    	
    	public IndianSettlementBuilder visited(boolean isVisited){
    		this.isVisited = isVisited;
    		
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
    	
    	public IndianSettlement build(){
    		UnitType skillToTeach = null;
    		
    		if(skillTaught != null){
    			skillToTeach = FreeCol.getSpecification().getUnitType(skillTaught);
    		}
    			
    		UnitType indianBraveType = FreeCol.getSpecification().getUnitType("model.unit.brave");
    		
    		// indianPlayer not set, get default
    		if(indianPlayer == null){
    			indianPlayer = game.getPlayer(defaultIndianPlayer);
    			if(indianPlayer == null){
    				throw new IllegalArgumentException("Default Indian player " + defaultIndianPlayer + " not in game");
    			}
    		}
    		
    		// settlement tile no set, get default
    		// indianPlayer not set, get default
    		if(settlementTile == null){
    			settlementTile = game.getMap().getTile(5, 8);
    			if(settlementTile == null){
    				throw new IllegalArgumentException("Default tile not in game");
    			}
    		}
    		
    		IndianSettlement camp = new IndianSettlement(game,indianPlayer,settlementTile,isCapital,skillToTeach,isVisited,residentMissionary);
            
    		// Add braves
            for(int i=0; i < initialBravesInCamp; i++){
            	Unit brave = new Unit(game, camp, indianPlayer, indianBraveType, UnitState.ACTIVE,
                    indianBraveType.getDefaultEquipment());
            	camp.addOwnedUnit(brave);
            }
            
            return camp;
    	}
    	
    	public IndianSettlementBuilder reset() {
    		setStartingParams();
    		
    		return this;
    	}
    }
}
