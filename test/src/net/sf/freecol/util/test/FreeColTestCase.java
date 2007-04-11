package net.sf.freecol.util.test;

import java.util.Vector;

import junit.framework.TestCase;
import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.server.generator.MapGeneratorOptions;

/**
 * The base class for all FreeCol tests. Contains useful methods used by the individual tests.
 * 
 * @author $Author$
 * @version $Revision$ ($Date$)
 *
 */
public class FreeColTestCase extends TestCase {
	
	public static Game getStandardGame() {
		Game game = new Game(new MockModelController());

		Vector<Player> players = new Vector<Player>();

		for (int i = 0; i < Player.NUMBER_OF_NATIONS; i++) {
			Player p = new Player(game, String.valueOf(i), false, i);
			game.addPlayer(p);
			players.add(p);
		}
		return game;
	}
    
    public static Map getEmptyMap() {
        Game game = getStandardGame();

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


}
