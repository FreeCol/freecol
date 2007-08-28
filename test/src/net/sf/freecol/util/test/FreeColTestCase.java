package net.sf.freecol.util.test;

import java.util.Locale;
import java.util.Vector;

import junit.framework.TestCase;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.server.generator.MapGeneratorOptions;

/**
 * The base class for all FreeCol tests. Contains useful methods used by the
 * individual tests.
 * 
 * @author $Author$
 * @version $Revision$ ($Date$)
 * 
 */
public class FreeColTestCase extends TestCase {

    /**
     * use getGame to access this.
     */
    static Game game;

    static boolean updateLocale = true;
    
    public void setUp() {
        if (updateLocale) {
            updateLocale = false;
            Messages.setMessageBundle(Locale.US);
        }
    }

    /**
     * Get a game pseudo-singleton, i.e. the same instance will be returned
     * until getStandardGame() is called, which resets the singleton to a new
     * value.
     * 
     * Calling this method repetetively without calling getStandardGame() will
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
     * Returns a new game, with all players set.
     * 
     * As a side effect this call will reset the singleton game value that can
     * be accessed using getGame().
     * 
     * @return A new game with with players for each nation added.
     */
    public static Game getStandardGame() {
        game = new Game(new MockModelController());

        Vector<Player> players = new Vector<Player>();

        for (int i = 0; i < Player.NUMBER_OF_NATIONS; i++) {
            Player p = new Player(game, String.valueOf(i), false, !Player.isEuropeanNoREF(i), i);
            game.addPlayer(p);
            players.add(p);
        }
        return game;
    }

    /**
     * Returns an empty Map object, i.e. one that does not yet contain
     * any tiles.
     *
     * @return a <code>Map</code> value
     */
    public static Map getEmptyMap() {
        Game game = getGame();

        try {
            return new Map(game, MapGeneratorOptions.MAP_SIZE_SMALL);
        } catch (FreeColException e) {
            fail();
            return null;
        }
    }

    public static Vector<Vector<Tile>> getTestTiles(Game game, int width, int height, TileType tileType) {
        Vector<Vector<Tile>> tiles = new Vector<Vector<Tile>>(10);

        for (int x = 0; x < width; x++) {
            tiles.add(new Vector<Tile>(15));
        }

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                tiles.get(x).add(new Tile(game, tileType, x, y));
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
        return getTestMap(FreeCol.getSpecification().getTileType("model.tile.plains"), false);
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
        Player dutch = game.getPlayer(Player.DUTCH);

        Map map = getTestMap(FreeCol.getSpecification().getTileType("model.tile.plains"), true);
        game.setMap(map);

        Colony colony = new Colony(game, dutch, "New Amsterdam", map.getTile(tileX, tileY));

        UnitType unitType = FreeCol.getSpecification().getUnitType("model.unit.freeColonist");
        Unit soldier = new Unit(game, map.getTile(tileX, tileY), dutch, unitType, Unit.ACTIVE, true, false, 0, false);

        soldier.setWorkType(Goods.FOOD);
        soldier.buildColony(colony);

        for (int i = 1; i < numberOfSettlers; i++) {
            Unit settler = new Unit(game, map.getTile(tileX, tileY), dutch, unitType, Unit.ACTIVE, true, false, 0,
                    false);
            settler.setLocation(colony);
        }

        assertEquals(numberOfSettlers, colony.getUnitCount());

        return colony;
    }

}
