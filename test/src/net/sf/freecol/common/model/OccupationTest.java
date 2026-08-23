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

package net.sf.freecol.common.model;

import java.util.ArrayList;
import java.util.List;

import net.sf.freecol.common.util.LogBuilder;
import net.sf.freecol.server.model.ServerUnit;
import net.sf.freecol.util.test.FreeColTestCase;

public class OccupationTest extends FreeColTestCase {

    public void testOccupationConstructor() {
        Game game = getGame();
        game.setMap(getTestMap());
        Colony colony = createStandardColony(1);
        WorkLocation wl = colony.getColonyTile(colony.getTile());
        GoodsType food = spec().getGoodsType("model.goods.food");

        Occupation occupation = new Occupation(wl, null, food);

        assertEquals("WorkLocation should be correctly assigned", wl, occupation.workLocation);
        assertEquals("WorkType should be correctly assigned", food, occupation.workType);
    }

    public void testInstall() {
        Game game = getGame();
        game.setMap(getTestMap());
        Colony colony = createStandardColony(1);
        Unit unit = colony.getUnitList().get(0);

        WorkLocation targetLocation = colony.getBuilding(spec().getBuildingType("model.building.townHall"));
        GoodsType bells = spec().getGoodsType("model.goods.bells");

        Occupation occupation = new Occupation(targetLocation, null, bells);

        boolean success = occupation.install(unit);

        assertTrue("Installation should return true", success);
        assertEquals("Unit should have moved to the new location", targetLocation, unit.getLocation());
        assertEquals("Unit work type should have changed", bells, unit.getWorkType());
    }

    public void testImprove() {
        Game game = getStandardGame();
        TileType plains = spec().getTileType("model.tile.plains");
        game.setMap(getTestMap(plains));

        Colony colony = createStandardColony(1);
        Unit unit = colony.getUnitList().get(0);

        Tile tile = colony.getTile().getNeighbourOrNull(Direction.N);
        tile.setOwner(colony.getOwner());

        WorkLocation wl = colony.getColonyTile(tile);
        GoodsType grain = spec().getGoodsType("model.goods.grain");
        
        List<GoodsType> workTypes = List.of(grain);

        Occupation occupation = new Occupation(null, null, null);
        LogBuilder lb = new LogBuilder(256);

        int amount = occupation.improve(unit, wl, 0, workTypes, lb);

        assertTrue("Should find positive production. Log: " + lb, amount > 0);
        assertEquals("Occupation WorkLocation should be updated", wl, occupation.workLocation);
        assertEquals("Occupation WorkType should be Grain", grain, occupation.workType);
        assertNotNull("Production type should be set", occupation.productionType);
    }

    public void testImprove_NoProductionPossible() {
        Game game = getStandardGame();
        game.setMap(getTestMap());

        Colony colony = createStandardColony(1);
        Unit unit = colony.getUnitList().get(0);

        Tile tile = colony.getTile().getNeighbourOrNull(Direction.N);
        tile.setType(spec().getTileType("model.tile.ocean"));
        tile.setOwner(colony.getOwner());

        WorkLocation wl = colony.getColonyTile(tile);

        GoodsType grain = spec().getGoodsType("model.goods.grain");
        List<GoodsType> workTypes = new ArrayList<>();
        workTypes.add(grain);

        Occupation occupation = new Occupation(null, null, null);
        LogBuilder lb = new LogBuilder(256);

        int amount = occupation.improve(unit, wl, 0, workTypes, lb);

        assertEquals("No production should be possible", 0, amount);
        assertNull("WorkLocation should remain unchanged", occupation.workLocation);
        assertNull("WorkType should remain unchanged", occupation.workType);
    }

    public void testImprove_RespectsBestAmount() {
        Game game = getStandardGame();
        game.setMap(getTestMap());

        Colony colony = createStandardColony(1);
        Unit unit = colony.getUnitList().get(0);

        Tile tile = colony.getTile().getNeighbourOrNull(Direction.N);
        tile.setOwner(colony.getOwner());
        WorkLocation wl = colony.getColonyTile(tile);

        GoodsType grain = spec().getGoodsType("model.goods.grain");
        List<GoodsType> workTypes = List.of(grain);

        Occupation occupation = new Occupation(null, null, null);
        LogBuilder lb = new LogBuilder(256);

        int amount = occupation.improve(unit, wl, 10, workTypes, lb);

        assertEquals("Existing bestAmount should not be overridden", 10, amount);
        assertNull("WorkLocation should not be updated", occupation.workLocation);
    }

    public void testImprove_SelectsBestAmongMultipleGoods() {
        Game game = getStandardGame();
        game.setMap(getTestMap());

        Colony colony = createStandardColony(1);
        Unit unit = colony.getUnitList().get(0);

        Tile tile = colony.getTile().getNeighbourOrNull(Direction.N);
        tile.setOwner(colony.getOwner());
        WorkLocation wl = colony.getColonyTile(tile);

        GoodsType grain = spec().getGoodsType("model.goods.grain");
        GoodsType food = spec().getGoodsType("model.goods.food");

        List<GoodsType> workTypes = new ArrayList<>();
        workTypes.add(grain);
        workTypes.add(food);

        Occupation occupation = new Occupation(null, null, null);
        LogBuilder lb = new LogBuilder(256);

        int amount = occupation.improve(unit, wl, 0, workTypes, lb);

        assertTrue("Should produce something", amount > 0);
        assertEquals("WorkLocation should be updated", wl, occupation.workLocation);
        assertTrue("WorkType should be one of the requested types",
            occupation.workType == grain || occupation.workType == food);
    }

    public void testImprove_RespectsExistingBestAmount() {
        Game game = getStandardGame();
        game.setMap(getTestMap());

        Colony colony = createStandardColony(1);
        Unit unit = colony.getUnitList().get(0);

        Tile tile = colony.getTile().getNeighbourOrNull(Direction.N);
        tile.setOwner(colony.getOwner());
        WorkLocation wl = colony.getColonyTile(tile);

        GoodsType grain = spec().getGoodsType("model.goods.grain");
        List<GoodsType> workTypes = new ArrayList<>();
        workTypes.add(grain);

        Occupation occupation = new Occupation(null, null, null);
        LogBuilder lb = new LogBuilder(256);

        int amount = occupation.improve(unit, wl, 10, workTypes, lb);

        assertEquals("Existing bestAmount should not be overridden", 10, amount);
        assertNull("Occupation should not be updated", occupation.workLocation);
    }

    public void testImprove_NullProductionType() {
        Game game = getStandardGame();
        game.setMap(getTestMap());

        Colony colony = createStandardColony(1);
        Unit unit = colony.getUnitList().get(0);

        Tile tile = colony.getTile().getNeighbourOrNull(Direction.N);
        tile.setOwner(colony.getOwner());
        WorkLocation wl = colony.getColonyTile(tile);

        wl.setProductionType(null);

        GoodsType grain = spec().getGoodsType("model.goods.grain");
        List<GoodsType> workTypes = new ArrayList<>();
        workTypes.add(grain);

        Occupation occupation = new Occupation(null, null, null);
        LogBuilder lb = new LogBuilder(256);

        int amount = occupation.improve(unit, wl, 0, workTypes, lb);

        assertTrue("Should still find a valid production", amount > 0);
        assertEquals("WorkLocation should be updated", wl, occupation.workLocation);
    }

    public void testImprove_CannotAddUnit() {
        Game game = getStandardGame();
        game.setMap(getTestMap());

        Colony colony = createStandardColony(1);

        Unit blocker = colony.getUnitList().get(0);

        UnitType freeColonist = spec().getUnitType("model.unit.freeColonist");
        Unit unit = new ServerUnit(game, colony.getTile(), colony.getOwner(), freeColonist);

        ColonyTile wl = colony.getColonyTile(colony.getTile());

        blocker.setLocation(wl);

        assertFalse("Sanity check: WL should reject second unit", wl.canAdd(unit));

        GoodsType grain = spec().getGoodsType("model.goods.grain");
        List<GoodsType> workTypes = List.of(grain);

        Occupation occupation = new Occupation(null, null, null);
        LogBuilder lb = new LogBuilder(256);

        int amount = occupation.improve(unit, wl, 0, workTypes, lb);

        assertEquals("Should not change bestAmount", 0, amount);
        assertNull("WorkLocation should not be updated", occupation.workLocation);
    }

    public void testToString() {
        Game game = getGame();
        game.setMap(getTestMap());
        Colony colony = createStandardColony(1);
        WorkLocation wl = colony.getColonyTile(colony.getTile());
        GoodsType grain = spec().getGoodsType("model.goods.grain");

        Occupation occupation = new Occupation(wl, null, grain);
        String result = occupation.toString();

        assertNotNull("toString should not be null", result);
        assertTrue("String representation should contain the occupation keyword",
            result.contains("Occupation"));
    }

    public void testImprove_CappedByMinInput() {
        Game game = getStandardGame();
        game.setMap(getTestMap());
        
        Colony colony = createStandardColony(1);
        BuildingType weaverHouseType = spec().getBuildingType("model.building.weaverHouse");
        Building weaverHouse = colony.getBuilding(weaverHouseType);
        
        GoodsType cotton = spec().getGoodsType("model.goods.cotton");
        GoodsType cloth = spec().getGoodsType("model.goods.cloth");
        Unit unit = colony.getUnitList().get(0);
        
        colony.addGoods(cotton, 2);
        
        Occupation occupation = new Occupation(null, null, null);
        LogBuilder lb = new LogBuilder(256);

        int amount = occupation.improve(unit, weaverHouse, 0, List.of(cloth), lb);

        assertEquals("Production should be capped by available raw materials (Cotton)", 2, amount);
        assertEquals("WorkType should be set to Cloth", cloth, occupation.workType);
        assertEquals("WorkLocation should be set to Weaver's House", weaverHouse, occupation.workLocation);
    }
}
