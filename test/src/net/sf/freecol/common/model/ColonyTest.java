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

import net.sf.freecol.FreeCol;
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
    
    /**
     * Tests completion of buildable
     */
    public void testBuildingCompletion() {
        Game game = getGame();
        game.setMap(getTestMap(true));
        
        Colony colony = getStandardColony();
        Building initialWarehouse = new Building(getGame(), colony, depotType);
        colony.addBuilding(initialWarehouse);
        assertTrue("Colony should be able to build warehouse",colony.canBuild(warehouseType));
        colony.setCurrentlyBuilding(warehouseType);
        colony.addGoods(hammerGoodsType, 90);
                
        // Simulate that the build is done
        assertFalse("Colony should not have warehouse",colony.getWarehouse().getType() == warehouseType);
        colony.checkBuildableComplete();
        assertTrue("Colony should have warehouse",colony.getWarehouse().getType() == warehouseType);
        
    }
    
    /**
     * Tests invalid completion of buildable, having enough resources
    */
    public void testInvalidCompletion() {
    	Game game = getGame();
    	game.setMap(getTestMap(true));

    	Colony colony = getStandardColony(2);
    	Building carpenterHouse = new Building(getGame(), colony, carpenterHouseType);
    	colony.addBuilding(carpenterHouse);
    	assertFalse("Colony should not be able to build lumber mill",colony.canBuild(lumberMillType));
    	colony.setCurrentlyBuilding(lumberMillType);
    	assertTrue("Colony should be building lumber mill",colony.getCurrentlyBuilding() == lumberMillType);
    	// add sufficient goods to build lumber mill
    	for(AbstractGoods reqGoods : lumberMillType.getGoodsRequired()){
    		GoodsType type = reqGoods.getType();
    		int ammount = reqGoods.getAmount() + 1;
    		colony.addGoods(type, ammount);
    		assertEquals("Wrong quantity of " + type,ammount, colony.getGoodsCount(type));
    	}

    	// test
    	assertFalse("Colony should not have lumber mill",colony.getBuilding(lumberMillType).getType() == lumberMillType);
    	colony.checkBuildableComplete();
    	assertFalse("Colony should not have lumber mill",colony.getBuilding(lumberMillType).getType() == lumberMillType);
    	assertFalse("Colony should no longer be building lumber mill",colony.getCurrentlyBuilding() == lumberMillType);
    }
    
    public void testNoBuildingMaterialsProductionWhenBuildingNothing(){
        Game game = getGame();
        game.setMap(getTestMap(true));
        
        Colony colony = getStandardColony();
        Building carpenterHouse = new Building(getGame(), colony, carpenterHouseType);
        colony.addBuilding(carpenterHouse);
        Unit unit = colony.getUnitList().get(0);
        colony.getBuilding(carpenterHouseType).add(unit);
        // necessary for work production
        int initialLumber = 100;
        int initialHammers = 0;
        colony.addGoods(lumberGoodsType, initialLumber);
        colony.setCurrentlyBuilding(null);
        
        assertEquals("Wrong initial lumber quantity, ",
                     initialLumber, colony.getGoodsCount(lumberGoodsType));
        assertTrue("Colony shoud be able to produce work (hammers)",
                   colony.getProductionOf(hammerGoodsType) > 0);
        assertEquals("Colony shold not have any work production(hammers) initially, ",
                     initialHammers, colony.getGoodsCount(hammerGoodsType));
        colony.newTurn();
        assertEquals("Colony shold not have any work production(hammers) after, ",
                     initialHammers, colony.getGoodsCount(hammerGoodsType));
        assertEquals("Wrong final lumber quantity, ",
                     initialLumber, colony.getGoodsCount(lumberGoodsType));
    }



    public void testLibertyAndImmigration() {

        int population = 3;

        Game game = getGame();
        game.setMap(getTestMap(true));
        Colony colony = getStandardColony(population);
        
        Building townHall = new Building(getGame(), colony, townHallType);
        colony.addBuilding(townHall);
        Unit statesman = colony.getUnitList().get(0);
        statesman.setLocation(null);
        townHall.add(statesman);

        Building church = new Building(getGame(), colony, churchType);
        colony.addBuilding(church);
        church.upgrade();
        Unit preacher = colony.getUnitList().get(1);
        preacher.setLocation(null);
        church.add(preacher);

        GoodsType bellsType = spec().getGoodsType("model.goods.bells");
        GoodsType crossType = spec().getGoodsType("model.goods.crosses");

        colony.newTurn();
        assertEquals(population, colony.getUnitCount());
        assertEquals(4, colony.getProductionOf(bellsType));
        assertEquals(population - 2, colony.getConsumptionOf(bellsType));

        int bells = colony.getProductionOf(bellsType) - colony.getConsumptionOf(bellsType);
        assertEquals(bells, colony.getProductionNetOf(bellsType));
        assertEquals(bells, colony.getGoodsCount(bellsType));
        assertEquals(bells, colony.getLiberty());

        colony.addGoods(bellsType, 7);
        bells += 7;
        assertEquals(bells, colony.getGoodsCount(bellsType));
        assertEquals(bells, colony.getLiberty());

        colony.removeGoods(bellsType, 5);
        bells -= 5;
        assertEquals(bells, colony.getGoodsCount(bellsType));
        assertEquals(bells, colony.getLiberty());

        int crosses = colony.getProductionOf(crossType) - colony.getConsumptionOf(crossType);
        assertEquals(crosses, colony.getProductionNetOf(crossType));
        assertEquals(crosses, colony.getGoodsCount(crossType));
        assertEquals(crosses, colony.getImmigration());

        colony.addGoods(crossType, 7);
        crosses += 7;
        assertEquals(crosses, colony.getGoodsCount(crossType));
        assertEquals(crosses, colony.getImmigration());

        colony.removeGoods(crossType, 5);
        crosses -= 5;
        assertEquals(crosses, colony.getGoodsCount(crossType));
        assertEquals(crosses, colony.getImmigration());

    }

    public void testOccupationWithFood() {

        int population = 3;
        GoodsType food = spec().getGoodsType("model.goods.food");
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
        Unit colonist = new Unit(game, colony.getOwner(), freeColonist);
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

    public void testTeaParty() {
        Game game = getGame();
        game.setMap(getTestMap(true));
        Colony colony = getStandardColony(5);
        spec();
        
        colony.getFeatureContainer().addModifier(Modifier.createTeaPartyModifier(game.getTurn()));
        colony.getFeatureContainer().addModifier(Modifier.createTeaPartyModifier(game.getTurn()));
        colony.getFeatureContainer().addModifier(Modifier.createTeaPartyModifier(game.getTurn()));

        int modifierCount = 0;
        for (Modifier existingModifier : colony.getFeatureContainer().getModifierSet("model.goods.bells")) {
            if (Specification.COLONY_GOODS_PARTY.equals(existingModifier.getSource())) {
                modifierCount++;
            }
        }
        assertEquals(1, modifierCount);

        Turn newTurn = new Turn(game.getTurn().getNumber() + 1);
        colony.getFeatureContainer().addModifier(Modifier.createTeaPartyModifier(newTurn));

        modifierCount = 0;
        for (Modifier existingModifier : colony.getFeatureContainer().getModifierSet("model.goods.bells")) {
            if (Specification.COLONY_GOODS_PARTY.equals(existingModifier.getSource())) {
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
        GoodsType food = spec().getGoodsType("model.goods.food");
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
        Unit colonist = new Unit(game, colony.getOwner(), spec().getUnitType("model.unit.freeColonist"));
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
