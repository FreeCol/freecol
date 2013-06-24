/**
 *  Copyright (C) 2002-2013  The FreeCol Team
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

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.FreeColObject.WriteScope;
import net.sf.freecol.common.model.Modifier;
import net.sf.freecol.server.model.ServerBuilding;
import net.sf.freecol.server.model.ServerUnit;
import net.sf.freecol.util.test.FreeColTestCase;


public class BuildingTest extends FreeColTestCase {

    private static final BuildingType armoryType
        = spec().getBuildingType("model.building.armory");
    private static final BuildingType blacksmithType
        = spec().getBuildingType("model.building.blacksmithHouse");
    private static final BuildingType carpenterHouseType
        = spec().getBuildingType("model.building.carpenterHouse");
    private static final BuildingType chapelType
        = spec().getBuildingType("model.building.chapel");
    private static final BuildingType countryType
        = spec().getBuildingType("model.building.country");
    private static final BuildingType depotType
        = spec().getBuildingType("model.building.depot");
    private static final BuildingType fortType
        = spec().getBuildingType("model.building.fort");
    private static final BuildingType fortressType
        = spec().getBuildingType("model.building.fortress");
    private static final BuildingType newspaperType
        = spec().getBuildingType("model.building.newspaper");
    private static final BuildingType printingPressType
        = spec().getBuildingType("model.building.printingPress");
    private static final BuildingType schoolType
        = spec().getBuildingType("model.building.schoolhouse");
    private static final BuildingType stockadeType
        = spec().getBuildingType("model.building.stockade");
    private static final BuildingType townHallType
        = spec().getBuildingType("model.building.townHall");
    private static final BuildingType universityType
        = spec().getBuildingType("model.building.university");
    private static final BuildingType warehouseType
        = spec().getBuildingType("model.building.warehouse");
    private static final BuildingType weaverHouseType
        = spec().getBuildingType("model.building.weaverHouse");

    private static final GoodsType bellsType
        = spec().getGoodsType("model.goods.bells");
    private static final GoodsType clothType
        = spec().getGoodsType("model.goods.cloth");
    private static final GoodsType cottonType
        = spec().getGoodsType("model.goods.cotton");
    private static final GoodsType crossesType
        = spec().getGoodsType("model.goods.crosses");
    private static final GoodsType foodType
        = spec().getPrimaryFoodType();
    private static final GoodsType grainType
        = spec().getGoodsType("model.goods.grain");
    private static final GoodsType hammersType
        = spec().getGoodsType("model.goods.hammers");
    private static final GoodsType horsesType
        = spec().getGoodsType("model.goods.horses");
    private static final GoodsType lumberType
        = spec().getGoodsType("model.goods.lumber");
    private static final GoodsType musketsType
        = spec().getGoodsType("model.goods.muskets");
    private static final GoodsType toolsType
        = spec().getGoodsType("model.goods.tools");

    private static final TileType plainsType
        = spec().getTileType("model.tile.plains");

    private static final UnitType elderStatesmanType
        = spec().getUnitType("model.unit.elderStatesman");
    private static final UnitType expertFarmerType
        = spec().getUnitType("model.unit.expertFarmer");
    private static final UnitType freeColonistType
        = spec().getUnitType("model.unit.freeColonist");
    private static final UnitType indenturedServantType
        = spec().getUnitType("model.unit.indenturedServant");
    private static final UnitType indianConvertType
        = spec().getUnitType("model.unit.indianConvert");
    private static final UnitType masterCarpenterType
        = spec().getUnitType("model.unit.masterCarpenter");
    private static final UnitType masterDistillerType
        = spec().getUnitType("model.unit.masterDistiller");
    private static final UnitType pettyCriminalType
        = spec().getUnitType("model.unit.pettyCriminal");


    public void testCanBuildNext() {
        Game game = getGame();
        game.setMap(getTestMap(true));

        Colony colony = getStandardColony();

        // First check with a building that can be fully built with a
        // normal colony
        Building warehouse = new ServerBuilding(getGame(), colony, depotType);
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

    public void testStockadeRequiresMinimumPopulation() {
        Game game = getGame();
        game.setMap(getTestMap(true));
        Colony colony = getStandardColony(2);
        assertEquals(Colony.NoBuildReason.POPULATION_TOO_SMALL, colony.getNoBuildReason(stockadeType));

        Unit colonist = new ServerUnit(game, colony.getTile(), colony.getOwner(), freeColonistType);
        colonist.setLocation(colony);

        assertEquals(Colony.NoBuildReason.NONE, colony.getNoBuildReason(stockadeType));
    }

    public void testFortRequiresMinimumPopulation() {
        Game game = getGame();
        game.setMap(getTestMap(true));
        Colony colony = getStandardColony(2);
        assertEquals(Colony.NoBuildReason.POPULATION_TOO_SMALL, colony.getNoBuildReason(fortType));

        Unit colonist = new ServerUnit(game, colony.getTile(), colony.getOwner(), freeColonistType);
        colonist.setLocation(colony);

        colony.addBuilding(new ServerBuilding(game, colony, stockadeType));
        assertEquals(Colony.NoBuildReason.NONE, colony.getNoBuildReason(fortType));
    }

    public void testFortressRequiresMinimumPopulation() {
        Game game = getGame();
        game.setMap(getTestMap(true));

        Colony colony = getStandardColony(7);
        colony.addBuilding(new ServerBuilding(game, colony, stockadeType));
        colony.addBuilding(new ServerBuilding(game, colony, fortType));
        assertEquals(Colony.NoBuildReason.POPULATION_TOO_SMALL, colony.getNoBuildReason(fortressType));

        Unit colonist = new ServerUnit(game, colony.getTile(), colony.getOwner(), freeColonistType);
        colonist.setLocation(colony);

        assertEquals(8, colony.getUnitCount());
        assertEquals(Colony.NoBuildReason.NONE, colony.getNoBuildReason(fortressType));
    }

    public void testInitialColony() {
        Game game = getGame();
        game.setMap(getTestMap(true));

        Colony colony = getStandardColony();

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

        assertFalse(chapelType.hasAbility("model.ability.dressMissionary"));
        assertFalse(unit.hasAbility("model.ability.dressMissionary"));
        assertFalse(unit.canBeEquippedWith(missionary));

        Building church = colony.getBuilding(chapelType);
        assertTrue(church != null);
        assertFalse(colony.hasAbility("model.ability.dressMissionary"));
        assertFalse(unit.hasAbility("model.ability.dressMissionary"));
        assertFalse(unit.canBeEquippedWith(missionary));
        assertEquals(1, church.getPotentialProduction(crossesType, null));

        church.upgrade();
        assertTrue(church.getType().hasAbility("model.ability.dressMissionary"));
        assertTrue(colony.hasAbility("model.ability.dressMissionary"));
        assertTrue(unit.hasAbility("model.ability.dressMissionary"));
        assertTrue(unit.canBeEquippedWith(missionary));
        assertEquals(2, church.getPotentialProduction(crossesType, null));
    }

    public void testCanAddToBuilding() {
        Game game = getGame();
        game.setMap(getTestMap(true));

        Colony colony = getStandardColony(6);
        List<Unit> units = colony.getUnitList();

        for (Building building : colony.getBuildings()) {

            // schoolhouse is special, see testCanAddToSchool
            if (building.canTeach()) continue;

            int maxUnits = building.getUnitCapacity();

            assertEquals(0, building.getUnitCount());

            for (int index = 0; index < maxUnits; index++) {
                assertTrue("unable to add unit " + index
                    + " to building type " + building.getType(),
                    building.canAdd(units.get(index)));
                building.add(units.get(index));
            }
            assertFalse("able to add unit " + maxUnits
                + " to building type " + building.getType(),
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

        assertEquals(school.getType().toString(), school.getType(),
            universityType);

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
                FreeColXMLWriter xw = new FreeColXMLWriter(sw);

                building.toXML(xw, WriteScope.toSave());

                xw.close();

            } catch (Exception e) {
                fail(e.toString());
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

        modifierSet = stockadeType.getModifierSet("model.modifier.defence");
        assertEquals(1, modifierSet.size());
        modifier = modifierSet.iterator().next();
        assertEquals(100f, modifier.getValue());
        assertEquals(Modifier.Type.PERCENTAGE, modifier.getType());
        assertEquals(0f, stockadeType.applyModifier(0,
                "model.modifier.minimumColonySize"));

        modifierSet = fortType.getModifierSet("model.modifier.defence");
        assertEquals(1, modifierSet.size());
        modifier = modifierSet.iterator().next();
        assertEquals(150f, modifier.getValue());
        assertEquals(Modifier.Type.PERCENTAGE, modifier.getType());
        assertEquals(0f, stockadeType.applyModifier(0,
                "model.modifier.minimumColonySize"));

        modifierSet = fortressType.getModifierSet("model.modifier.defence");
        assertEquals(1, modifierSet.size());
        modifier = modifierSet.iterator().next();
        assertEquals(200f, modifier.getValue());
        assertEquals(Modifier.Type.PERCENTAGE, modifier.getType());
        assertEquals(0f, stockadeType.applyModifier(0,
                "model.modifier.minimumColonySize"));
    }

    public void testCottonClothProduction() {
        Game game = getGame();
        game.setMap(getTestMap(true));

        Colony colony = getStandardColony(2);
        List<Unit> units = colony.getUnitList();
        Unit colonist = units.get(0);
        Unit worker = units.get(1);

        Building weaver = colony.getBuilding(weaverHouseType);
        List<AbstractGoods> inputs = weaver.getInputs();
        assertEquals(1, inputs.size());
        assertEquals(cottonType, inputs.get(0).getType());
        List<AbstractGoods> outputs = weaver.getOutputs();
        assertEquals(1, outputs.size());
        assertEquals(clothType, outputs.get(0).getType());

        assertTrue(colonist.getLocation() instanceof ColonyTile);
        assertEquals(plainsType, ((ColonyTile)colonist.getLocation()).getWorkTile().getType());
        assertTrue(worker.getLocation() instanceof ColonyTile);
        assertEquals(plainsType, ((ColonyTile)worker.getLocation()).getWorkTile().getType());

        weaver.add(worker);
        assertEquals(worker, weaver.getUnitList().get(0));

        colony.addGoods(cottonType, 2);
        assertEquals(2, colony.getTotalProductionOf(cottonType));
        assertEquals(3, weaver.getTotalProductionOf(clothType));
        assertEquals(3, colony.getTotalProductionOf(clothType));
        assertEquals(-1, colony.getNetProductionOf(cottonType));
        assertEquals(3, colony.getNetProductionOf(clothType));

        colonist.changeWorkType(cottonType);
        colony.invalidateCache();

        assertEquals(4, colony.getTotalProductionOf(cottonType));
        colony.addGoods(cottonType, 4);
        assertEquals(3, colony.getTotalProductionOf(clothType));
        assertEquals(1, colony.getNetProductionOf(cottonType));
        assertEquals(3, colony.getNetProductionOf(clothType));
    }

    public void testAutoProduction() {
        Game game = getGame();
        game.setMap(getTestMap(true));

        Colony colony = getStandardColony(1);

        Building pasture = colony.getBuilding(countryType);
        List<AbstractGoods> inputs = pasture.getInputs();
        assertEquals(1, inputs.size());
        assertEquals(grainType, inputs.get(0).getType());
        List<AbstractGoods> outputs = pasture.getOutputs();
        assertEquals(1, outputs.size());
        assertEquals(horsesType, outputs.get(0).getType());

        // no horses yet
        assertEquals(8, colony.getNetProductionOf(foodType));
        assertEquals(0, pasture.getTotalProductionOf(horsesType));
        assertEquals(0, colony.getNetProductionOf(horsesType));
        assertEquals(0, pasture.getMaximumProductionOf(horsesType));

        colony.addGoods(horsesType, 50);
        assertEquals(2, pasture.getTotalProductionOf(horsesType));
        assertEquals(2, pasture.getMaximumProductionOf(horsesType));
        assertEquals(2, colony.getNetProductionOf(horsesType));

        colony.addGoods(horsesType, 1);
        assertEquals(4, pasture.getTotalProductionOf(horsesType));
        assertEquals(4, pasture.getMaximumProductionOf(horsesType));
        assertEquals(4, colony.getNetProductionOf(horsesType));

        pasture.upgrade();
        colony.removeGoods(horsesType);

        colony.addGoods(horsesType, 25);
        assertEquals(25, colony.getGoodsCount(horsesType));
        assertEquals(2, pasture.getTotalProductionOf(horsesType));
        assertEquals(2, pasture.getMaximumProductionOf(horsesType));
        assertEquals(2, colony.getNetProductionOf(horsesType));

        colony.addGoods(horsesType, 1);
        assertEquals(26, colony.getGoodsCount(horsesType));
        assertEquals(4, pasture.getTotalProductionOf(horsesType));
        assertEquals(4, pasture.getMaximumProductionOf(horsesType));
        assertEquals(4, colony.getNetProductionOf(horsesType));

        colony.addGoods(horsesType, 24);
        assertEquals(50, colony.getGoodsCount(horsesType));
        assertEquals(4, pasture.getTotalProductionOf(horsesType));
        assertEquals(4, pasture.getMaximumProductionOf(horsesType));
        assertEquals(4, colony.getNetProductionOf(horsesType));

        colony.addGoods(horsesType, 1);
        assertEquals(51, colony.getGoodsCount(horsesType));
        // no more than half the surplus production!
        assertEquals(4, pasture.getTotalProductionOf(horsesType));
        assertEquals(6, pasture.getMaximumProductionOf(horsesType));
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
        statesman.setType(elderStatesmanType);

        Building building = colony.getBuilding(townHallType);

        Set<Modifier> modifiers = colony.getModifierSet("model.goods.bells");
        assertEquals("Initial modifier size", 1,
                     modifiers.size());
        Modifier bellsModifier = modifiers.iterator().next();
        assertEquals("Initial modifier type", Modifier.Type.ADDITIVE,
                     bellsModifier.getType());
        assertEquals("Initial modifier value", 1.0f,
                     bellsModifier.getValue());

        assertEquals("Initial bell production", (int)bellsModifier.getValue(),
                     building.getTotalProductionOf(bellsType));

        building.add(colonist);
        colony.invalidateCache();
        // 3 from the colonist
        assertEquals("Production(Colonist)", 3,
                     building.getUnitProduction(colonist, bellsType));
        // 3(colonist) + 1(autoproduced)
        assertEquals("Total production(Colonist)", 4,
                     building.getTotalProductionOf(bellsType));

        // Add Jefferson.
        FoundingFather jefferson = spec()
            .getFoundingFather("model.foundingFather.thomasJefferson");
        modifiers = jefferson.getModifierSet("model.goods.bells");
        assertEquals("Jefferson modifier size", 1, modifiers.size());
        bellsModifier = modifiers.iterator().next();
        owner.addFather(jefferson);

        // Jefferson is a property of the player...
        assertTrue("Jefferson modifier present in player",
            colony.getOwner().getModifierSet("model.goods.bells")
                             .contains(bellsModifier));
        // ...not the colony,
        assertFalse("Jefferson modifier not present in colony",
            colony.getModifierSet("model.goods.bells")
                  .contains(bellsModifier));
        // ...but the building modifiers do have it.
        assertFalse("Jefferson modifier present in building modifiers",
            building.getModifierSet("model.goods.bells")
                    .contains(bellsModifier));

        // 3(colonist)
        assertEquals("Production(Colonist/Jefferson)", 3,
                     building.getUnitProduction(colonist, bellsType));
        // 3(colonist) + 50%(Jefferson) + 1 = 5.5
        assertEquals("Total production(Colonist/Jefferson)", 5,
                     building.getTotalProductionOf(bellsType));

        // Add statesman
        building.add(statesman);
        // 3 * 2(expert) = 6
        assertEquals("Production(Statesman/Jefferson)", 6,
                     building.getUnitProduction(statesman, bellsType));
        // 3 + 6 + 50%(Jefferson) + 1 = 14
        assertEquals("Total production(Colonist/Statesman/Jefferson)", 14,
                     building.getTotalProductionOf(bellsType));

        // Improve production
        setProductionBonus(colony, 2);
        colony.invalidateCache();
        assertEquals("Production(Colonist/Jefferson/2)", 5,
                     building.getUnitProduction(colonist, bellsType));
        assertEquals("Production(Statesman/Jefferson/2)", 10,
                     building.getUnitProduction(statesman, bellsType));
        // 5 + 10 + 50% + 1 = 23
        assertEquals("Total production(Colonist/Statesman/Jefferson/2)", 23,
                     building.getTotalProductionOf(bellsType));

        // Add newspaper
        Building newspaper = new ServerBuilding(getGame(), colony, newspaperType);
        colony.addBuilding(newspaper);
        colony.invalidateCache();
        assertEquals("Production(Colonist/Jefferson/2/Newspaper)", 5,
                     building.getUnitProduction(colonist, bellsType));
        assertEquals("Production(Statesman/Jefferson/2/Newspaper)", 10,
                     building.getUnitProduction(statesman, bellsType));
        System.err.println("NEWPAGER");
        // 5 + 10 + 50% + 100% + 1 = 45
        assertEquals("Total production(Colonist/Statesman/Jefferson/2/Newspaper)", 47,
                     building.getTotalProductionOf(bellsType));
    }

    public void testPrintingPressBonus() {
        Game game = getGame();
        game.setMap(getTestMap(true));

        Colony colony = getStandardColony(6);
        Unit unit = colony.getUnitList().get(0);
        Building building = colony.getBuilding(townHallType);

        int bellProduction = building.getTotalProductionOf(bellsType);
        int expectBellProd = 1;
        assertEquals("Wrong initial bell production",expectBellProd,bellProduction);

        Building printingPress = new ServerBuilding(getGame(), colony, printingPressType);
        colony.addBuilding(printingPress);

        bellProduction = building.getTotalProductionOf(bellsType);
        expectBellProd = 1;
        assertEquals("Wrong bell production with printing press",expectBellProd,bellProduction);

        building.add(unit);
        bellProduction = building.getTotalProductionOf(bellsType);
        expectBellProd = 6; // 1 initial plus 3 from the colonist + 2 from printing press
        assertEquals("Wrong final bell production",expectBellProd,bellProduction);
    }

    public void testNewspaperBonus() {
        Game game = getGame();
        game.setMap(getTestMap(true));

        Colony colony = getStandardColony(6);
        Unit unit = colony.getUnitList().get(0);
        Building building = colony.getBuilding(townHallType);

        int bellProduction = building.getTotalProductionOf(bellsType);
        int expectBellProd = 1;
        assertEquals("Wrong initial bell production",expectBellProd,bellProduction);

        Building newspaper = new ServerBuilding(getGame(), colony, newspaperType);
        colony.addBuilding(newspaper);

        bellProduction = building.getTotalProductionOf(bellsType);
        expectBellProd = 2;
        assertEquals("Wrong bell production with newspaper",expectBellProd,bellProduction);

        building.add(unit);
        bellProduction = building.getTotalProductionOf(bellsType);
        expectBellProd = 8; // 1 initial plus 3 from the colonist + 4 from newspaper
        assertEquals("Wrong final bell production",expectBellProd,bellProduction);
    }

    public void testCarpenterHouseNationalAdvantage() {
        Game game = getStandardGame("freecol");
        game.setMap(getTestMap(true));

        Colony colony = getStandardColony(2);
        colony.addGoods(lumberType, 100);
        Unit unit = colony.getUnitList().get(0);
        Building building = colony.getBuilding(carpenterHouseType);

        assertEquals("Production()", 0,
            building.getTotalProductionOf(hammersType));

        building.add(unit);
        colony.invalidateCache();
        assertEquals("Production(unit)", 3,
            building.getTotalProductionOf(hammersType));

        Player swedish = game.getPlayer("model.nation.swedish");
        assertNotNull("Swedes exist", swedish);
        colony.changeOwner(swedish);
        colony.invalidateCache();
        assertEquals("Production(unit/building-advantage)", 5,
            building.getTotalProductionOf(hammersType));
    }

    public void testUnitProduction() {
        Game game = getGame();
        game.setMap(getTestMap(true));

        Colony colony = getStandardColony(4);
        Unit unit = colony.getUnitList().get(0);

        for (Building building : colony.getBuildings()) {
            for (AbstractGoods output : building.getOutputs()) {
                GoodsType outputType = output.getType();
                for (UnitType type : spec().getUnitTypeList()) {
                    if (!building.getType().canAdd(type)
                        || !type.isAvailableTo(colony.getOwner())) continue;
                    unit.setType(type);
                    if (output != null) {
                        int productivity = building.getUnitProduction(unit, outputType);
                        int expected = output.getAmount();
                        if (type == building.getExpertUnitType()) {
                            expected = 6;
                        } else if (type == indenturedServantType) {
                            expected = 2;
                        } else if (type == indianConvertType) {
                            expected = 1;
                        } else if (type == pettyCriminalType) {
                            expected = 1;
                        }
                        if (expected != output.getAmount()) {
                            assertFalse("ModifierSet should not be empty!",
                                        type.getModifierSet(outputType.getId()).isEmpty());
                        }
                        assertEquals("Wrong productivity for " + type, expected,
                                     productivity);
                    }
                }
            }
        }
    }

    public void testToolsMusketProduction() {
        Game game = getGame();
        game.setMap(getTestMap(true));

        Colony colony = getStandardColony(8);
        List<Unit> units = colony.getUnitList();
        assertEquals(8, units.size());
        // make sure there are enough goods to get started
        //colony.addGoods(spec().getGoodsType("model.goods.food"), 100);
        colony.addGoods(spec().getGoodsType("model.goods.ore"), 100);
        // make sure no penalties apply
        colony.addGoods(spec().getGoodsType("model.goods.bells"),
                        Colony.LIBERTY_PER_REBEL * 3);
        colony.updatePopulation(0);

        Building smithy = colony.getBuilding(blacksmithType);
        smithy.add(units.get(0));
        smithy.add(units.get(1));
        Building armory = new ServerBuilding(game, colony, armoryType);
        colony.addBuilding(armory);
        armory.add(units.get(2));
        armory.add(units.get(3));

        assertEquals(6, smithy.getTotalProductionOf(toolsType));
        assertEquals(6, armory.getTotalProductionOf(musketsType));

        smithy.upgrade();
        armory.upgrade();

        assertEquals(12, smithy.getTotalProductionOf(toolsType));
        assertEquals(12, armory.getTotalProductionOf(musketsType));

        // make sure we can build factory level buildings
        colony.getOwner().addFather(spec().getFoundingFather("model.foundingFather.adamSmith"));

        smithy.upgrade();
        armory.upgrade();

        assertEquals(18, smithy.getTotalProductionOf(toolsType));
        //assertEquals("According to bug report #3430371, the arsenal does not enjoy "
        //            + "the usual factory level production bonus of 50%",
        //    12, armory.getTotalProductionOf(musketsType));
        // #3430371 has been reverted until we can work out what arsenal
        // did that differed from magazine
        assertEquals(18, armory.getTotalProductionOf(musketsType));
    }
}
