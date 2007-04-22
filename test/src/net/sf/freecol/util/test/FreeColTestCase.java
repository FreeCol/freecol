package net.sf.freecol.util.test;

import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import junit.framework.TestCase;
import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Map.Position;
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
            Player p = new Player(game, String.valueOf(i), !Player.isEuropeanNoREF(i), i);
            game.addPlayer(p);
            players.add(p);
        }
        return game;
    }

    public static Map getEmptyMap() {
        Game game = getGame();

        try {
            return new Map(game, MapGeneratorOptions.MAP_SIZE_SMALL);
        } catch (FreeColException e) {
            fail();
            return null;
        }
    }

    public static Vector<Vector<Tile>> getTestTiles(Game game, int width, int height, int type) {
        Vector<Vector<Tile>> tiles = new Vector<Vector<Tile>>(10);

        for (int x = 0; x < width; x++) {
            tiles.add(new Vector<Tile>(15));
        }

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                tiles.get(x).add(new Tile(game, type, x, y));
            }
        }
        return tiles;
    }
    
    /**
     * Creates a standardized map on which all fields have the same given type.
     * 
     * Uses the getGame() method to access the currently running game.
     * 
     * Does not call Game.setMap(Map) with the returned map.
     * 
     * @param type
     * @return
     */
    public static Map getTestMap(int type){
        return new Map(getGame(), getTestTiles(getGame(), 20, 15, type));
    }
}
