package net.sf.freecol.server.generator;

import java.util.Iterator;
import java.util.Vector;

import junit.framework.TestCase;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Map.Position;
import net.sf.freecol.common.option.SelectOption;
import net.sf.freecol.util.test.MockModelController;

public class MapGeneratorTest extends TestCase {

	public void testWithNoIndians() {

		Game g = new Game(new MockModelController());

		// A new game does not have a map yet
		assertEquals(null, g.getMap());

		MapGenerator gen = new MapGenerator(g);

		Vector<Player> players = new Vector<Player>();
		players.add(new Player(g, String.valueOf(Player.DUTCH), false, Player.DUTCH));
		players.add(new Player(g, String.valueOf(Player.FRENCH), false, Player.FRENCH));
		players.add(new Player(g, String.valueOf(Player.ENGLISH), false, Player.ENGLISH));
		players.add(new Player(g, String.valueOf(Player.SPANISH), false, Player.SPANISH));

		for (Player p : players) {
			g.addPlayer(p);
		}

		gen.createMap(players);

		// Check that the map is created at all
		assertNotNull(g.getMap());
	}

	public void testSinglePlayerOnSmallMap() {

		Game g = new Game(new MockModelController());

		// A new game does not have a map yet
		assertEquals(null, g.getMap());

		MapGenerator gen = new MapGenerator(g);
		((SelectOption) gen.getMapGeneratorOptions().getObject(MapGeneratorOptions.MAP_SIZE))
			.setValue(MapGeneratorOptions.MAP_SIZE_SMALL);

		Vector<Player> players = new Vector<Player>();
		players.add(new Player(g, String.valueOf(Player.DUTCH), false, Player.DUTCH));

		for (Player p : players) {
			g.addPlayer(p);
		}
		gen.createMap(players);

		// Check that the map is created at all
		assertNotNull(g.getMap());

		assertEquals(30, g.getMap().getWidth());
		assertEquals(64, g.getMap().getHeight());

	}
	
	public void testMapGenerator() {

		Game g = new Game(new MockModelController());

		// A new game does not have a map yet
		assertEquals(null, g.getMap());

		MapGenerator gen = new MapGenerator(g);

		Vector<Player> players = new Vector<Player>();

		for (int i = 0; i < Player.NUMBER_OF_NATIONS; i++) {
			if (g.getPlayer(i) != null) {
				continue;
			}
			Player p = new Player(g, String.valueOf(i), false, i);
			g.addPlayer(p);
			players.add(p);
		}

		gen.createMap(players);

		// Check that the map is created at all
		assertNotNull(g.getMap());

		// Map of correct size?
		Map m = g.getMap();
		assertEquals(m.getWidth(), gen.getMapGeneratorOptions().getWidth());
		assertEquals(m.getHeight(), gen.getMapGeneratorOptions().getHeight());

		// Sufficient land?
		Iterator<Position> it = m.getWholeMapIterator();
		int land = 0;
		int total = 0;
		while (it.hasNext()) {
			Position p = it.next();
			Tile t = m.getTile(p);
			if (t.isLand())
				land++;
			total++;
		}
		// Land Mass requirement fulfilled?
		assertTrue(100 * land / total >= gen.getMapGeneratorOptions().getLandMass());

		// Does the wholeMapIterator visit all fields?
		assertEquals(gen.getMapGeneratorOptions().getWidth()
			* gen.getMapGeneratorOptions().getHeight(), total);
	}

	/**
	 * Make sure that each tribe has exactely one capital
	 * 
	 */
	public void testIndianCapital() {

		Game g = new Game(new MockModelController());

		MapGenerator gen = new MapGenerator(g);

		Vector<Player> players = new Vector<Player>();

		for (int i = 0; i < Player.NUMBER_OF_NATIONS; i++) {
			Player p = new Player(g, String.valueOf(i), false, i);
			g.addPlayer(p);
			players.add(p);
		}

		gen.createMap(players);

		// Check that the map is created at all
		assertNotNull(g.getMap());

		for (Player p : players) {
			if (!p.isIndian())
				continue;

			Iterator<Settlement> i = p.getIndianSettlementIterator();

			// Check that every indian player has exactely one capital if s/he
			// has at least one settlement.
			if (i.hasNext()) {
				int found = 0;
				while (i.hasNext()) {
					IndianSettlement s = (IndianSettlement) i.next();
					if (s.isCapital())
						found++;
				}

				assertEquals(1, found);
			}
		}
	}

}
