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

import java.io.StringWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.model.Modifier;
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

        assertFalse(warehouse.upgrade());
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
        Unit unit = colony.getUnitList().get(0);
        EquipmentType missionary = spec().getEquipmentType("model.equipment.missionary");

        BuildingType churchType = spec().getBuildingType("model.building.chapel");
        assertFalse(churchType.hasAbility("model.ability.dressMissionary"));
        assertFalse(unit.hasAbility("model.ability.dressMissionary"));
        assertFalse(unit.canBeEquippedWith(missionary));

        Building church = colony.getBuilding(churchType);
        assertTrue(church != null);
        assertFalse(colony.hasAbility("model.ability.dressMissionary"));
        assertFalse(unit.hasAbility("model.ability.dressMissionary"));
        assertFalse(unit.canBeEquippedWith(missionary));

        church.upgrade();
        assertTrue(church.getType().hasAbility("model.ability.dressMissionary"));
        assertTrue(colony.hasAbility("model.ability.dressMissionary"));
        assertTrue(unit.hasAbility("model.ability.dressMissionary"));
        assertTrue(unit.canBeEquippedWith(missionary));
    }

    public void testCanAddToBuilding() {
    	Game game = getGame();
    	game.setMap(getTestMap(true));

        Colony colony = getStandardColony(6);
        List<Unit> units = colony.getUnitList();

        for (Building building : colony.getBuildings()) {

            // schoolhouse is special, see testCanAddToSchool
            if (building.canTeach())
            	continue;

            int maxUnits = building.getUnitCapacity();

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
                building.remove(building.getUnitList().get(0));
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
        Game game = getGame();
        game.setMap(getTestMap(true));
        Set<Modifier> modifierSet;

        Colony colony = getStandardColony(2);
        modifierSet = colony.getModifierSet("model.modifier.defence");
        assertEquals(1, modifierSet.size());
        Modifier modifier = modifierSet.iterator().next();
        assertEquals(50f, modifier.getValue());
        assertEquals(Modifier.Type.PERCENTAGE, modifier.getType());

        BuildingType stockade = spec()
            .getBuildingType("model.building.stockade");
        modifierSet = stockade.getModifierSet("model.modifier.defence");
        assertEquals(1, modifierSet.size());
        modifier = modifierSet.iterator().next();
        assertEquals(100f, modifier.getValue());
        assertEquals(Modifier.Type.PERCENTAGE, modifier.getType());
        assertEquals(0f, stockade.getFeatureContainer()
                     .applyModifier(0, "model.modifier.minimumColonySize"));

        BuildingType fort = spec().getBuildingType("model.building.fort");
        modifierSet = fort.getModifierSet("model.modifier.defence");
        assertEquals(1, modifierSet.size());
        modifier = modifierSet.iterator().next();
        assertEquals(150f, modifier.getValue());
        assertEquals(Modifier.Type.PERCENTAGE, modifier.getType());
        assertEquals(0f, stockade.getFeatureContainer()
                     .applyModifier(0, "model.modifier.minimumColonySize"));

        BuildingType fortress = spec()
            .getBuildingType("model.building.fortress");
        modifierSet = fortress.getModifierSet("model.modifier.defence");
        assertEquals(1, modifierSet.size());
        modifier = modifierSet.iterator().next();
        assertEquals(200f, modifier.getValue());
        assertEquals(Modifier.Type.PERCENTAGE, modifier.getType());
        assertEquals(0f, stockade.getFeatureContainer()
                     .applyModifier(0, "model.modifier.minimumColonySize"));
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

        colony.addGoods(cottonType, 2);
        assertEquals(2, colony.getProductionOf(cottonType));
        assertEquals(3, weaver.getProductionOf(clothType));
        assertEquals(3, colony.getProductionOf(clothType));
        assertEquals(-1, colony.getNetProductionOf(cottonType));
        assertEquals(3, colony.getNetProductionOf(clothType));

        colonist.setWorkType(cottonType);

        assertEquals(4, colony.getProductionOf(cottonType));
        colony.addGoods(cottonType, 4);
        assertEquals(3, colony.getProductionOf(clothType));
        assertEquals(1, colony.getNetProductionOf(cottonType));
        assertEquals(3, colony.getNetProductionOf(clothType));

    }

    public void testAutoProduction() {
    	Game game = getGame();
    	game.setMap(getTestMap(true));

        Colony colony = getStandardColony(1);
        GoodsType foodType = spec().getPrimaryFoodType();
        GoodsType grainType = spec().getGoodsType("model.goods.grain");
        GoodsType horsesType = spec().getGoodsType("model.goods.horses");

        BuildingType country = spec().getBuildingType("model.building.country");
        Building pasture = colony.getBuilding(country);
        assertEquals(grainType, pasture.getGoodsInputType());
        assertEquals(horsesType, pasture.getGoodsOutputType());

        // no horses yet
        assertEquals(8, colony.getNetProductionOf(foodType));
        assertEquals(0, pasture.getProductionOf(horsesType));
        assertEquals(0, colony.getNetProductionOf(horsesType));
        assertEquals(0, pasture.getMaximumProduction());

        colony.addGoods(horsesType, 50);
        assertEquals(2, pasture.getProductionOf(horsesType));
        assertEquals(2, pasture.getMaximumProduction());
        assertEquals(2, colony.getNetProductionOf(horsesType));

        colony.addGoods(horsesType, 1);
        assertEquals(4, pasture.getProductionOf(horsesType));
        assertEquals(4, pasture.getMaximumProduction());
        assertEquals(4, colony.getNetProductionOf(horsesType));

        pasture.upgrade();
        colony.removeGoods(horsesType);

        colony.addGoods(horsesType, 25);
        assertEquals(25, colony.getGoodsCount(horsesType));
        assertEquals(2, pasture.getProductionOf(horsesType));
        assertEquals(2, pasture.getMaximumProduction());
        assertEquals(2, colony.getNetProductionOf(horsesType));

        colony.addGoods(horsesType, 1);
        assertEquals(26, colony.getGoodsCount(horsesType));
        assertEquals(4, pasture.getProductionOf(horsesType));
        assertEquals(4, pasture.getMaximumProduction());
        assertEquals(4, colony.getNetProductionOf(horsesType));

        colony.addGoods(horsesType, 24);
        assertEquals(50, colony.getGoodsCount(horsesType));
        assertEquals(4, pasture.getProductionOf(horsesType));
        assertEquals(4, pasture.getMaximumProduction());
        assertEquals(4, colony.getNetProductionOf(horsesType));

        colony.addGoods(horsesType, 1);
        assertEquals(51, colony.getGoodsCount(horsesType));
        // no more than half the surplus production!
        assertEquals(4, pasture.getProductionOf(horsesType));
        assertEquals(6, pasture.getMaximumProduction());
        assertEquals("Horse production should equal food surplus.",
                     colony.getNetProductionOf(foodType),
                     colony.getNetProductionOf(horsesType));
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
        Unit unit = colony.getUnitList().get(0);
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
        Unit unit = colony.getUnitList().get(0);
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


    public void testUnitProductivity() {
        Game game = getGame();
        game.setMap(getTestMap(true));

        Colony colony = getStandardColony(4);
        Unit unit = colony.getUnitList().get(0);

        UnitType servant = spec().getUnitType("model.unit.indenturedServant");
        UnitType convert = spec().getUnitType("model.unit.indianConvert");
        UnitType criminal = spec().getUnitType("model.unit.pettyCriminal");

        for (Building building : colony.getBuildings()) {
            GoodsType outputType = building.getGoodsOutputType();
            if (outputType != null) {
                for (UnitType type : spec().getUnitTypeList()) {
                    if (building.getType().canAdd(type)
                        && type.isAvailableTo(colony.getOwner())) {
                        unit.setType(type);
                        int productivity = building.getUnitProductivity(unit);
                        int expected = building.getType().getBasicProduction();
                        if (type == building.getExpertUnitType()) {
                            expected = 6;
                        } else if (type == servant) {
                            expected = 2;
                        } else if (type == convert) {
                            expected = 1;
                        } else if (type == criminal) {
                            expected = 1;
                        }
                        if (expected != building.getType().getBasicProduction()) {
                            Set<Modifier> modifierSet = type.getModifierSet(outputType.getId());
                            assertFalse("ModifierSet should not be empty!",
                                        modifierSet.isEmpty());
                        }
                        assertEquals("Wrong productivity for " + type,
                                     expected, productivity);
                    }
                }
            }
        }


    }

}
