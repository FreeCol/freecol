package net.sf.freecol.common.model;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.model.Map.Position;
import net.sf.freecol.server.generator.MapGeneratorOptions;
import net.sf.freecol.util.test.FreeColTestCase;

public class MapTest extends FreeColTestCase {

	public void testMapGameInt() throws FreeColException {

		Game game = getStandardGame();
		Map m = new Map(game, MapGeneratorOptions.MAP_SIZE_SMALL);

		assertEquals(30, m.getWidth());
		assertEquals(64, m.getHeight());

	}

	public void testMapConstructorVector() {

		Game game = getStandardGame();

		Vector<Vector<Tile>> tiles = new Vector<Vector<Tile>>(10);

		for (int x = 0; x < 10; x++) {
			tiles.add(new Vector<Tile>(15));
		}

		for (int x = 0; x < 10; x++) {
			for (int y = 0; y < 15; y++) {
				tiles.get(x).add(new Tile(game, Tile.PLAINS, x, y));
			}
		}

		Map map = new Map(game, tiles);

		assertEquals(10, map.getWidth());
		assertEquals(15, map.getHeight());

		for (int x = 0; x < 10; x++) {
			for (int y = 0; y < 15; y++) {
				assertEquals(Tile.PLAINS, map.getTile(x, y).getType());
			}
		}
	}

	public void testGetSurroundingTiles() {
		Game game = getStandardGame();

		Vector<Vector<Tile>> tiles = new Vector<Vector<Tile>>(10);

		for (int x = 0; x < 10; x++) {
			tiles.add(new Vector<Tile>(15));
		}

		for (int x = 0; x < 10; x++) {
			for (int y = 0; y < 15; y++) {
				tiles.get(x).add(new Tile(game, Tile.PLAINS, x, y));
			}
		}

		Map map = new Map(game, tiles);

		// Check in the middle
		Vector<Tile> surroundingTiles = map.getSurroundingTiles(tiles.get(4).get(8), 1);

		assertEquals(8, surroundingTiles.size());
		assertTrue(surroundingTiles.contains(map.getTile(4, 6)));
		assertTrue(surroundingTiles.contains(map.getTile(4, 10)));
		assertTrue(surroundingTiles.contains(map.getTile(3, 8)));
		assertTrue(surroundingTiles.contains(map.getTile(5, 8)));
		assertTrue(surroundingTiles.contains(map.getTile(3, 7)));
		assertTrue(surroundingTiles.contains(map.getTile(4, 7)));
		assertTrue(surroundingTiles.contains(map.getTile(3, 9)));
		assertTrue(surroundingTiles.contains(map.getTile(4, 9)));

		// Check on sides
		surroundingTiles = map.getSurroundingTiles(tiles.get(0).get(0), 1);

		assertEquals(3, surroundingTiles.size());
		assertTrue(surroundingTiles.contains(map.getTile(0, 2)));
		assertTrue(surroundingTiles.contains(map.getTile(1, 0)));
		assertTrue(surroundingTiles.contains(map.getTile(0, 1)));

		// Check larger range
		surroundingTiles = map.getSurroundingTiles(tiles.get(4).get(8), 2);

		assertEquals(25 - 1, surroundingTiles.size());

		// Check that all tiles are returned
		surroundingTiles = map.getSurroundingTiles(tiles.get(4).get(8), 10);
		assertEquals(150 - 1, surroundingTiles.size());
	}

	public void testGetReverseDirection() {
		assertEquals(Map.S, Map.getReverseDirection(Map.N));
		assertEquals(Map.N, Map.getReverseDirection(Map.S));
		assertEquals(Map.E, Map.getReverseDirection(Map.W));
		assertEquals(Map.W, Map.getReverseDirection(Map.E));
		assertEquals(Map.NE, Map.getReverseDirection(Map.SW));
		assertEquals(Map.NW, Map.getReverseDirection(Map.SE));
		assertEquals(Map.SW, Map.getReverseDirection(Map.NE));
		assertEquals(Map.SE, Map.getReverseDirection(Map.NW));
	}

	public void testGetWholeMapIterator() {

		Game game = getStandardGame();

		Vector<Vector<Tile>> tiles = new Vector<Vector<Tile>>(10);

		for (int x = 0; x < 5; x++) {
			tiles.add(new Vector<Tile>(6));
		}

		Set<Position> positions = new HashSet<Position>();
        Set<Tile> allTiles = new HashSet<Tile>();
        
		for (int x = 0; x < 5; x++) {
			for (int y = 0; y < 6; y++) {
                Tile tile = new Tile(game, Tile.PLAINS, x, y);
				tiles.get(x).add(tile);
                allTiles.add(tile);
				positions.add(new Position(x, y));
			}
		}

		Map map = new Map(game, tiles);

		Iterator<Position> wholeMapIterator = map.getWholeMapIterator();
		for (int i = 0; i < 30; i++) {
			assertTrue(wholeMapIterator.hasNext());
			assertTrue(positions.remove(wholeMapIterator.next()));
        }
        assertEquals(0, positions.size());
		assertFalse(wholeMapIterator.hasNext());

        // Check for-Iterator
		for (Tile t : map.getAllTiles()){
		    assertTrue(allTiles.remove(t));
        }
        assertEquals(0, positions.size());
	}

	public void testGetAdjacent() {
		Game game = getStandardGame();

		Vector<Vector<Tile>> tiles = new Vector<Vector<Tile>>(10);

		for (int x = 0; x < 10; x++) {
			tiles.add(new Vector<Tile>(15));
		}

		for (int x = 0; x < 10; x++) {
			for (int y = 0; y < 15; y++) {
				tiles.get(x).add(new Tile(game, Tile.PLAINS, x, y));
			}
		}

		Map map = new Map(game, tiles);

		{ // Even case
			Iterator<Position> i = map.getAdjacentIterator(map.getTile(4, 8).getPosition());

			Vector<Position> shouldBe = new Vector<Position>();
			shouldBe.add(new Position(4, 6));
			shouldBe.add(new Position(4, 10));
			shouldBe.add(new Position(3, 8));
			shouldBe.add(new Position(5, 8));

			shouldBe.add(new Position(4, 7));
			shouldBe.add(new Position(4, 9));
			shouldBe.add(new Position(3, 7));
			shouldBe.add(new Position(3, 9));

			for (int j = 0; j < 8; j++) {
				assertTrue(i.hasNext());
				Position p = i.next();
				assertTrue("" + p.getX() + ", " + p.getY(), shouldBe.contains(p));
			}
			assertFalse(i.hasNext());
		}
		{ // Even case 2

			Iterator<Position> i = map.getAdjacentIterator(map.getTile(5, 8).getPosition());

			Vector<Position> shouldBe = new Vector<Position>();
			shouldBe.add(new Position(5, 6));
			shouldBe.add(new Position(5, 10));
			shouldBe.add(new Position(4, 8));
			shouldBe.add(new Position(6, 8));

			shouldBe.add(new Position(4, 7));
			shouldBe.add(new Position(5, 7));
			shouldBe.add(new Position(4, 9));
			shouldBe.add(new Position(5, 9));

			for (int j = 0; j < 8; j++) {
				assertTrue(i.hasNext());
				Position p = i.next();
				assertTrue("" + p.getX() + ", " + p.getY(), shouldBe.contains(p));
			}
			assertFalse(i.hasNext());
		}
		{ // Odd case

			Iterator<Position> i = map.getAdjacentIterator(map.getTile(4, 7).getPosition());

			Vector<Position> shouldBe = new Vector<Position>();
			shouldBe.add(new Position(4, 5));
			shouldBe.add(new Position(4, 9));
			shouldBe.add(new Position(3, 7));
			shouldBe.add(new Position(5, 7));

			shouldBe.add(new Position(4, 6));
			shouldBe.add(new Position(5, 6));
			shouldBe.add(new Position(4, 8));
			shouldBe.add(new Position(5, 8));

			for (int j = 0; j < 8; j++) {
				assertTrue(i.hasNext());
				Position p = i.next();
				assertTrue("" + p.getX() + ", " + p.getY(), shouldBe.contains(p));
			}
			assertFalse(i.hasNext());
		}
	}

}
