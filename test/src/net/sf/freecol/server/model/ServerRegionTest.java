/**
 *  Copyright (C) 2002-2024  The FreeCol Team
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

package net.sf.freecol.server.model;

import java.util.List;

import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Region;
import net.sf.freecol.common.util.LogBuilder;
import net.sf.freecol.server.ServerTestHelper;
import net.sf.freecol.util.test.FreeColTestCase;

public class ServerRegionTest extends FreeColTestCase {

    public void testRequireFixedRegionsCreatesPolarRegions() {
        Game game = ServerTestHelper.startServerGame(getTestMap());

        Map map = game.getMap();

        ServerRegion.requireFixedRegions(
            map,
            new LogBuilder(0));

        assertNotNull(
            map.getRegionByKey("model.region.arctic"));

        assertNotNull(
            map.getRegionByKey("model.region.antarctic"));
    }

    public void testRequireFixedRegionsCreatesGeographicRegions() {
        Game game = ServerTestHelper.startServerGame(getTestMap());

        Map map = game.getMap();

        ServerRegion.requireFixedRegions(
            map,
            new LogBuilder(0));

        assertNotNull(map.getRegionByKey("model.region.northWest"));
        assertNotNull(map.getRegionByKey("model.region.north"));
        assertNotNull(map.getRegionByKey("model.region.northEast"));

        assertNotNull(map.getRegionByKey("model.region.west"));
        assertNotNull(map.getRegionByKey("model.region.center"));
        assertNotNull(map.getRegionByKey("model.region.east"));

        assertNotNull(map.getRegionByKey("model.region.southWest"));
        assertNotNull(map.getRegionByKey("model.region.south"));
        assertNotNull(map.getRegionByKey("model.region.southEast"));
    }

    public void testRequireFixedRegionsCreatesOceanRegions() {
        Game game = ServerTestHelper.startServerGame(getTestMap());

        Map map = game.getMap();

        ServerRegion.requireFixedRegions(
            map,
            new LogBuilder(0));

        assertNotNull(map.getRegionByKey("model.region.pacific"));
        assertNotNull(map.getRegionByKey("model.region.northPacific"));
        assertNotNull(map.getRegionByKey("model.region.southPacific"));

        assertNotNull(map.getRegionByKey("model.region.atlantic"));
        assertNotNull(map.getRegionByKey("model.region.northAtlantic"));
        assertNotNull(map.getRegionByKey("model.region.southAtlantic"));
    }

    public void testPacificIsDiscoverable() {
        Game game = ServerTestHelper.startServerGame(getTestMap());

        Map map = game.getMap();

        ServerRegion.requireFixedRegions(
            map,
            new LogBuilder(0));

        Region pacific =
            map.getRegionByKey("model.region.pacific");

        assertNotNull(pacific);
        assertTrue(pacific.getDiscoverable());
    }

    public void testNorthPacificIsNotDiscoverable() {
        Game game = ServerTestHelper.startServerGame(getTestMap());

        Map map = game.getMap();

        ServerRegion.requireFixedRegions(
            map,
            new LogBuilder(0));

        Region northPacific =
            map.getRegionByKey("model.region.northPacific");

        assertNotNull(northPacific);
        assertFalse(northPacific.getDiscoverable());
    }

    public void testRequireFixedRegionsIsIdempotent() {
        Game game = ServerTestHelper.startServerGame(getTestMap());

        Map map = game.getMap();

        ServerRegion.requireFixedRegions(
            map,
            new LogBuilder(0));

        int firstCount =
            map.getRegions().size();

        ServerRegion.requireFixedRegions(
            map,
            new LogBuilder(0));

        int secondCount =
            map.getRegions().size();

        assertEquals(firstCount, secondCount);
    }

    public void testPacificHierarchy() {
        Game game = ServerTestHelper.startServerGame(getTestMap());
        Map map = game.getMap();

        ServerRegion.requireFixedRegions(map, new LogBuilder(0));

        Region pacific = map.getRegionByKey("model.region.pacific");
        Region northPacific = map.getRegionByKey("model.region.northPacific");
        Region southPacific = map.getRegionByKey("model.region.southPacific");

        assertEquals(pacific, northPacific.getParent());
        assertEquals(pacific, southPacific.getParent());

        assertTrue(pacific.getChildren().contains(northPacific));
        assertTrue(pacific.getChildren().contains(southPacific));
    }

    public void testPacificScoreValue() {
        Game game = ServerTestHelper.startServerGame(getTestMap());
        Map map = game.getMap();

        ServerRegion.requireFixedRegions(map, new LogBuilder(0));

        Region pacific = map.getRegionByKey("model.region.pacific");

        assertEquals(ServerRegion.PACIFIC_SCORE_VALUE,
                     pacific.getScoreValue());
    }

    public void testGeographicRegionsAreLandRegions() {
        Game game = ServerTestHelper.startServerGame(getTestMap());
        Map map = game.getMap();

        ServerRegion.requireFixedRegions(map, new LogBuilder(0));

        assertEquals(Region.RegionType.LAND,
            map.getRegionByKey("model.region.center").getType());

        assertEquals(Region.RegionType.LAND,
            map.getRegionByKey("model.region.northWest").getType());

        assertEquals(Region.RegionType.LAND,
            map.getRegionByKey("model.region.southEast").getType());
    }

    public void testOceanRegionsAreOceanRegions() {
        Game game = ServerTestHelper.startServerGame(getTestMap());
        Map map = game.getMap();

        ServerRegion.requireFixedRegions(map, new LogBuilder(0));

        assertEquals(Region.RegionType.OCEAN,
            map.getRegionByKey("model.region.pacific").getType());

        assertEquals(Region.RegionType.OCEAN,
            map.getRegionByKey("model.region.atlantic").getType());
    }

    public void testGeographicRegionsHaveBounds() {
        Game game = ServerTestHelper.startServerGame(getTestMap());
        Map map = game.getMap();

        ServerRegion.requireFixedRegions(map, new LogBuilder(0));

        ServerRegion center =
            (ServerRegion) map.getRegionByKey("model.region.center");

        assertTrue(center.getBounds().width >= 0);
        assertTrue(center.getBounds().height >= 0);
    }

    public void testRequireFixedRegionsReturnsExpectedRegions() {
        Game game = ServerTestHelper.startServerGame(getTestMap());
        Map map = game.getMap();

        List<ServerRegion> result =
            ServerRegion.requireFixedRegions(map, new LogBuilder(0));

        assertFalse(result.isEmpty());

        assertTrue(result.stream()
            .anyMatch(r -> "model.region.pacific".equals(r.getKey())));
    }

    public void testAtlanticHierarchy() {
        Game game = ServerTestHelper.startServerGame(getTestMap());
        Map map = game.getMap();

        ServerRegion.requireFixedRegions(map, new LogBuilder(0));

        Region atlantic = map.getRegionByKey("model.region.atlantic");
        Region northAtlantic = map.getRegionByKey("model.region.northAtlantic");
        Region southAtlantic = map.getRegionByKey("model.region.southAtlantic");

        assertEquals(atlantic, northAtlantic.getParent());
        assertEquals(atlantic, southAtlantic.getParent());

        assertTrue(atlantic.getChildren().contains(northAtlantic));
        assertTrue(atlantic.getChildren().contains(southAtlantic));
    }

    public void testCenterRegionHasPositiveBounds() {
        Game game = ServerTestHelper.startServerGame(getTestMap());
        Map map = game.getMap();

        ServerRegion.requireFixedRegions(map, new LogBuilder(0));

        ServerRegion center =
            (ServerRegion) map.getRegionByKey("model.region.center");

        assertTrue(center.getBounds().width > 0);
        assertTrue(center.getBounds().height > 0);
    }

    public void testNorthWestRegionHasBounds() {
        Game game = ServerTestHelper.startServerGame(getTestMap());
        Map map = game.getMap();

        ServerRegion.requireFixedRegions(map, new LogBuilder(0));

        ServerRegion northWest =
            (ServerRegion) map.getRegionByKey("model.region.northWest");

        assertTrue(northWest.getBounds().width >= 0);
        assertTrue(northWest.getBounds().height >= 0);
    }

    public void testSouthEastRegionHasBounds() {
        Game game = ServerTestHelper.startServerGame(getTestMap());
        Map map = game.getMap();

        ServerRegion.requireFixedRegions(map, new LogBuilder(0));

        ServerRegion southEast =
            (ServerRegion) map.getRegionByKey("model.region.southEast");

        assertTrue(southEast.getBounds().width >= 0);
        assertTrue(southEast.getBounds().height >= 0);
    }

    public void testGeographicRegionIsGeographic() {
        Game game = ServerTestHelper.startServerGame(getTestMap());
        Map map = game.getMap();

        ServerRegion.requireFixedRegions(map, new LogBuilder(0));

        ServerRegion center =
            (ServerRegion) map.getRegionByKey("model.region.center");

        assertTrue(center.isGeographic());
    }

    public void testOceanRegionIsNotGeographic() {
        Game game = ServerTestHelper.startServerGame(getTestMap());
        Map map = game.getMap();

        ServerRegion.requireFixedRegions(map, new LogBuilder(0));

        ServerRegion pacific =
            (ServerRegion) map.getRegionByKey("model.region.pacific");

        assertFalse(pacific.isGeographic());
    }

    public void testSouthPacificIsNotDiscoverable() {
        Game game = ServerTestHelper.startServerGame(getTestMap());
        Map map = game.getMap();

        ServerRegion.requireFixedRegions(map, new LogBuilder(0));

        assertFalse(
            map.getRegionByKey("model.region.southPacific")
                .getDiscoverable());
    }

    public void testAtlanticIsNotDiscoverable() {
        Game game = ServerTestHelper.startServerGame(getTestMap());
        Map map = game.getMap();

        ServerRegion.requireFixedRegions(map, new LogBuilder(0));

        assertFalse(
            map.getRegionByKey("model.region.atlantic")
                .getDiscoverable());
    }

    public void testAtlanticScoreValueIsZero() {
        Game game = ServerTestHelper.startServerGame(getTestMap());
        Map map = game.getMap();

        ServerRegion.requireFixedRegions(map, new LogBuilder(0));

        assertEquals(
            0,
            map.getRegionByKey("model.region.atlantic")
                .getScoreValue());
    }

    public void testNorthPacificScoreValueIsZero() {
        Game game = ServerTestHelper.startServerGame(getTestMap());
        Map map = game.getMap();

        ServerRegion.requireFixedRegions(map, new LogBuilder(0));

        assertEquals(
            0,
            map.getRegionByKey("model.region.northPacific")
                .getScoreValue());
    }

    public void testRequireFixedRegionsPreservesPacificRegion() {
        Game game = ServerTestHelper.startServerGame(getTestMap());
        Map map = game.getMap();

        ServerRegion.requireFixedRegions(map, new LogBuilder(0));

        Region first =
            map.getRegionByKey("model.region.pacific");

        ServerRegion.requireFixedRegions(map, new LogBuilder(0));

        Region second =
            map.getRegionByKey("model.region.pacific");

        assertSame(first, second);
    }

    public void testAtlanticIsNotGeographic() {
        Game game = ServerTestHelper.startServerGame(getTestMap());
        Map map = game.getMap();

        ServerRegion.requireFixedRegions(map, new LogBuilder(0));

        ServerRegion atlantic =
            (ServerRegion) map.getRegionByKey("model.region.atlantic");

        assertFalse(atlantic.isGeographic());
    }

    public void testPolarRegionsAreLandRegions() {
        Game game = ServerTestHelper.startServerGame(getTestMap());
        Map map = game.getMap();

        ServerRegion.requireFixedRegions(map, new LogBuilder(0));

        assertEquals(
            Region.RegionType.LAND,
            map.getRegionByKey("model.region.arctic").getType());

        assertEquals(
            Region.RegionType.LAND,
            map.getRegionByKey("model.region.antarctic").getType());
    }
}
