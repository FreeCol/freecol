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

import java.io.StringWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.server.model.ServerBuilding;
import net.sf.freecol.util.test.FreeColTestCase;


public class BuildingTest extends FreeColTestCase {

    BuildingType printingPressType = spec().getBuildingType("model.building.printingPress");
    BuildingType newspaperType = spec().getBuildingType("model.building.newspaper");

    public void testCanBuildNext() {
    	Game game = getGame();
    	game.setMap(getTestMap(true));

    	Colony colony = getStandardColony();

        // First check with a building that can be fully built with a
        // normal colony
        BuildingType warehouseType = spec().getBuildingType("model.building.depot");
        Building warehouse = new ServerBuilding(getGame(), colony, warehouseType);
        colony.addBuilding(warehouse);
        assertTrue(warehouse.canBuildNext());
        warehouse.upgrade();
        assertTrue(warehouse.canBuildNext());
        warehouse.upgrade();
        assertFalse(warehouse.canBuildNext());

        try {
        	warehouse.upgrade();
        	fail();
        } catch (IllegalStateException e){
        	// Should throw exception
        }
        assertFalse(warehouse.canBuildNext());

        // Check whether population restrictions work

        // Colony smallColony = getStandardColony(1);
        //
        // Colony largeColony = getStandardColony(6);
        // ...

        // Check whether founding fathers work

    }

    public void testInitialColony() {
    	Game game = getGame();
    	game.setMap(getTestMap(true));

        Colony colony = getStandardColony();

        BuildingType warehouseType = spec().getBuildingType("model.building.warehouse");
        Building warehouse = colony.getBuilding(warehouseType);

        // Is build as depot...
        assertTrue(warehouse != null);

        assertTrue(warehouse.canBuildNext());

        // Check other building...

        // Check dock -> only possible if not landlocked...

    }

    public void testChurch() {
    	Game game = getGame();
    	game.setMap(getTestMap(true));

        Colony colony = getStandardColony(6);
        BuildingType churchType = spec().getBuildingType("model.building.chapel");
        assertFalse(churchType.hasAbility("model.ability.dressMissionary"));

        Building church = colony.getBuilding(churchType);
        assertTrue(church != null);
        assertFalse(colony.hasAbility("model.ability.dressMissionary"));

        church.upgrade();
        assertTrue(church.getType().hasAbility("model.ability.dressMissionary"));
        assertTrue(colony.hasAbility("model.ability.dressMissionary"));

    }

    public void testCanAddToBuilding() {
    	Game game = getGame();
    	game.setMap(getTestMap(true));

        Colony colony = getStandardColony(6);
        List<Unit> units = colony.getUnitList();

        for (Building building : colony.getBuildings()) {

            // schoolhouse is special, see testCanAddToSchool
            if (building.getType().hasAbility("model.ability.teach"))
            	continue;

            int maxUnits = building.getMaxUnits();

            assertEquals(0, building.getUnitCount());

            for (int index = 0; index < maxUnits; index++) {
                assertTrue("unable to add unit " + index + " to building type " +
                           building.getType(), building.canAdd(units.get(index)));
                building.add(units.get(index));
            }
            assertFalse("able to add unit " + maxUnits + " to building type " +
                        building.getType(),
                        building.canAdd(units.get(maxUnits)));
            for (int index = 0; index < maxUnits; index++) {
                building.remove(building.getFirstUnit());
            }
        }
    }


    /**
     * WARNING! This test makes implicit assumptions about the
     * schoolhouse that could be invalidated by the
     * specification.
     *
     * TODO: make this more generic.
     */
    public void testCanAddToSchool(){
        UnitType freeColonistType = spec().getUnitType("model.unit.freeColonist");
        UnitType indenturedServantType = spec().getUnitType("model.unit.indenturedServant");
        UnitType pettyCriminalType = spec().getUnitType("model.unit.pettyCriminal");
        UnitType expertFarmerType = spec().getUnitType("model.unit.expertFarmer");
        UnitType masterCarpenterType = spec().getUnitType("model.unit.masterCarpenter");
        UnitType masterDistillerType = spec().getUnitType("model.unit.masterDistiller");
        UnitType elderStatesmanType = spec().getUnitType("model.unit.elderStatesman");
        UnitType indianConvertType = spec().getUnitType("model.unit.indianConvert");

    	Game game = getGame();
    	game.setMap(getTestMap(true));

        Colony colony = getStandardColony(10);

        Iterator<Unit> units = colony.getUnitIterator();

        Unit farmer = units.next();
        farmer.setType(expertFarmerType);

        Unit colonist = units.next();
        colonist.setType(freeColonistType);

        Unit criminal = units.next();
        criminal.setType(pettyCriminalType);

        Unit servant = units.next();
        servant.setType(indenturedServantType);

        Unit indian = units.next();
        indian.setType(indianConvertType);

        Unit distiller = units.next();
        distiller.setType(masterDistillerType);

        Unit elder = units.next();
        elder.setType(elderStatesmanType);

        Unit carpenter = units.next();
        carpenter.setType(masterCarpenterType);

        // Check school
        BuildingType schoolType = spec().getBuildingType("model.building.schoolhouse");
        Building school = colony.getBuilding(schoolType);
        assertTrue(school == null);

        // build school
        colony.addBuilding(new ServerBuilding(getGame(), colony, schoolType));
        school = colony.getBuilding(schoolType);
        assertTrue(school != null);

        // these can never teach
        assertFalse("able to add free colonist to Schoolhouse",
                    school.canAdd(colonist));
        assertFalse("able to add petty criminal to Schoolhouse",
                    school.canAdd(criminal));
        assertFalse("able to add indentured servant to Schoolhouse",
                    school.canAdd(servant));
        assertFalse("able to add indian convert to Schoolhouse",
                    school.canAdd(indian));

        assertFalse("able to add elder statesman to Schoolhouse",
                    school.canAdd(elder));
        assertFalse("able to add master distiller to Schoolhouse",
                    school.canAdd(distiller));
        assertTrue("unable to add master farmer to Schoolhouse",
                   school.canAdd(farmer));
        school.add(farmer);
        assertFalse("able to add master carpenter to Schoolhouse",
                    school.canAdd(carpenter));
        school.remove(farmer);

        school.upgrade();
        // these can never teach
        assertFalse("able to add free colonist to College",
                    school.canAdd(colonist));
        assertFalse("able to add petty criminal to College",
                    school.canAdd(criminal));
        assertFalse("able to add indentured servant to College",
                    school.canAdd(servant));
        assertFalse("able to add indian convert to College",
                    school.canAdd(indian));

        assertFalse("able to add elder statesman to College",
                    school.canAdd(elder));
        assertTrue("unable to add master distiller to College",
                   school.canAdd(distiller));
        school.add(distiller);
        assertTrue("unable to add master farmer to College",
                   school.canAdd(farmer));
        school.add(farmer);
        assertFalse("able to add master carpenter to College",
                    school.canAdd(carpenter));
        school.remove(distiller);
        school.remove(farmer);

        school.upgrade();

        assertEquals(school.getType().toString(), school.getType(), spec().getBuildingType("model.building.university"));

        // these can never teach
        assertFalse("able to add free colonist to University",
                    school.canAdd(colonist));
        assertFalse("able to add petty criminal to University",
                    school.canAdd(criminal));
        assertFalse("able to add indentured servant to University",
                    school.canAdd(servant));
        assertFalse("able to add indian convert to University",
                    school.canAdd(indian));

        assertTrue("unable to add elder statesman to University",
                   school.canAdd(elder));
        school.add(elder);
        assertTrue("unable to add master distiller to University",
                   school.canAdd(distiller));
        school.add(distiller);
        assertTrue("unable to add master farmer to University",
                   school.canAdd(farmer));
        school.add(farmer);
        assertFalse("able to add master carpenter to University",
                    school.canAdd(carpenter));
        school.remove(elder);
        school.remove(distiller);
        school.remove(farmer);

    }

    public void testSerialize() {
    	Game game = getGame();
    	game.setMap(getTestMap(true));

        Colony colony = getStandardColony(6);
        for (Building building : colony.getBuildings()) {
            try {
                StringWriter sw = new StringWriter();
                XMLOutputFactory xif = XMLOutputFactory.newInstance();
                XMLStreamWriter xsw = xif.createXMLStreamWriter(sw);
                building.toXML(xsw, building.getColony().getOwner(), true, true);
                xsw.close();
            } catch (XMLStreamException e) {
                fail();
            }
        }
    }

    public void testStockade() {

        Set<Modifier> modifierSet;

        BuildingType stockade = spec().getBuildingType("model.building.stockade");
        modifierSet = stockade.getModifierSet("model.modifier.defence");
        assertEquals(1, modifierSet.size());
        assertEquals(100f, modifierSet.iterator().next().getValue());
        assertEquals(0f, stockade.getFeatureContainer().applyModifier(0, "model.modifier.minimumColonySize"));

        BuildingType fort = spec().getBuildingType("model.building.fort");
        modifierSet = fort.getModifierSet("model.modifier.defence");
        assertEquals(1, modifierSet.size());
        assertEquals(150f, modifierSet.iterator().next().getValue());
        assertEquals(0f, stockade.getFeatureContainer().applyModifier(0, "model.modifier.minimumColonySize"));

        BuildingType fortress = spec().getBuildingType("model.building.fortress");
        modifierSet = fortress.getModifierSet("model.modifier.defence");
        assertEquals(1, modifierSet.size());
        assertEquals(200f, modifierSet.iterator().next().getValue());
        assertEquals(0f, stockade.getFeatureContainer().applyModifier(0, "model.modifier.minimumColonySize"));

    }

    public void testCottonClothProduction() {
    	Game game = getGame();
    	game.setMap(getTestMap(true));

        Colony colony = getStandardColony(2);
        List<Unit> units = colony.getUnitList();
        Unit colonist = units.get(0);
        Unit worker = units.get(1);

        TileType plainsType = spec().getTileType("model.tile.plains");
        GoodsType cottonType = spec().getGoodsType("model.goods.cotton");
        GoodsType clothType = spec().getGoodsType("model.goods.cloth");

        Building weaver = colony.getBuilding(spec().getBuildingType("model.building.weaverHouse"));
        assertEquals(cottonType, weaver.getGoodsInputType());
        assertEquals(clothType, weaver.getGoodsOutputType());

        assertEquals(plainsType, ((WorkLocation) colonist.getLocation()).getTile().getType());
        assertEquals(plainsType, ((WorkLocation) worker.getLocation()).getTile().getType());

        weaver.add(worker);
        assertEquals(worker, weaver.getUnitList().get(0));

        assertEquals(2, colony.getProductionOf(cottonType));
        colony.addGoods(cottonType, 2);
        assertEquals(2, weaver.getProductionOf(clothType));
        assertEquals(2, colony.getProductionOf(clothType));
        assertEquals(-1, colony.getProductionNetOf(cottonType));
        assertEquals(3, colony.getProductionNetOf(clothType));

        colonist.setWorkType(cottonType);

        assertEquals(4, colony.getProductionOf(cottonType));
        colony.addGoods(cottonType, 4);
        assertEquals(3, colony.getProductionOf(clothType));
        assertEquals(1, colony.getProductionNetOf(cottonType));
        assertEquals(3, colony.getProductionNetOf(clothType));

    }

    public void testAutoProduction() {
    	Game game = getGame();
    	game.setMap(getTestMap(true));

        Colony colony = getStandardColony(1);
        GoodsType foodType = spec().getGoodsType("model.goods.grain");
        GoodsType horsesType = spec().getGoodsType("model.goods.horses");


        Building pasture = colony.getBuilding(spec().getBuildingType("model.building.country"));
        assertEquals(foodType, pasture.getGoodsInputType());
        assertEquals(horsesType, pasture.getGoodsOutputType());

        // no horses yet
        assertEquals(10, colony.getProductionOf(foodType));
        assertEquals(0, pasture.getProductionOf(horsesType));
        assertEquals(2, colony.getFoodConsumption());
        assertEquals(0, pasture.getGoodsInputNextTurn());
        //assertEquals(8, colony.getProductionNetOf(foodType));
        assertEquals(0, colony.getProductionNetOf(horsesType));
        assertEquals(0, pasture.getMaximumProduction());

        colony.addGoods(horsesType, 20);
        assertEquals(1, pasture.getProductionOf(horsesType));
        assertEquals(2, pasture.getMaximumProduction());
        assertEquals(1, colony.getProductionNetOf(horsesType));
        colony.addGoods(horsesType, 20);
        assertEquals(2, pasture.getProductionOf(horsesType));
        assertEquals(4, pasture.getMaximumProduction());
        assertEquals(2, colony.getProductionNetOf(horsesType));

    }

    public void testTownhallProduction() {
    	Game game = getGame();
    	game.setMap(getTestMap(true));

        Colony colony = getStandardColony(6);
        Player owner = colony.getOwner();
        Unit colonist = colony.getUnitList().get(0);
        Unit statesman = colony.getUnitList().get(1);
        statesman.setType(spec().getUnitType("model.unit.elderStatesman"));

        BuildingType townHall = spec().getBuildingType("model.building.townHall");
        Building building = colony.getBuilding(townHall);
        GoodsType bellsType = spec().getGoodsType("model.goods.bells");

        Set<Modifier> modifiers = colony.getModifierSet("model.goods.bells");
        assertEquals(1, modifiers.size());
        Modifier bellsModifier = modifiers.iterator().next();
        assertEquals(Modifier.Type.ADDITIVE, bellsModifier.getType());
        assertEquals(1.0f, bellsModifier.getValue());

        assertEquals("Wrong initial bell production",
                     (int) bellsModifier.getValue(), building.getProduction());

        building.add(colonist);
        // 3 from the colonist
        assertEquals(3, colonist.getProductionOf(bellsType, townHall.getBasicProduction()));
        assertEquals(3, building.getUnitProductivity(colonist));
        // 3 from the colonist + 1
        assertEquals("Wrong bell production", 4, building.getProduction());

        FoundingFather jefferson = spec().getFoundingFather("model.foundingFather.thomasJefferson");
        modifiers = jefferson.getModifierSet("model.goods.bells");
        assertEquals(1, modifiers.size());
        bellsModifier = modifiers.iterator().next();
        owner.addFather(jefferson);
        assertTrue(colony.getOwner().getFeatureContainer().getModifierSet("model.goods.bells")
                   .contains(bellsModifier));
        assertTrue(colony.getModifierSet("model.goods.bells").contains(bellsModifier));

        assertEquals(3, building.getUnitProductivity(colonist));
        // 3 from the colonist + 50% + 1 = 5.5
        assertEquals("Wrong bell production with Jefferson", 5, building.getProduction());

        building.add(statesman);
        assertEquals(3, building.getUnitProductivity(colonist));
        assertEquals(6, building.getUnitProductivity(statesman));
        // 3 + 6 + 50% + 1 = 14
        assertEquals("Wrong bell production with Jefferson", 14, building.getProduction());

        setProductionBonus(colony, 2);
        assertEquals(5, building.getUnitProductivity(colonist));
        assertEquals(10, building.getUnitProductivity(statesman));
        // 5 + 10 + 50% + 1 = 23
        assertEquals("Wrong bell production with Jefferson and +2 production bonus",
                     23, building.getProduction());

        Building newspaper = new ServerBuilding(getGame(), colony, newspaperType);
        colony.addBuilding(newspaper);
        assertEquals(5, building.getUnitProductivity(colonist));
        assertEquals(10, building.getUnitProductivity(statesman));
        // 5 + 10 + 50% + 1 + 100% = 47
        assertEquals("Wrong bell production with Jefferson, newspaper and +2 production bonus",
                     47, building.getProduction());


    }

    public void testPrintingPressBonus() {
        Game game = getGame();
        game.setMap(getTestMap(true));

        Colony colony = getStandardColony(6);
        Unit unit = colony.getRandomUnit();
        Building building = colony.getBuilding(spec().getBuildingType("model.building.townHall"));

        int bellProduction = building.getProduction();
        int expectBellProd = 1;
        assertEquals("Wrong initial bell production",expectBellProd,bellProduction);

        Building printingPress = new ServerBuilding(getGame(), colony, printingPressType);
        colony.addBuilding(printingPress);

        bellProduction = building.getProduction();
        expectBellProd = 1;
        assertEquals("Wrong bell production with printing press",expectBellProd,bellProduction);

        building.add(unit);
        bellProduction = building.getProduction();
        expectBellProd = 6; // 1 initial plus 3 from the colonist + 2 from printing press
        assertEquals("Wrong final bell production",expectBellProd,bellProduction);
    }

    public void testNewspaperBonus() {
        Game game = getGame();
        game.setMap(getTestMap(true));

        Colony colony = getStandardColony(6);
        Unit unit = colony.getRandomUnit();
        Building building = colony.getBuilding(spec().getBuildingType("model.building.townHall"));

        int bellProduction = building.getProduction();
        int expectBellProd = 1;
        assertEquals("Wrong initial bell production",expectBellProd,bellProduction);

        Building newspaper = new ServerBuilding(getGame(), colony, newspaperType);
        colony.addBuilding(newspaper);

        bellProduction = building.getProduction();
        expectBellProd = 2;
        assertEquals("Wrong bell production with newspaper",expectBellProd,bellProduction);

        building.add(unit);
        bellProduction = building.getProduction();
        expectBellProd = 8; // 1 initial plus 3 from the colonist + 4 from newspaper
        assertEquals("Wrong final bell production",expectBellProd,bellProduction);
    }

}
