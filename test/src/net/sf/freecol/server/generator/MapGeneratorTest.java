/**
 *  Copyright (C) 2002-2008  The FreeCol Team
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

package net.sf.freecol.server.generator;

import java.io.File;
import java.util.Iterator;
import java.util.Vector;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Nation;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Region;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Turn;
import net.sf.freecol.common.model.Map.Position;
import net.sf.freecol.common.option.FileOption;
import net.sf.freecol.common.option.RangeOption;
import net.sf.freecol.server.model.ServerGame;
import net.sf.freecol.util.test.FreeColTestCase;
import net.sf.freecol.util.test.MockModelController;

public class MapGeneratorTest extends FreeColTestCase {

    public void testWithNoIndians() {
        MockModelController mmc = new MockModelController();
        Game g = new ServerGame(mmc);
        Specification s = FreeCol.getSpecification();

        // A new game does not have a map yet
        assertEquals(null, g.getMap());

        IMapGenerator gen = new MapGenerator(mmc.getPseudoRandom());

        for (Nation n : s.getNations()) {
            if (n.getType().isEuropean() && !n.getType().isREF()) {
                g.addPlayer(new Player(g, n.getType().getNameKey(), false, n));
            }
        }

        try {
            gen.createMap(g);
        } catch (FreeColException e) {
            fail();
        }

        // Check that the map is created at all
        assertNotNull(g.getMap());
    }

    public void testSinglePlayerOnSmallMap() {
        MockModelController mmc = new MockModelController();
        Game g = new ServerGame(mmc);

        // A new game does not have a map yet
        assertEquals(null, g.getMap());

        IMapGenerator gen = new MapGenerator(mmc.getPseudoRandom());
        RangeOption mapSize = (RangeOption) gen.getMapGeneratorOptions().getObject(MapGeneratorOptions.MAP_SIZE);
        mapSize.setValue(MapGeneratorOptions.MAP_SIZE_SMALL);

        Nation nation = FreeCol.getSpecification().getNation(
                                                             "model.nation.dutch");

        g.addPlayer(new Player(g, nation.getType().getNameKey(), false, nation));

        try {
            gen.createMap(g);
        } catch (FreeColException e) {
            fail();
        }

        // Check that the map is created at all
        assertNotNull(g.getMap());

        assertEquals(gen.getMapGeneratorOptions().getWidth(), g.getMap()
                     .getWidth());
        assertEquals(gen.getMapGeneratorOptions().getHeight(), g.getMap()
                     .getHeight());

    }

    public void testMapGenerator() {
        MockModelController mmc = new MockModelController();
        Game g = new ServerGame(mmc);

        // A new game does not have a map yet
        assertEquals(null, g.getMap());

        IMapGenerator gen = new MapGenerator(mmc.getPseudoRandom());

        // Apply the difficulty level
        Specification.getSpecification().applyDifficultyLevel("model.difficulty.medium");

        Vector<Player> players = new Vector<Player>();

        for (Nation n : FreeCol.getSpecification().getNations()) {
            Player p;
            if (n.getType().isEuropean() && !n.getType().isREF()){
                p = new Player(g, n.getType().getNameKey(), false, n);
            } else {
                p = new Player(g, n.getType().getNameKey(), false, true, n);
            }
            g.addPlayer(p);
            players.add(p);
        }

        try {
            gen.createMap(g);
        } catch (FreeColException e) {
            fail();
        }
		
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
        assertTrue(100 * land / total >= gen.getMapGeneratorOptions()
                   .getLandMass());

        // Does the wholeMapIterator visit all fields?
        assertEquals(gen.getMapGeneratorOptions().getWidth()
                     * gen.getMapGeneratorOptions().getHeight(), total);
    }

    /**
     * Make sure that each tribe has exactly one capital
     * 
     */
    public void testIndianCapital() {
        MockModelController mmc = new MockModelController();
        Game g = new ServerGame(mmc);

        IMapGenerator gen = new MapGenerator(mmc.getPseudoRandom());

        Vector<Player> players = new Vector<Player>();

        for (Nation n : FreeCol.getSpecification().getNations()) {
            Player p;
            if (n.getType().isEuropean() && !n.getType().isREF()){
                p = new Player(g, n.getType().getNameKey(), false, n);
            } else {
                p = new Player(g, n.getType().getNameKey(), false, true, n);
            }
            g.addPlayer(p);
            players.add(p);
        }

        try {
            gen.createMap(g);
        } catch (FreeColException e) {
            fail();
        }

        // Check that the map is created at all
        assertNotNull(g.getMap());

        for (Player p : players) {
            if (!p.isIndian())
                continue;

            // Check that every indian player has exactly one capital if s/he
            // has at least one settlement.
            int settlements = 0;
            int capitals = 0;
            for (IndianSettlement s : p.getIndianSettlements()) {
                settlements++;
                if (s.isCapital())
                    capitals++;
            }
            if (settlements > 0) 
                assertEquals(1, capitals);
        }
    }
    
    public void testImportMap() {
        /**
         * Make sure we can import all distributed maps.
         */
        MockModelController mmc = new MockModelController();
        Game g = new ServerGame(mmc);
        IMapGenerator gen = new MapGenerator(mmc.getPseudoRandom());
        File mapDir = new File("data/maps/");
        for (File importFile : mapDir.listFiles()) {
            if (importFile.getName().endsWith(".fsg")) {
                gen.getMapGeneratorOptions().setFile(MapGeneratorOptions.IMPORT_FILE, importFile);
                try {
                    gen.createMap(g);
                } catch (FreeColException e) {
                    fail("Failed to import file " + importFile.getName());
                }
            }
        }
    }

    public void testRegions() {
        // Reset import file option value (set by previous tests)
        ((FileOption) Specification.getSpecification().getOption(MapGeneratorOptions.IMPORT_FILE)).setValue(null);

        MockModelController mmc = new MockModelController();
        Game game = new ServerGame(mmc);
        IMapGenerator gen = new MapGenerator(mmc.getPseudoRandom());
        try {
            gen.createMap(game);
        } catch (FreeColException e) {
            fail();
        }

        Map map = game.getMap();
        Region pacific = map.getRegion("model.region.pacific");
        assertNotNull(pacific);
        assertTrue(pacific.isPacific());
        assertEquals(pacific, pacific.getDiscoverableRegion());

        Region southPacific = map.getRegion("model.region.southPacific");
        assertNotNull(southPacific);
        assertFalse(southPacific.isDiscoverable());
        assertTrue(southPacific.isPacific());
        assertEquals(pacific, southPacific.getParent());
        assertEquals(pacific, southPacific.getDiscoverableRegion());

        pacific.discover(new Player(game, "id"), new Turn(1), "someName");

        assertFalse(pacific.isDiscoverable());
        assertNull(pacific.getDiscoverableRegion());
        assertFalse(southPacific.isDiscoverable());
        assertTrue(southPacific.isPacific());
        assertEquals(pacific, southPacific.getParent());
        assertNull(southPacific.getDiscoverableRegion());

    }        

}

