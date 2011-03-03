/**
 *  Copyright (C) 2002-2011  The FreeCol Team
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

import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.server.model.ServerUnit;
import net.sf.freecol.util.test.FreeColTestCase;


public class ColonyTest extends FreeColTestCase {
    BuildingType depotType = spec().getBuildingType("model.building.depot");
    BuildingType warehouseType = spec().getBuildingType("model.building.warehouse");
    BuildingType warehouseExpansionType = spec().getBuildingType("model.building.warehouseExpansion");
    BuildingType churchType = spec().getBuildingType("model.building.chapel");
    BuildingType townHallType = spec().getBuildingType("model.building.townHall");
    BuildingType carpenterHouseType =  spec().getBuildingType("model.building.carpenterHouse");
    BuildingType lumberMillType =  spec().getBuildingType("model.building.lumberMill");
    UnitType wagonTrainType = spec().getUnitType("model.unit.wagonTrain");
    GoodsType hammerGoodsType = spec().getGoodsType("model.goods.hammers");
    GoodsType lumberGoodsType = spec().getGoodsType("model.goods.lumber");

    public void testCurrentlyBuilding() {
        Game game = getGame();
    	game.setMap(getTestMap(true));

    	Colony colony = getStandardColony();

    	colony.setCurrentlyBuilding(warehouseType);
    	assertEquals("Colony should be building a warehouse",warehouseType,colony.getCurrentlyBuilding());

        colony.setCurrentlyBuilding(churchType);
        assertEquals("Colony should be building a church",churchType,colony.getCurrentlyBuilding());
    }

    public void testBuildQueueDoesNotAcceptBuildingDoubles() {
        Game game = getGame();
        game.setMap(getTestMap(true));

        Colony colony = getStandardColony();

        colony.setCurrentlyBuilding(warehouseType);
        assertEquals("Building queue should have 1 entry", 1, colony.getBuildQueue().size());

        colony.setCurrentlyBuilding(warehouseType);
        assertEquals("Building queue should still have 1 entry", 1, colony.getBuildQueue().size());

        colony.setCurrentlyBuilding(churchType);
        assertEquals("Building queue should have 2 entries", 2, colony.getBuildQueue().size());

        colony.setCurrentlyBuilding(warehouseType);
        assertEquals("Building queue should still have 2 entries", 2, colony.getBuildQueue().size());
    }

    public void testBuildQueueAcceptsUnitDoubles() {
        Game game = getGame();
        game.setMap(getTestMap(true));

        Colony colony = getStandardColony();

        colony.setCurrentlyBuilding(wagonTrainType);
        // default item will be added to new colony's build queue
        assertEquals("Building queue should have 2 entry", 2, colony.getBuildQueue().size());

        colony.setCurrentlyBuilding(wagonTrainType);
        assertEquals("Building queue should have 3 entries", 3, colony.getBuildQueue().size());
    }

    public void testOccupationWithFood() {

        int population = 3;
        GoodsType food = spec().getGoodsType("model.goods.grain");
        GoodsType cotton = spec().getGoodsType("model.goods.cotton");
        UnitType freeColonist = spec().getUnitType("model.unit.freeColonist");
        UnitType cottonPlanter = spec().getUnitType("model.unit.masterCottonPlanter");
        UnitType statesman = spec().getUnitType("model.unit.elderStatesman");
        BuildingType townHall = spec().getBuildingType("model.building.townHall");

        Game game = getGame();
        game.setMap(getTestMap(true));
        Colony colony = getStandardColony(population);

        assertTrue("colony produces less food than it consumes",
                   colony.getFoodProduction() > colony.getFoodConsumption() +
                   freeColonist.getConsumptionOf(food));

        // colonist with no skill or experience will produce food
        Unit colonist = new ServerUnit(game, colony.getTile(),
                                       colony.getOwner(), freeColonist,
                                       UnitState.ACTIVE);
        nonServerJoinColony(colonist, colony);
        assertTrue(colonist.getLocation() instanceof ColonyTile);
        assertEquals(food, colonist.getWorkType());

        // colonist with experience in producing farmed goods will
        // produce that type of goods
        colonist.putOutsideColony();
        colonist.setWorkType(cotton);
        colonist.modifyExperience(100);
        nonServerJoinColony(colonist, colony);
        assertTrue(colonist.getLocation() instanceof ColonyTile);
        assertEquals(cotton, colonist.getWorkType());

        // expert will produce expert goods
        colonist.putOutsideColony();
        colonist.setType(cottonPlanter);
        nonServerJoinColony(colonist, colony);
        assertTrue(colonist.getLocation() instanceof ColonyTile);
        assertEquals(cotton, colonist.getWorkType());

        // expert will produce expert goods
        colonist.putOutsideColony();
        colonist.setType(statesman);
        nonServerJoinColony(colonist, colony);
        assertTrue(colonist.getLocation() instanceof Building);
        assertEquals(townHall, ((Building) colonist.getLocation()).getType());

    }

    private Modifier createTeaPartyModifier(Turn turn) {
        Modifier template = spec().getModifiers("model.modifier.colonyGoodsParty").get(0);
        return Modifier.makeTimedModifier("model.goods.bells", template, turn);
    }

    public void testTeaParty() {
        Game game = getGame();
        game.setMap(getTestMap(true));
        Colony colony = getStandardColony(5);
        spec();

        colony.getFeatureContainer().addModifier(createTeaPartyModifier(game.getTurn()));
        colony.getFeatureContainer().addModifier(createTeaPartyModifier(game.getTurn()));
        colony.getFeatureContainer().addModifier(createTeaPartyModifier(game.getTurn()));

        int modifierCount = 0;
        for (Modifier existingModifier : colony.getFeatureContainer().getModifierSet("model.goods.bells")) {
            if (Specification.COLONY_GOODS_PARTY_SOURCE.equals(existingModifier.getSource())) {
                modifierCount++;
            }
        }
        assertEquals(1, modifierCount);

        Turn newTurn = new Turn(game.getTurn().getNumber() + 1);
        colony.getFeatureContainer().addModifier(createTeaPartyModifier(newTurn));

        modifierCount = 0;
        for (Modifier existingModifier : colony.getFeatureContainer().getModifierSet("model.goods.bells")) {
            if (Specification.COLONY_GOODS_PARTY_SOURCE.equals(existingModifier.getSource())) {
                modifierCount++;
            }
        }
        assertEquals(2, modifierCount);

    }

    public void testOccupationWithoutFood() {

        int population = 1;
        GoodsType bells = spec().getGoodsType("model.goods.bells");
        GoodsType cotton = spec().getGoodsType("model.goods.cotton");
        GoodsType cloth = spec().getGoodsType("model.goods.cloth");
        GoodsType food = spec().getPrimaryFoodType();
        BuildingType townHall = spec().getBuildingType("model.building.townHall");
        BuildingType weaversHouse = spec().getBuildingType("model.building.weaverHouse");

        UnitType freeColonist = spec().getUnitType("model.unit.freeColonist");
        assertEquals(2, freeColonist.getConsumptionOf(food));
        UnitType weaver = spec().getUnitType("model.unit.masterWeaver");
        assertEquals(2, weaver.getConsumptionOf(food));

        Game game = getGame();
        game.setMap(getTestMap(spec().getTileType("model.tile.arctic"), true));
        Colony colony = getStandardColony(population);

        assertTrue("colony produces more food than it consumes",
                   colony.getFoodProduction() < colony.getFoodConsumption() +
                   freeColonist.getConsumptionOf(food));

        // colonist produces bells because they require no input
        Unit colonist = new ServerUnit(game, colony.getTile(),
                                       colony.getOwner(),
                                       spec().getUnitType("model.unit.freeColonist"),
                                       UnitState.ACTIVE);
        nonServerJoinColony(colonist, colony);
        assertTrue(colonist.getLocation() instanceof Building);
        assertEquals(townHall, colony.getBuildingFor(colonist).getType());
        assertEquals(townHall, ((Building) colonist.getLocation()).getType());
        assertEquals(bells, colonist.getWorkType());

        colonist.putOutsideColony();
        colonist.setWorkType(cotton);
        colonist.modifyExperience(100);
        nonServerJoinColony(colonist, colony);
        assertEquals(townHall, colony.getBuildingFor(colonist).getType());
        assertEquals(townHall, ((Building) colonist.getLocation()).getType());
        assertEquals(bells, colonist.getWorkType());

        colonist.putOutsideColony();
        colonist.setType(spec().getUnitType("model.unit.masterCottonPlanter"));
        nonServerJoinColony(colonist, colony);
        assertEquals(townHall, colony.getBuildingFor(colonist).getType());
        assertEquals(townHall, ((Building) colonist.getLocation()).getType());
        assertEquals(bells, colonist.getWorkType());

        // colonist produces cloth, because there is cotton now
        colonist.putOutsideColony();
        colonist.setType(weaver);
        colony.addGoods(cotton, 100);
        nonServerJoinColony(colonist, colony);
        assertTrue(colonist.getLocation() instanceof Building);
        assertEquals(weaversHouse, colony.getBuildingFor(colonist).getType());
        assertEquals(weaversHouse, ((Building) colonist.getLocation()).getType());
        assertEquals(cloth, colonist.getWorkType());


    }

}
