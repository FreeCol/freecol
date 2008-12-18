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

package net.sf.freecol.common.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.Map.Position;
import net.sf.freecol.util.test.FreeColTestCase;

public class MapTest extends FreeColTestCase {

    public void testMapGameInt() throws FreeColException {
        int expectedWidth = 20;
        int expectedHeigth = 15;
        
        Game game = getStandardGame();
        MapBuilder builder = new MapBuilder(game);
        Map map = builder.setDimensions(expectedWidth, expectedHeigth).build();

        assertEquals(expectedWidth, map.getWidth());
        assertEquals(expectedHeigth, map.getHeight());
    }

    public void testGetSurroundingTiles() {
        Game game = getStandardGame();

        MapBuilder builder = new MapBuilder(game);
        Map map = builder.setDimensions(10, 15).build();

        // Check in the middle
        List<Tile> surroundingTiles = map.getSurroundingTiles(map.getTile(4,8), 1);

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
        surroundingTiles = map.getSurroundingTiles(map.getTile(0, 0), 1);

        assertEquals(3, surroundingTiles.size());
        assertTrue(surroundingTiles.contains(map.getTile(0, 2)));
        assertTrue(surroundingTiles.contains(map.getTile(1, 0)));
        assertTrue(surroundingTiles.contains(map.getTile(0, 1)));

        // Check larger range
        surroundingTiles = map.getSurroundingTiles(map.getTile(4,8), 2);

        assertEquals(25 - 1, surroundingTiles.size());

        // Check that all tiles are returned
        surroundingTiles = map.getSurroundingTiles(map.getTile(4,8), 10);
        assertEquals(150 - 1, surroundingTiles.size());
    }

    public void testGetReverseDirection() {
        assertEquals(Direction.S, Direction.N.getReverseDirection());
        assertEquals(Direction.N, Direction.S.getReverseDirection());
        assertEquals(Direction.E, Direction.W.getReverseDirection());
        assertEquals(Direction.W, Direction.E.getReverseDirection());
        assertEquals(Direction.NE, Direction.SW.getReverseDirection());
        assertEquals(Direction.NW, Direction.SE.getReverseDirection());
        assertEquals(Direction.SW, Direction.NE.getReverseDirection());
        assertEquals(Direction.SE, Direction.NW.getReverseDirection());
    }

    public void testGetWholeMapIterator() {

        Game game = getStandardGame();

        Tile[][] tiles = new Tile[5][6];

        Set<Position> positions = new HashSet<Position>();
        Set<Tile> allTiles = new HashSet<Tile>();
        
        for (int x = 0; x < 5; x++) {
            for (int y = 0; y < 6; y++) {
                Tile tile = new Tile(game, spec().getTileType("model.tile.plains"), x, y);
                tiles[x][y] = tile;
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

        MapBuilder builder = new MapBuilder(game);
        Map map = builder.setDimensions(10, 15).build();

        { // Even case
            Iterator<Position> i = map.getAdjacentIterator(map.getTile(4, 8).getPosition());

            List<Position> shouldBe = new ArrayList<Position>();
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

            List<Position> shouldBe = new ArrayList<Position>();
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

            List<Position> shouldBe = new ArrayList<Position>();
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

    public void testRandomDirection() {
        Game game = getStandardGame();
        MapBuilder builder = new MapBuilder(game);
        Map map = builder.setDimensions(10, 15).build();
        
        Direction[] dirs = map.getRandomDirectionArray();
        assertNotNull(dirs);
    }
}
