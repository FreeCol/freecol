/**
 *  Copyright (C) 2002-2019  The FreeCol Team
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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.io.FreeColDirectories;
import net.sf.freecol.common.io.FreeColSavegameFile;
import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Nation;
import net.sf.freecol.common.model.NationOptions;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Region;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Turn;
import net.sf.freecol.common.option.MapGeneratorOptions;
import net.sf.freecol.common.util.LogBuilder;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerGame;
import net.sf.freecol.server.model.ServerPlayer;
import net.sf.freecol.server.model.ServerUnit;
import net.sf.freecol.util.test.FreeColTestCase;


public class MapGeneratorTest extends FreeColTestCase {

    public void testWithNoIndians() {
        spec().setFile(MapGeneratorOptions.IMPORT_FILE, null);

        Game g = getStandardGame();
        g.setNationOptions(new NationOptions(spec()));

        // A new game has no map
        assertNull("No new map", g.getMap());

        MapGenerator gen = new SimpleMapGenerator(new Random(1));

        for (Nation n : spec().getNations()) {
            if (n.getType().isEuropean() && !n.getType().isREF()
                && !n.isUnknownEnemy()) {
                g.addPlayer(new ServerPlayer(g, false, n));
            }
        }

        gen.generateMap(g, null, new LogBuilder(-1));
        assertNotNull("New map", g.getMap());
    }

    public void testSinglePlayerOnSmallMap() {
        spec().setFile(MapGeneratorOptions.IMPORT_FILE, null);

        Game g = getStandardGame();
        g.setNationOptions(new NationOptions(spec()));

        // A new game has no map
        assertNull("No new map", g.getMap());

        MapGenerator gen = new SimpleMapGenerator(new Random(1));
        Nation nation = spec().getNation("model.nation.dutch");

        g.addPlayer(new ServerPlayer(g, false, nation));

        gen.generateMap(g, null, new LogBuilder(-1));
        assertNotNull("New map", g.getMap());

        // Check that the map is created at all
        assertNotNull(g.getMap());

        assertEquals(g.getMapGeneratorOptions().getInteger(MapGeneratorOptions.MAP_WIDTH),
                     g.getMap().getWidth());
        assertEquals(g.getMapGeneratorOptions().getInteger(MapGeneratorOptions.MAP_HEIGHT),
                     g.getMap().getHeight());

    }

    public void testMapGenerator() {
        spec().setFile(MapGeneratorOptions.IMPORT_FILE, null);

        Game g = getStandardGame();
        g.setNationOptions(new NationOptions(spec()));

        // A new game has no map
        assertNull("No new map", g.getMap());

        // Apply the difficulty level
        //spec().applyDifficultyLevel("model.difficulty.medium");

        MapGenerator gen = new SimpleMapGenerator(new Random(1));
        gen.generateMap(g, null, new LogBuilder(-1));
        assertNotNull("New map", g.getMap());

        // Map of correct size?
        Map m = g.getMap();
        assertEquals(m.getWidth(),
                     g.getMapGeneratorOptions().getInteger(MapGeneratorOptions.MAP_WIDTH));
        assertEquals(m.getHeight(),
                     g.getMapGeneratorOptions().getInteger(MapGeneratorOptions.MAP_HEIGHT));

        // Sufficient land?
        int total = m.getWidth() * m.getHeight();
        int land = m.getTileSet(Tile::isLand).size();
        // Land Mass requirement fulfilled?
        assertTrue(100 * land / total >= g.getMapGeneratorOptions()
                   .getInteger(MapGeneratorOptions.LAND_MASS));

        // Does the wholeMapIterator visit all fields?
        assertEquals(total,
                     g.getMapGeneratorOptions().getInteger(MapGeneratorOptions.MAP_WIDTH)
                     * g.getMapGeneratorOptions().getInteger(MapGeneratorOptions.MAP_HEIGHT));
    }

    /**
     * Make sure that each tribe has exactly one capital
     *
     */
    public void testIndianCapital() {
        spec().setFile(MapGeneratorOptions.IMPORT_FILE, null);

        Game g = getStandardGame();
        g.setNationOptions(new NationOptions(spec()));

        MapGenerator gen = new SimpleMapGenerator(new Random(1));

        List<Player> players = new ArrayList<>();
        for (Nation n : spec().getNations()) {
            if (n.isUnknownEnemy()) continue;
            Player p = new ServerPlayer(g, false, n);
            p.setAI(!n.getType().isEuropean() || n.getType().isREF());
            g.addPlayer(p);
            players.add(p);
        }

        gen.generateMap(g, null, new LogBuilder(-1));

        // Check that the map is created at all
        assertNotNull(g.getMap());

        for (Player p : players) {
            if (!p.isIndian())
                continue;

            // Check that every indian player has exactly one capital if s/he
            // has at least one settlement.
            int settlements = 0;
            int capitals = 0;
            for (IndianSettlement s : p.getIndianSettlementList()) {
                settlements++;
                if (s.isCapital()) capitals++;
            }
            if (settlements > 0) assertEquals(1, capitals);
        }
    }

    /**
     * Make sure we can import all distributed maps.
     */
    public void testImportMap() {
        Game game = getStandardGame();
        final Specification spec = game.getSpecification();

        MapGenerator gen = new SimpleMapGenerator(new Random(1));
        Map importMap = null;
        for (File importFile : FreeColDirectories.getMapFileList()) {
            spec.setFile(MapGeneratorOptions.IMPORT_FILE, importFile);
            try {
                importMap = FreeColServer.readMap(importFile, spec);
            } catch (FreeColException|IOException|XMLStreamException ex) {
                fail("Map read of " + importFile.getName() + " failed: "
                    + ex.toString());
            }
            assertNotNull(gen.generateMap(game, importMap, new LogBuilder(-1)));
        }
        // Clear import file option
        spec.setFile(MapGeneratorOptions.IMPORT_FILE, null);
    }

    public void testRegions() {
        spec().setFile(MapGeneratorOptions.IMPORT_FILE, null);
        Game game = getStandardGame();
        MapGenerator gen = new SimpleMapGenerator(new Random(1));
        gen.generateMap(game, null, new LogBuilder(-1));
        
        Map map = game.getMap();
        Region pacific = map.getRegionByKey("model.region.pacific");
        assertNotNull(pacific);
        assertTrue(pacific.isPacific());
        assertEquals(pacific, pacific.getDiscoverableRegion());

        Region southPacific = map.getRegionByKey("model.region.southPacific");
        assertNotNull(southPacific);
        assertFalse(southPacific.getDiscoverable());
        assertTrue(southPacific.isPacific());
        assertEquals(pacific, southPacific.getParent());
        assertEquals(pacific, southPacific.getDiscoverableRegion());

        Player player = new Player(game, FreeColObject.ID_ATTRIBUTE_TAG);
        ServerUnit unit = new ServerUnit(game, null, player,
            spec().getUnitType("model.unit.caravel"));
        pacific.discover(player, unit, new Turn(1));

        assertFalse(pacific.getDiscoverable());
        assertNull(pacific.getDiscoverableRegion());
        assertFalse(southPacific.getDiscoverable());
        assertTrue(southPacific.isPacific());
        assertEquals(pacific, southPacific.getParent());
        assertNull(southPacific.getDiscoverableRegion());

        Region atlantic = map.getRegionByKey("model.region.atlantic");
        assertNotNull(atlantic);
        assertFalse(atlantic.isPacific());
        assertFalse(atlantic.getDiscoverable());
        assertNull(atlantic.getDiscoverableRegion());

        Region northAtlantic = map.getRegionByKey("model.region.northAtlantic");
        assertNotNull(northAtlantic);
        assertFalse(northAtlantic.isPacific());
        assertFalse(northAtlantic.getDiscoverable());
        assertNull(northAtlantic.getDiscoverableRegion());
    }
}
