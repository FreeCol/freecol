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

package net.sf.freecol.util.test;

import java.lang.reflect.Field;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import junit.framework.TestCase;
import net.sf.freecol.FreeCol;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.io.FreeColTcFile;
import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.CombatModel;
import net.sf.freecol.common.model.CombatModel.CombatOdds;
import net.sf.freecol.common.model.CombatModel.CombatResult;
import net.sf.freecol.common.model.FreeColGameObject;
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
import net.sf.freecol.common.model.WorkLocation;
import net.sf.freecol.server.model.ServerGame;
import net.sf.freecol.server.model.ServerIndianSettlement;
import net.sf.freecol.server.model.ServerPlayer;
import net.sf.freecol.server.model.ServerUnit;


/**
 * The base class for all FreeCol tests. Contains useful methods used by the
 * individual tests.
 */
public class FreeColTestCase extends TestCase {

    private static java.util.Map<String, Specification> specifications
        = new HashMap<>();

    /**
     * use getGame to access this.
     */
    static Game game;

    static boolean updateLocale = true;

    @Override
    protected void setUp() throws Exception {
        if (updateLocale) {
            updateLocale = false;
            Messages.loadMessageBundle(Locale.US);
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
     * Specifically sets the game instance to run with.  Necessary for
     * server tests that create their own game instances.  Allows for
     * same interface for accessing the game instance for all types of
     * tests.
     *
     * @param newGame Game instance to work with
     */
    public static void setGame(Game newGame) {
        game = newGame;
    }

    public static Specification spec() {
        return getSpecification("freecol");
    }

    public static Specification spec(String name) {
        return getSpecification(name);
    }

    public static Specification getSpecification(String name) {
        Specification result = specifications.get(name);
        if (result == null) {
            try {
                FreeColTcFile tc = new FreeColTcFile(name);
                result = FreeCol.loadSpecification(tc, null, "model.difficulty.medium");
                specifications.put(name, result);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        return result;
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
        return getStandardGame("freecol");
    }

    /**
     * Returns a new game, with all players set.
     *
     * As a side effect this call will reset the singleton game value that can
     * be accessed using getGame().
     *
     * @param specName a <code>String</code> value
     * @return A new game with with players for each nation added.
     */
    public static Game getStandardGame(String specName) {
        Specification specification = spec(specName);
        game = new ServerGame(specification);
        NationOptions nationOptions = new NationOptions(specification);
        for (Nation nation : specification.getEuropeanNations()) {
            nationOptions.setNationState(nation, NationOptions.NationState.AVAILABLE);
        }
        game.setNationOptions(nationOptions);

        specification.applyDifficultyLevel("model.difficulty.medium");
        for (Nation n : specification.getNations()) {
            if (n.isUnknownEnemy()) continue;
            Player p = new ServerPlayer(game, false, n, null, null);
            boolean ai = !n.getType().isEuropean() || n.getType().isREF();
            p.setAI(ai);
            if (ai || game.canAddNewPlayer()) game.addPlayer(p);
        }
        return game;
    }

    /**
     * Creates a standardized map on which all fields have the plains type.
     *
     * Uses the getGame() method to access the currently running game.
     *
     * Does not call Game.setMap(Map) with the returned map. The map
     * is unexplored.
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
     * Does not call Game.setMap(Map) with the returned map. The map
     * is unexplored.
     *
     * @param tileType The type of land with which to initialize the map.
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
     * @param tileType The type of land with which to initialize the map.
     * @param explored Set to true if you want all the tiles on the
     *     map to have been explored by all players.
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
     * @param landTileType The type of land with which to initialize the map.
     *
     * @param explored Set to true if you want all the tiles on the map to have been explored by all players.
     *
     * @return The map created as described above.
     */
    public static Map getCoastTestMap(TileType landTileType, boolean explored) {
        int totalWidth = 20;
        int totalHeight = 15;
        final TileType oceanType = spec().getTileType("model.tile.ocean");
        
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

        // Add high seas.
        final TileType highSeasType = spec().getTileType("model.tile.highSeas");
        for (int y = 0; y < totalHeight; y++) {
            builder.setTile(totalWidth - 1, y, highSeasType);
        }

        return builder.build();
    }

    /**
     * Get a standard colony at the location 5,8 with one free colonist
     *
     * @return The <code>Colony</code> as specified.
     */
    public Colony getStandardColony() {
        return getStandardColony(1, 5, 8);
    }

    /**
     * Get a colony with the given number of settlers
     *
     * @param numberOfSettlers The number of settlers to put into the
     *     colony.  Must be >= 1.
     * @return The <code>Colony</code> as specified.
     */
    public Colony getStandardColony(int numberOfSettlers) {
        return getStandardColony(numberOfSettlers, 5, 8);
    }

    /**
     * Get a colony with the given number of settlers, at a specified
     * location.
     *
     * @param numberOfSettlers The number of settlers to put into the
     *     colony.  Must be >= 1.
     * @param tileX Coordinate of tile for the colony.
     * @param tileY Coordinate of tile for the colony.
     * @return The <code>Colony</code> as specified.
     */
    public Colony getStandardColony(int numberOfSettlers,
                                    int tileX, int tileY) {
        Game game = getGame();
        Map map = game.getMap();
        Tile tile = map.getTile(tileX, tileY);

        FreeColTestUtils.ColonyBuilder builder
            = FreeColTestUtils.getColonyBuilder();
        builder.colonyTile(tile).initialColonists(numberOfSettlers);

        Colony ret = builder.build();
        ((ServerPlayer)ret.getOwner()).exploreForSettlement(ret);
        return ret;
    }

    /**
     * Useful utility to make sure a work location is empty before doing
     * some test that implicates it.
     *
     * @param wl The <code>WorkLocation</code> to clear.
     * @return True if the work location is clear, false if there was a problem
     *     removing a unit.
     */
    public boolean clearWorkLocation(WorkLocation wl) {
        for (Unit u : wl.getUnitList()) {
            for (WorkLocation w : wl.getColony().getCurrentWorkLocations()) {
                if (w == wl) continue;
                if (w.canAdd(u)) {
                    u.setLocation(w);
                    if (u.getLocation() == w) break;
                }
            }
        }
        return wl.isEmpty();
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
            this.tiles = new TileType[width][height];
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
            Map map = new Map(game, width, height);
            Region region = new Region(game);

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    TileType tileType = tiles[x][y];
                    Tile t = new Tile(game, tileType, x, y);
                    t.setRegion(region);
                    map.setTile(t, x, y);
                }
            }

            map.resetContiguity();
            map.resetHighSeasCount();
            if (exploredByAll) {
                for (Tile tile : map.getAllTiles()) {
                    for (Player player : game.getLiveEuropeanPlayers(null)) {
                        tile.setExplored(player, true);
                    }
                }
            }
            return map;
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
            Tile tileOnMap = this.game.getMap().getTile(tile.getX(), tile.getY());
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
                skillToTeach = spec().getUnitType(skillTaught);
            }

            // indianPlayer not set, get default
            if (indianPlayer == null) {
                indianPlayer = game.getPlayerByNationId(defaultIndianPlayer);
                if(indianPlayer == null){
                    throw new IllegalArgumentException("Default Indian player " + defaultIndianPlayer + " not in game");
                }
            }
            UnitType indianBraveType = spec().getDefaultUnitType(indianPlayer);

            // settlement tile no set, get default
            if(settlementTile == null){
                settlementTile = game.getMap().getTile(5, 8);
                if(settlementTile == null){
                    throw new IllegalArgumentException("Default tile not in game");
                }
            }

            IndianSettlement camp
                = new ServerIndianSettlement(game, indianPlayer,
                                             getSimpleName(indianPlayer, isCapital),
                                             settlementTile, isCapital,
                                             skillToTeach, residentMissionary);
            indianPlayer.addSettlement(camp);

            // Add braves
            for(int i=0; i < initialBravesInCamp; i++){
                Unit brave = new ServerUnit(game, camp, indianPlayer,
                                            indianBraveType);
                camp.addOwnedUnit(brave);
            }
            camp.placeSettlement(true);
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
            colony.invalidateCache();
        } catch (Exception e) {
            // do nothing
        }
    }

    /**
     * Build/place a colony with a unit, without requiring the server.
     */
    public void nonServerBuildColony(Unit builder, Colony colony) {
        colony.placeSettlement(true);//-vis
        colony.getOwner().invalidateCanSeeTiles();//+vis
        nonServerJoinColony(builder, colony);
    }

    /**
     * Join a colony with a unit, without requiring the server.
     */
    public void nonServerJoinColony(Unit builder, Colony colony) {
        builder.setLocation(colony);
        builder.setMovesLeft(0);
    }

    /**
     * Repeatedly ask the CombatModel for an attack result until it
     * gives the primary one we want (WIN, LOSE, NO_RESULT).
     */
    public List<CombatResult> fakeAttackResult(CombatResult result,
                                               FreeColGameObject attacker,
                                               FreeColGameObject defender)
    {
        List<CombatResult> crs;
        final double delta = 0.02;
        CombatModel combatModel = getGame().getCombatModel();
        CombatOdds combatOdds = combatModel.calculateCombatOdds(attacker, defender);
        double p = combatOdds.win;
        MockPseudoRandom mr = new MockPseudoRandom();
        List<Integer> number = new ArrayList<>();
        number.add(-1);
        do {
            p += (result == CombatResult.WIN) ? -delta : delta;
            if (p < 0.0 || p >= 1.0) {
                throw new IllegalStateException("f out of range: "
                                                + Double.toString(p));
            }
            number.set(0, (int)(Integer.MAX_VALUE * p));
            mr.setNextNumbers(number, true);
            crs = combatModel.generateAttackResult(mr, attacker, defender);
        } while (crs.get(0) != result);
        return crs;
    }

    /**
     * Check a combat result list.
     *
     * @param name A base string for the error message.
     * @param crs The list of <code>CombatResult</code> to check.
     * @param results The expected <code>CombatResult</code>s.
     */
    public void checkCombat(String name, List<CombatResult> crs,
                            CombatResult... results) {
        int i = 0;
        for (CombatResult cr : results) {
            CombatResult expect = (i < crs.size()) ? crs.get(i) : null;
            if (expect != cr) break;
            i++;
        }
        if (i == results.length) {
            if (crs.size() == i) return;
            i++;
        }
        String err = name + ", failed at " + i + ":";
        for (CombatResult cr : results) {
            err += " " + cr;
        }
        err += " !=";
        for (CombatResult cr : crs) {
            err += " " + cr;
        }
        fail(err);
    }               

    /**
     * Check a list of goods.
     *
     * @param err A base string for the error message.
     * @param goods A list of expected <code>AbstractGoods</code> to check.
     * @param results The expected <code>AbstractGoods</code>.
     */
    public void checkGoods(String err, List<AbstractGoods> goods,
                           AbstractGoods... results) {
        List<AbstractGoods> check = new ArrayList<>(goods);
        for (AbstractGoods ag : results) {
            assertTrue(err + " requires " + ag, check.contains(ag));
            check.remove(ag);
        }
        assertTrue(err + " requires more goods", check.isEmpty());
    }
}
