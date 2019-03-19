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

package net.sf.freecol.common.model;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.Modifier;
import net.sf.freecol.common.model.Modifier.ModifierType;
import net.sf.freecol.common.model.UnitLocation.NoAddReason;
import net.sf.freecol.common.model.Direction;
import net.sf.freecol.common.model.ProductionType;
import net.sf.freecol.common.model.Tile;
import static net.sf.freecol.common.util.CollectionUtils.*;
import net.sf.freecol.server.model.ServerBuilding;
import net.sf.freecol.server.model.ServerUnit;
import net.sf.freecol.util.test.FreeColTestCase;


public class BuildingTest extends FreeColTestCase {

    private static final BuildingType armoryType
        = spec().getBuildingType("model.building.armory");
    private static final BuildingType arsenalType
        = spec().getBuildingType("model.building.arsenal");
    private static final BuildingType blacksmithHouseType
        = spec().getBuildingType("model.building.blacksmithHouse");
    private static final BuildingType blacksmithShopType
        = spec().getBuildingType("model.building.blacksmithShop");
    private static final BuildingType carpenterHouseType
        = spec().getBuildingType("model.building.carpenterHouse");
    private static final BuildingType cathedralType
        = spec().getBuildingType("model.building.cathedral");
    private static final BuildingType chapelType
        = spec().getBuildingType("model.building.chapel");
    private static final BuildingType churchType
        = spec().getBuildingType("model.building.church");
    private static final BuildingType cigarFactoryType
        = spec().getBuildingType("model.building.cigarFactory");
    private static final BuildingType countryType
        = spec().getBuildingType("model.building.country");
    private static final BuildingType depotType
        = spec().getBuildingType("model.building.depot");
    private static final BuildingType distillerHouseType
        = spec().getBuildingType("model.building.distillerHouse");
    private static final BuildingType fortType
        = spec().getBuildingType("model.building.fort");
    private static final BuildingType fortressType
        = spec().getBuildingType("model.building.fortress");
    private static final BuildingType furTraderHouseType
        = spec().getBuildingType("model.building.furTraderHouse");
    private static final BuildingType furTradingPostType
        = spec().getBuildingType("model.building.furTradingPost");
    private static final BuildingType furFactoryType
        = spec().getBuildingType("model.building.furFactory");
    private static final BuildingType ironWorksType
        = spec().getBuildingType("model.building.ironWorks");
    private static final BuildingType lumberMillType
        = spec().getBuildingType("model.building.lumberMill");
    private static final BuildingType magazineType
        = spec().getBuildingType("model.building.magazine");
    private static final BuildingType newspaperType
        = spec().getBuildingType("model.building.newspaper");
    private static final BuildingType printingPressType
        = spec().getBuildingType("model.building.printingPress");
    private static final BuildingType rumDistilleryType
        = spec().getBuildingType("model.building.rumDistillery");
    private static final BuildingType rumFactoryType
        = spec().getBuildingType("model.building.rumFactory");
    private static final BuildingType schoolType
        = spec().getBuildingType("model.building.schoolhouse");
    private static final BuildingType stockadeType
        = spec().getBuildingType("model.building.stockade");
    private static final BuildingType textileMillType
        = spec().getBuildingType("model.building.textileMill");
    private static final BuildingType tobacconistHouseType
        = spec().getBuildingType("model.building.tobacconistHouse");
    private static final BuildingType tobacconistShopType
        = spec().getBuildingType("model.building.tobacconistShop");
    private static final BuildingType townHallType
        = spec().getBuildingType("model.building.townHall");
    private static final BuildingType universityType
        = spec().getBuildingType("model.building.university");
    private static final BuildingType warehouseType
        = spec().getBuildingType("model.building.warehouse");
    private static final BuildingType weaverHouseType
        = spec().getBuildingType("model.building.weaverHouse");
    private static final BuildingType weaverShopType
        = spec().getBuildingType("model.building.weaverShop");

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
    private static final GoodsType oreType
        = spec().getGoodsType("model.goods.ore");
    private static final GoodsType toolsType
        = spec().getGoodsType("model.goods.tools");

    private static final Role missionaryRole
        = spec().getRole("model.role.missionary");

    private static final TileType plainsType
        = spec().getTileType("model.tile.plains");

    private static final UnitType elderStatesmanType
        = spec().getUnitType("model.unit.elderStatesman");
    private static final UnitType expertFarmerType
        = spec().getUnitType("model.unit.expertFarmer");
    private static final UnitType firebrandPreacherType
        = spec().getUnitType("model.unit.firebrandPreacher");
    private static final UnitType freeColonistType
        = spec().getUnitType("model.unit.freeColonist");
    private static final UnitType indenturedServantType
        = spec().getUnitType("model.unit.indenturedServant");
    private static final UnitType indianConvertType
        = spec().getUnitType("model.unit.indianConvert");
    private static final UnitType masterBlacksmithType
        = spec().getUnitType("model.unit.masterBlacksmith");
    private static final UnitType masterCarpenterType
        = spec().getUnitType("model.unit.masterCarpenter");
    private static final UnitType masterDistillerType
        = spec().getUnitType("model.unit.masterDistiller");
    private static final UnitType pettyCriminalType
        = spec().getUnitType("model.unit.pettyCriminal");

    private static final Integer[] bonusLevels = new Integer[] { -2,-1,0,1,2 };


    private void clearBuilding(Building b) {
        final Colony colony = b.getColony();
        for (Unit u : b.getUnitList()) {
            u.setLocation(colony.getTile());
            u.setWorkType(grainType);
            colony.joinColony(u);
        }
        assertEquals(0, b.getUnitCount());
    }
        
    private void productionTest(BuildingType[] buildings, int[][][]values) {
        Game game = getGame();
        game.setMap(getTestMap(true));

        // Enable factory
        Colony colony = getStandardColony(8); // Big enough for factory
        colony.getOwner().addFather(spec()
            .getFoundingFather("model.foundingFather.adamSmith"));

        Building building = colony.getBuilding(buildings[0]);
        if (building == null) {
            building = new ServerBuilding(game, colony, buildings[0]);
            colony.addBuilding(building);
        }
        while (building.getType() != buildings[0]) building.upgrade();
        clearBuilding(building);
        // Add goods so production is viable
        ProductionType pt = building.getBestProductionType(false, null);
        assertNotNull(pt);
        forEach(pt.getInputs(), ag -> colony.addGoods(ag.getType(), 50));
        GoodsType outputType = first(pt.getOutputs()).getType();
        UnitType[] unitTypes = new UnitType[] {
            indianConvertType, pettyCriminalType, indenturedServantType,
            freeColonistType, building.getExpertUnitType() };
        Tile tile = colony.getTile();
        for (UnitType ut : unitTypes) {
            if (none(colony.getUnits(), matchKey(ut, Unit::getType))) {
                Unit u = find(colony.getUnits(),
                              matchKey(freeColonistType, Unit::getType));
                u.changeType(ut);
            }
        }

        StringBuilder sb = new StringBuilder(64);
        for (int i = 0; i < values.length; i++) {
            final BuildingType bt = buildings[i];

            for (int j = 0; j < values[0].length; j++) {
                final int level = bonusLevels[j];

                for (int k = 0; k < values[0][0].length; k++) {
                    final UnitType ut = unitTypes[k];

                    // Set up conditions for bt,level,ut
                    while (building.getType() != bt) building.upgrade();
                    assertEquals(building.getType(), bt);
                    clearBuilding(building);
                    colony.setProductionBonus(level);
                    Unit worker = find(colony.getUnits(),
                                       matchKey(ut, Unit::getType));
                    assertNotNull(worker);
                    worker.setWorkType(outputType);
                    worker.setLocation(building);
                    assertEquals(1, building.getUnitCount());
                    assertEquals(worker, building.getUnitList().get(0));
                    assertEquals(outputType, worker.getWorkType());
                    colony.invalidateCache();
                    assertEquals(colony.getProductionBonus(), level);

                    //System.err.println("PM " + outputType.getSuffix()+"/"+bt.getSuffix() + "/" + ut.getSuffix() + " PI=" + building.getProductionInfo());
                    //forEach(building.getProductionModifiers(outputType, ut), m -> System.err.println("M=" + m));

                    // At last! The test.
                    sb.setLength(0);
                    sb.append(outputType.getSuffix())
                        .append(" produced in ").append(bt.getSuffix())
                        .append(" at bonus ").append(String.valueOf(level))
                        .append(" with ").append(ut.getSuffix());
                    assertEquals(sb.toString(), values[i][j][k],
                                 building.getTotalProductionOf(outputType));
                }
            }
        }
    }                            
        
    public void testCanBuildNext() {
        Game game = getGame();
        game.setMap(getTestMap(true));

        // First check with a building that can be fully built with a
        // normal colony
        Colony colony = getStandardColony();
        Building warehouse = new ServerBuilding(getGame(), colony, depotType);
        colony.addBuilding(warehouse);
        assertTrue(warehouse.canBuildNext());
        assertNotNull(warehouse.upgrade());
        assertTrue(warehouse.canBuildNext());
        assertNotNull(warehouse.upgrade());
        assertFalse(warehouse.canBuildNext());

        assertNull(warehouse.upgrade());
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
        assertEquals(Colony.NoBuildReason.POPULATION_TOO_SMALL,
                     colony.getNoBuildReason(stockadeType, null));

        Unit colonist = new ServerUnit(game, colony.getTile(), colony.getOwner(), freeColonistType);
        colonist.setLocation(colony);

        assertEquals(Colony.NoBuildReason.NONE,
                     colony.getNoBuildReason(stockadeType, null));
    }

    public void testFortRequiresMinimumPopulation() {
        Game game = getGame();
        game.setMap(getTestMap(true));

        Colony colony = getStandardColony(2);
        assertEquals(Colony.NoBuildReason.POPULATION_TOO_SMALL,
                     colony.getNoBuildReason(fortType, null));

        Unit colonist = new ServerUnit(game, colony.getTile(), colony.getOwner(), freeColonistType);
        colonist.setLocation(colony);

        colony.addBuilding(new ServerBuilding(game, colony, stockadeType));
        assertEquals(Colony.NoBuildReason.NONE,
                     colony.getNoBuildReason(fortType, null));
    }

    public void testFortressRequiresMinimumPopulation() {
        Game game = getGame();
        game.setMap(getTestMap(true));

        Colony colony = getStandardColony(7);
        colony.addBuilding(new ServerBuilding(game, colony, stockadeType));
        colony.addBuilding(new ServerBuilding(game, colony, fortType));
        assertEquals(Colony.NoBuildReason.POPULATION_TOO_SMALL,
                     colony.getNoBuildReason(fortressType, null));

        Unit colonist = new ServerUnit(game, colony.getTile(), colony.getOwner(), freeColonistType);
        colonist.setLocation(colony);

        assertEquals(8, colony.getUnitCount());
        assertEquals(Colony.NoBuildReason.NONE,
                     colony.getNoBuildReason(fortressType, null));
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

        // Make a colony big enough to build a cathedral, and add enough
        // liberty to zero the production bonus.
        Colony colony = getStandardColony(10);
        while (colony.getProductionBonus() < 0) colony.modifyLiberty(100);
        Unit unit0 = colony.getUnitList().get(0);
        Unit unit1 = colony.getUnitList().get(1);
        Unit unit2 = colony.getUnitList().get(2);

        assertFalse(chapelType.hasAbility(Ability.DRESS_MISSIONARY));
        assertFalse(unit0.hasAbility(Ability.DRESS_MISSIONARY));
        assertFalse(unit0.roleIsAvailable(missionaryRole));

        Building church = colony.getBuilding(chapelType);
        assertNotNull(church);
        assertFalse(colony.hasAbility(Ability.DRESS_MISSIONARY));

        assertEquals("Chapel base cross production, no unit", 1,
            church.getBaseProduction(null, crossesType, null));
        assertEquals("Chapel potential cross production, no unit", 1,
            church.getPotentialProduction(crossesType, null));
        assertEquals("Chapel total cross production, no unit", 1,
            church.getTotalProductionOf(crossesType));

        church.upgrade();
        assertTrue("model.building.church".equals(church.getType().getId()));
        assertTrue(church.getType().hasAbility(Ability.DRESS_MISSIONARY));
        assertTrue(colony.hasAbility(Ability.DRESS_MISSIONARY));
        assertTrue(unit0.hasAbility(Ability.DRESS_MISSIONARY));
        assertTrue(unit0.roleIsAvailable(missionaryRole));

        assertEquals("Church base cross production, no unit", 2,
            church.getBaseProduction(null, crossesType, null));
        assertEquals("Church potential cross production, no unit", 2,
            church.getPotentialProduction(crossesType, null));
        assertEquals("Church cross production, no unit", 2,
            church.getTotalProductionOf(crossesType));

        church.setWorkFor(unit0);

        assertEquals("Church base cross production, free colonist", 3,
            church.getBaseProduction(church.getProductionType(), crossesType,
                                     unit0.getType()));
        assertEquals("Church potential cross production, free colonist", 3,
            church.getPotentialProduction(crossesType, unit0.getType()));
        assertEquals("Church total cross production, free colonist", 5,
            church.getTotalProductionOf(crossesType));

        church.setWorkFor(unit1);

        assertEquals("Church base cross production, 2 x free colonist", 3,
            church.getBaseProduction(church.getProductionType(), crossesType,
                                     unit1.getType()));
        assertEquals("Church potential cross production, 2 x free colonist", 3,
            church.getPotentialProduction(crossesType, unit1.getType()));
        assertEquals("Church total cross production, 2 x free colonist", 8,
            church.getTotalProductionOf(crossesType));

        church.setWorkFor(unit2);

        assertEquals("Church base cross production, 3 x free colonist", 3,
            church.getBaseProduction(church.getProductionType(), crossesType,
                                     unit2.getType()));
        assertEquals("Church potential cross production, 3 x free colonist", 3,
            church.getPotentialProduction(crossesType, unit2.getType()));
        assertEquals("Church total cross production, 3 x free colonist", 11,
            church.getTotalProductionOf(crossesType));

        Building smithy = colony.getBuilding(blacksmithHouseType);
        assertTrue(unit0.setLocation(smithy));
        assertTrue(unit1.setLocation(smithy));
        assertTrue(unit2.setLocation(smithy));
        unit0.setType(firebrandPreacherType);
        church.setWorkFor(unit0);
        
        assertEquals("Church base cross production, firebrand preacher", 3,
            church.getBaseProduction(church.getProductionType(), crossesType,
                                     unit0.getType()));
        assertEquals("Church potential cross production, firebrand preacher", 6,
            church.getPotentialProduction(crossesType, unit0.getType()));
        assertEquals("Church total cross production, firebrand preacher", 8,
            church.getTotalProductionOf(crossesType));

        unit0.setType(freeColonistType);
        assertTrue(unit0.setLocation(smithy));
        church.upgrade();
        assertTrue("model.building.cathedral".equals(church.getType().getId()));
        assertTrue(church.getType().hasAbility(Ability.DRESS_MISSIONARY));
        assertTrue(colony.hasAbility(Ability.DRESS_MISSIONARY));
        assertTrue(unit0.hasAbility(Ability.DRESS_MISSIONARY));
        assertTrue(unit0.roleIsAvailable(missionaryRole));

        assertEquals("Cathedral base cross production, no unit", 3,
            church.getBaseProduction(null, crossesType, null));
        assertEquals("Cathedral potential cross production, no unit", 3,
            church.getPotentialProduction(crossesType, null));
        assertEquals("Cathedral cross production, no colonist", 3,
            church.getTotalProductionOf(crossesType));

        church.setWorkFor(unit0);

        assertEquals("Cathedral base cross production, free colonist", 6,
            church.getBaseProduction(church.getProductionType(), crossesType,
                                     unit0.getType()));
        assertEquals("Cathedral potential cross production, free colonist", 6,
            church.getPotentialProduction(crossesType, unit0.getType()));
        assertEquals("Cathedral total cross production, free colonist", 9,
            church.getTotalProductionOf(crossesType));

        church.setWorkFor(unit1);

        assertEquals("Cathedral base cross production, 2 x free colonist", 6,
            church.getBaseProduction(church.getProductionType(), crossesType,
                                     unit1.getType()));
        assertEquals("Cathedral potential cross production, 2 x free colonist", 6,
            church.getPotentialProduction(crossesType, unit1.getType()));
        assertEquals("Cathedral total cross production, 2 x free colonist", 15,
            church.getTotalProductionOf(crossesType));

        church.setWorkFor(unit2);

        assertEquals("Cathedral base cross production, 3 x free colonist", 6,
            church.getBaseProduction(church.getProductionType(), crossesType,
                                     unit2.getType()));
        assertEquals("Cathedral potential cross production, 3 x free colonist", 6,
            church.getPotentialProduction(crossesType, unit2.getType()));
        assertEquals("Cathedral total cross production, 3 x free colonist", 21,
            church.getTotalProductionOf(crossesType));

        unit0.setLocation(smithy);
        unit1.setLocation(smithy);
        unit2.setLocation(smithy);
        unit0.setType(firebrandPreacherType);
        church.setWorkFor(unit0);

        assertEquals("Cathedral base cross production, firebrand preacher", 6,
            church.getBaseProduction(church.getProductionType(), crossesType,
                                     unit0.getType()));
        assertEquals("Cathedral potential cross production, firebrand preacher", 12,
            church.getPotentialProduction(crossesType, unit0.getType()));
        assertEquals("Cathedral total cross production, firebrand preacher", 15,
            church.getTotalProductionOf(crossesType));
    }

    public void testCanAddToBuilding() {
        Game game = getGame();
        game.setMap(getTestMap(true));

        Colony colony = getStandardColony(6);
        Tile tile = colony.getTile();
        List<Unit> units = colony.getUnitList();

        // Standard colony tends to place units in the town hall
        for (Unit u : units) {
            if (u.getLocation() instanceof Building) u.setLocation(tile);
        }

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
     * FIXME: make this more generic.
     */
    public void testCanAddToSchool() {
        Game game = getGame();
        game.setMap(getTestMap(true));

        Colony colony = getStandardColony(10);
        Iterator<Unit> units = colony.getUnitList().iterator();

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
        assertEquals(NoAddReason.MINIMUM_SKILL, school.getNoAddReason(colonist));
        assertFalse("able to add free colonist to Schoolhouse",
                    school.canAdd(colonist));

        assertEquals(NoAddReason.MINIMUM_SKILL, school.getNoAddReason(criminal));
        assertFalse("able to add petty criminal to Schoolhouse",
                    school.canAdd(criminal));

        assertEquals(NoAddReason.MINIMUM_SKILL, school.getNoAddReason(servant));
        assertFalse("able to add indentured servant to Schoolhouse",
                    school.canAdd(servant));

        assertEquals(NoAddReason.MINIMUM_SKILL, school.getNoAddReason(indian));
        assertFalse("able to add indian convert to Schoolhouse",
                    school.canAdd(indian));

        assertEquals(NoAddReason.MAXIMUM_SKILL, school.getNoAddReason(elder));
        assertFalse("able to add elder statesman to Schoolhouse",
                    school.canAdd(elder));

        assertEquals(NoAddReason.MAXIMUM_SKILL, school.getNoAddReason(distiller));
        assertFalse("able to add master distiller to Schoolhouse",
                    school.canAdd(distiller));

        assertTrue("unable to add master farmer to Schoolhouse",
                   school.canAdd(farmer));
        farmer.setLocation(school);
        assertEquals(school, farmer.getLocation());

        assertFalse("able to add master carpenter to Schoolhouse",
                    school.canAdd(carpenter));

        school.upgrade();
        farmer.setLocation(colony.getTile());

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
        distiller.setLocation(school);
        assertTrue("unable to add master farmer to College",
                   school.canAdd(farmer));
        farmer.setLocation(school);
        assertFalse("able to add master carpenter to College",
                    school.canAdd(carpenter));

        school.upgrade();
        assertEquals(school.getType().toString(), universityType,
                     school.getType());
        distiller.setLocation(colony.getTile());
        farmer.setLocation(colony.getTile());

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
        elder.setLocation(school);
        assertTrue("unable to add master distiller to University",
                   school.canAdd(distiller));
        distiller.setLocation(school);
        assertTrue("unable to add master farmer to University",
                   school.canAdd(farmer));
        farmer.setLocation(school);
        assertFalse("able to add master carpenter to University",
                    school.canAdd(carpenter));
    }

    public void testSerialize() {
        Game game = getGame();
        game.setMap(getTestMap(true));

        Colony colony = getStandardColony(6);
        for (Building building : colony.getBuildings()) {
            try (StringWriter sw = new StringWriter();
                FreeColXMLWriter xw = new FreeColXMLWriter(sw,
                    FreeColXMLWriter.WriteScope.toSave(), false)) {
                building.toXML(xw);
            } catch (IOException|XMLStreamException ex) {
                fail(ex.toString());
            }
        }
    }

    public void testStockade() {
        final Game game = getGame();
        game.setMap(getTestMap(true));

        final Turn turn = game.getTurn();
        List<Modifier> modifiers;
        Colony colony = getStandardColony(2);
        modifiers = toList(colony.getModifiers(Modifier.DEFENCE));
        assertEquals(1, modifiers.size());
        Modifier modifier = first(modifiers);
        assertEquals(50f, modifier.getValue());
        assertEquals(ModifierType.PERCENTAGE, modifier.getType());

        modifiers = toList(stockadeType.getModifiers(Modifier.DEFENCE));
        assertEquals(1, modifiers.size());
        modifier = first(modifiers);
        assertEquals(100f, modifier.getValue());
        assertEquals(ModifierType.PERCENTAGE, modifier.getType());
        assertEquals(0f, stockadeType.apply(0f, turn, Modifier.MINIMUM_COLONY_SIZE));

        modifiers = toList(fortType.getModifiers(Modifier.DEFENCE));
        assertEquals(1, modifiers.size());
        modifier = first(modifiers);
        assertEquals(150f, modifier.getValue());
        assertEquals(ModifierType.PERCENTAGE, modifier.getType());
        assertEquals(0f, stockadeType.apply(0f, turn, Modifier.MINIMUM_COLONY_SIZE));

        modifiers = toList(fortressType.getModifiers(Modifier.DEFENCE));
        assertEquals(1, modifiers.size());
        modifier = first(modifiers);
        assertEquals(200f, modifier.getValue());
        assertEquals(ModifierType.PERCENTAGE, modifier.getType());
        assertEquals(0f, stockadeType.apply(0f, turn, Modifier.MINIMUM_COLONY_SIZE));
    }

    public void testCottonClothProduction() {
        Game game = getGame();
        game.setMap(getTestMap(true));

        Colony colony = getStandardColony(2);
        List<Unit> units = colony.getUnitList();
        Unit colonist = units.get(0);
        Unit worker = units.get(1);

        final Building weaver = colony.getBuilding(weaverHouseType);

        assertTrue(colonist.getLocation() instanceof ColonyTile);
        assertEquals(plainsType, colonist.getWorkTile().getType());
        assertTrue(worker.getLocation() instanceof ColonyTile);
        assertEquals(plainsType, worker.getWorkTile().getType());

        assertTrue(weaver.add(worker));
        assertEquals(worker, weaver.getUnitList().get(0));

        colony.addGoods(cottonType, 2);
        colony.invalidateCache();

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

    public void testPasture() {
        Game game = getGame();
        game.setMap(getTestMap(true));

        Colony colony = getStandardColony(1);
        Building country = colony.getBuilding(countryType);
        assertNotNull(country.getProductionType());

        Unit unit = colony.getFirstUnit();
        assertEquals(grainType, unit.getWorkType());

        // no horses yet
        assertEquals(8, colony.getNetProductionOf(foodType));
        assertEquals(0, country.getTotalProductionOf(horsesType));
        assertEquals(0, colony.getNetProductionOf(horsesType));
        assertEquals(0, country.getMaximumProductionOf(horsesType));

        colony.addGoods(horsesType, 50);
        colony.invalidateCache();

        assertEquals(2, country.getTotalProductionOf(horsesType));
        assertEquals(2, country.getMaximumProductionOf(horsesType));
        assertEquals(2, colony.getNetProductionOf(horsesType));

        colony.addGoods(horsesType, 1);
        colony.invalidateCache();

        assertEquals(4, country.getTotalProductionOf(horsesType));
        assertEquals(4, country.getMaximumProductionOf(horsesType));
        assertEquals(4, colony.getNetProductionOf(horsesType));

        country.upgrade();
        colony.removeGoods(horsesType);
        colony.addGoods(horsesType, 25);
        colony.invalidateCache();

        assertEquals(25, colony.getGoodsCount(horsesType));
        assertEquals(2, country.getTotalProductionOf(horsesType));
        assertEquals(2, country.getMaximumProductionOf(horsesType));
        assertEquals(2, colony.getNetProductionOf(horsesType));

        colony.addGoods(horsesType, 1);
        colony.invalidateCache();

        assertEquals(26, colony.getGoodsCount(horsesType));
        assertEquals(4, country.getTotalProductionOf(horsesType));
        assertEquals(4, country.getMaximumProductionOf(horsesType));
        assertEquals(4, colony.getNetProductionOf(horsesType));

        colony.addGoods(horsesType, 24);
        colony.invalidateCache();

        assertEquals(50, colony.getGoodsCount(horsesType));
        assertEquals(4, country.getTotalProductionOf(horsesType));
        assertEquals(4, country.getMaximumProductionOf(horsesType));
        assertEquals(4, colony.getNetProductionOf(horsesType));

        colony.addGoods(horsesType, 1);
        colony.invalidateCache();

        assertEquals(51, colony.getGoodsCount(horsesType));
        // no more than half the surplus production!
        assertEquals(4, country.getTotalProductionOf(horsesType));
        assertEquals(6, country.getMaximumProductionOf(horsesType));
        assertEquals("Horse production should equal food surplus.",
                     colony.getNetProductionOf(foodType),
                     colony.getNetProductionOf(horsesType));
    }

    public void testTownhallProduction() {
        final Game game = getGame();
        game.setMap(getTestMap(true));

        final Turn turn = game.getTurn();
        Colony colony = getStandardColony(6);
        Player owner = colony.getOwner();
        Unit colonist = colony.getUnitList().get(0);
        Unit statesman = colony.getUnitList().get(1);
        statesman.setType(elderStatesmanType);

        Tile tile = colony.getTile();
        Building building = colony.getBuilding(townHallType);
        for (Unit u : building.getUnitList()) u.setLocation(tile);

        assertTrue("No initial modifiers",
                   none(colony.getModifiers("model.goods.bells")));
        assertEquals("Initial bell production", 1,
                     building.getTotalProductionOf(bellsType));

        colonist.setLocation(building);
        // 3 from the colonist
        assertEquals("Production(Colonist)", 3,
                     building.getUnitProduction(colonist, bellsType));
        // 3(colonist) + 1(unattended)
        assertEquals("Total production(Colonist)", 4,
                     building.getTotalProductionOf(bellsType));

        // Add Jefferson.
        FoundingFather jefferson = spec()
            .getFoundingFather("model.foundingFather.thomasJefferson");
        List<Modifier> modifiers = toList(jefferson.getModifiers("model.goods.bells"));
        assertEquals("Jefferson modifier size", 1, modifiers.size());
        Modifier bellsModifier = first(modifiers);
        owner.addFather(jefferson);

        // Jefferson is a property of the player...
        assertTrue("Jefferson should be present in player",
            any(owner.getModifiers("model.goods.bells"),
                m -> m == bellsModifier));
        assertTrue("Jefferson should be present in player in building scope",
            any(owner.getModifiers("model.goods.bells", townHallType, turn),
                m -> m ==  bellsModifier));
        assertFalse("Jefferson should not be present in player in unit scope",
            any(owner.getModifiers("model.goods.bells", freeColonistType, turn),
                m -> m == bellsModifier));
        // ...not the colony,
        assertFalse("Jefferson modifier should not be present in colony",
            any(colony.getModifiers("model.goods.bells"),
                m -> m == bellsModifier));
        // ...and the building modifiers do not have it.
        assertFalse("Jefferson modifier should not be present in building modifiers",
            any(building.getModifiers("model.goods.bells"),
                m -> m == bellsModifier));

        // 3(colonist)
        assertEquals("Production(Colonist/Jefferson)", 3,
                     building.getUnitProduction(colonist, bellsType));
        // 3(colonist) + 1 + 50%(Jefferson) = 6
        assertEquals("Total production(Colonist/Jefferson)", 6,
                     building.getTotalProductionOf(bellsType));

        // Add statesman
        statesman.setLocation(building);
        // 3 * 2(expert) = 6
        assertEquals("Production(Statesman/Jefferson)", 6,
                     building.getUnitProduction(statesman, bellsType));
        // 3 + 6 + 1 + 50%(Jefferson) = 15
        assertEquals("Total production(Colonist/Statesman/Jefferson)", 15,
                     building.getTotalProductionOf(bellsType));

        // Improve production
        setProductionBonus(colony, 2);
        colony.invalidateCache();
        assertEquals("Production(Colonist/Jefferson/2)", 5,
                     building.getUnitProduction(colonist, bellsType));
        assertEquals("Production(Statesman/Jefferson/2)", 10,
                     building.getUnitProduction(statesman, bellsType));
        // 5 + 10 + 1 + 50%(Jefferson) = 24
        assertEquals("Total production(Colonist/Statesman/Jefferson/2)", 24,
                     building.getTotalProductionOf(bellsType));

        // Add newspaper
        Building newspaper = new ServerBuilding(getGame(), colony,
                                                newspaperType);
        colony.addBuilding(newspaper);
        colony.invalidateCache();
        assertEquals("Production(Colonist/Jefferson/2/Newspaper)", 5,
                     building.getUnitProduction(colonist, bellsType));
        assertEquals("Production(Statesman/Jefferson/2/Newspaper)", 10,
                     building.getUnitProduction(statesman, bellsType));
        // 5 + 10 + 1 + 50%(Jefferson) + 100%(Newspaper) = 48
        assertEquals("Total production(Colonist/Statesman/Jefferson/2/Newspaper)", 48,
                     building.getTotalProductionOf(bellsType));
    }

    public void testPrintingPressBonus() {
        Game game = getGame();
        game.setMap(getTestMap(true));

        Colony colony = getStandardColony(6);
        Unit unit = colony.getUnitList().get(0);
        Building building = colony.getBuilding(townHallType);
        Tile tile = colony.getTile();
        for (Unit u : building.getUnitList()) u.setLocation(tile);

        int bellProduction = building.getTotalProductionOf(bellsType);
        int expectBellProd = 1;
        assertEquals("Wrong initial bell production",expectBellProd,bellProduction);

        Building printingPress = new ServerBuilding(getGame(), colony, printingPressType);
        colony.addBuilding(printingPress);

        bellProduction = building.getTotalProductionOf(bellsType);
        expectBellProd = 1;
        assertEquals("Wrong bell production with printing press",expectBellProd,bellProduction);

        unit.setLocation(building);
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
        clearWorkLocation(building);

        int bellProduction = building.getTotalProductionOf(bellsType);
        int expectBellProd = 1;
        assertEquals("Wrong initial bell production", expectBellProd,
                     bellProduction);

        Building newspaper = new ServerBuilding(getGame(), colony,
                                                newspaperType);
        colony.addBuilding(newspaper);
        colony.invalidateCache();

        bellProduction = building.getTotalProductionOf(bellsType);
        expectBellProd = 2;
        assertEquals("Wrong bell production with newspaper", expectBellProd,
                     bellProduction);

        unit.setLocation(building);
        bellProduction = building.getTotalProductionOf(bellsType);
        expectBellProd = 8; // 1 initial plus 3 from the colonist + 4 from newspaper
        assertEquals("Wrong final bell production", expectBellProd,
                     bellProduction);
    }

    public void testUnitProduction() {
        Game game = getGame();
        game.setMap(getTestMap(true));

        Colony colony = getStandardColony(4);
        Unit unit = colony.getUnitList().get(0);
        for (Building building : colony.getBuildings()) {
            clearWorkLocation(building);
            unit.setLocation(building);
            for (AbstractGoods output : iterable(building.getOutputs())) {
                GoodsType outputType = output.getType();
                for (UnitType type : transform(spec().getUnitTypeList(),
                        ut -> (building.getType().canAdd(ut)
                            && ut.isAvailableTo(colony.getOwner())))) {
                    unit.changeType(type);
                    if (output != null) {
                        int productivity = building.getUnitProduction(unit, outputType);
                        int expect = (type == building.getExpertUnitType()) ? 6
                            : (type == indenturedServantType) ? 2
                            : (type == indianConvertType) ? 1
                            : (type == pettyCriminalType) ? 1
                            : output.getAmount();
                        if (expect != output.getAmount()) {
                            assertTrue("Modifiers expected",
                                count(type.getModifiers(outputType.getId())) > 0);
                        }
                        assertEquals("Wrong productivity for " + type
                            + " in " + building, expect, productivity);
                    }
                }
            }
        }
    }

    // Lumber and cross production data contributed by Lone_Wolf in BR#2981.
    private static int lumberProd[][][] = {
        { // carpenterHouse
            { 0, 0, 0, 1, 4 }, // -2
            { 0, 0, 1, 2, 5 }, // -1
            { 1, 1, 2, 3, 6 }, // 0
            { 2, 2, 3, 4, 7 }, // +1
            { 3, 3, 4, 5, 8 }, // +2
        }, { // lumber mill
            { 0, 0, 0, 2, 8 }, // -2
            { 0, 0, 2, 4, 10 }, // -1
            { 2, 2, 4, 6, 12 }, // 0
            { 4, 4, 6, 8, 14 }, // +1
            { 6, 6, 8, 10, 16 }, // +2
        }
    };
    private static final BuildingType[] lumberBuildings = new BuildingType[] {
        carpenterHouseType, lumberMillType };
    public void testLumberProduction() {
        productionTest(lumberBuildings, lumberProd);
    }

    // Cross production is very like lumber production, with the addition of
    // the unattended production.
    private static int crossProd[][][] = {
        { // church
            { 2, 2, 2, 3, 6 }, // -2
            { 2, 2, 3, 4, 7 }, // -1
            { 3, 3, 4, 5, 8 }, // 0
            { 4, 4, 5, 6, 9 }, // +1
            { 5, 5, 6, 7, 10 }, // +2
        }, { // cathedral
            { 3, 3, 3, 5, 11 }, // -2
            { 3, 3, 5, 7, 13 }, // -1
            { 5, 5, 7, 9, 15 }, // 0
            { 7, 7, 9, 11, 17 }, // +1
            { 9, 9, 11, 13, 19 }, // +2
        }
    };
    private static final BuildingType[] crossBuildings = new BuildingType[] {
        churchType, cathedralType };
    public void testCrossProduction() {
        productionTest(crossBuildings, crossProd);
    }
    
    // Factory production data contributed by Lone_Wolf in BR#2979.
    private static int factoryProd[][][] = {
        { // house
            { 0, 0, 0, 1, 2 }, // -2
            { 0, 0, 1, 2, 4 }, // -1
            { 1, 1, 2, 3, 6 }, // 0
            { 2, 2, 3, 4, 8 }, // 1
            { 3, 3, 4, 5, 10 }, // 2
        }, { // shop
            { 0, 0, 2, 4, 8 }, // -2
            { 1, 1, 3, 5, 10 }, // -1
            { 2, 2, 4, 6, 12 }, // 0
            { 3, 3, 5, 7, 14 }, // 1
            { 4, 4, 6, 8, 16 }, // 2
        }, { // factory
            { 0, 0, 3, 6, 12 }, // -2
            { 1, 1, 4, 7, 14 }, // -1
            { 3, 3, 6, 9, 18 }, // 0
            { 4, 4, 7, 10, 20 }, // 1
            { 6, 6, 9, 12, 24 }, // 2
        }
    };
    private static final BuildingType[] clothBuildings = new BuildingType[] {
        weaverHouseType, weaverShopType, textileMillType };
    private static final BuildingType[] furBuildings = new BuildingType[] {
        furTraderHouseType, furTradingPostType, furFactoryType };
    private static final BuildingType[] sugarBuildings = new BuildingType[] {
        distillerHouseType, rumDistilleryType, rumFactoryType };
    private static final BuildingType[] tobaccoBuildings = new BuildingType[] {
        tobacconistHouseType, tobacconistShopType, cigarFactoryType };
    private static final BuildingType[] toolBuildings = new BuildingType[] {
        blacksmithHouseType, blacksmithShopType, ironWorksType };
    private static final BuildingType[] musketBuildings = new BuildingType[] {
        armoryType, magazineType, arsenalType };
    public void testFactoryProduction() {
        productionTest(clothBuildings, factoryProd);
        productionTest(clothBuildings, factoryProd);
        productionTest(sugarBuildings, factoryProd);
        productionTest(tobaccoBuildings, factoryProd);
        productionTest(toolBuildings, factoryProd);
        productionTest(musketBuildings, factoryProd);
    }
        
    public void testToolsMusketProduction() {
        // Test the interaction between tools and muskets
        Game game = getGame();
        game.setMap(getTestMap(true));

        Colony colony = getStandardColony(8);
        Tile tile = colony.getTile();
        List<Unit> units = colony.getUnitList();
        assertEquals(8, units.size());

        // Make sure there are enough goods to get started, and make
        // sure enough bells and food are being produced.
        // colony.addGoods(spec().getGoodsType("model.goods.food"), 100);
        colony.addGoods(bellsType, Colony.LIBERTY_PER_REBEL * 3);
        units.get(0).setLocation(tile);
        units.get(1).setLocation(tile);
        units.get(2).setLocation(tile);
        units.get(3).setLocation(tile);
        assertTrue(colony.getColonyTile(tile.getNeighbourOrNull(Direction.N))
            .setWorkFor(units.get(4)));
        assertTrue(colony.getColonyTile(tile.getNeighbourOrNull(Direction.E))
            .setWorkFor(units.get(5)));
        assertTrue(colony.getColonyTile(tile.getNeighbourOrNull(Direction.S))
            .setWorkFor(units.get(6)));
        assertTrue(colony.getBuilding(townHallType)
            .setWorkFor(units.get(7)));

        Building smithy = colony.getBuilding(blacksmithHouseType);
        assertFalse(smithy.setWorkFor(units.get(0)));
        colony.addGoods(oreType, 50); // Add ore so the smithy becomes viable
        assertTrue(smithy.setWorkFor(units.get(0)));
        assertTrue(smithy.setWorkFor(units.get(1)));

        Building armory = new ServerBuilding(game, colony, armoryType);
        colony.addBuilding(armory);
        colony.invalidateCache();

        assertTrue(armory.setWorkFor(units.get(2)));
        assertTrue(armory.setWorkFor(units.get(3)));

        assertEquals(toolsType,   units.get(0).getWorkType());
        assertEquals(toolsType,   units.get(1).getWorkType());
        assertEquals(musketsType, units.get(2).getWorkType());
        assertEquals(musketsType, units.get(3).getWorkType());
        assertEquals(6, smithy.getTotalProductionOf(toolsType));
        assertEquals(6, armory.getTotalProductionOf(musketsType));

        // Upgrade the buildings
        smithy.upgrade();
        armory.upgrade();
        colony.invalidateCache();

        assertEquals(toolsType,   units.get(0).getWorkType());
        assertEquals(toolsType,   units.get(1).getWorkType());
        assertEquals(musketsType, units.get(2).getWorkType());
        assertEquals(musketsType, units.get(3).getWorkType());
        assertEquals(12, smithy.getTotalProductionOf(toolsType));
        assertEquals(12, armory.getTotalProductionOf(musketsType));

        // Upgrade to factory level
        colony.getOwner().addFather(spec()
            .getFoundingFather("model.foundingFather.adamSmith"));
        smithy.upgrade();
        armory.upgrade();
        colony.invalidateCache();

        assertEquals(toolsType,   units.get(0).getWorkType());
        assertEquals(toolsType,   units.get(1).getWorkType());
        assertEquals(musketsType, units.get(2).getWorkType());
        assertEquals(musketsType, units.get(3).getWorkType());
        assertEquals(18, smithy.getTotalProductionOf(toolsType));
        //assertEquals("According to bug report #3430371, the arsenal does not enjoy "
        //            + "the usual factory level production bonus of 50%",
        //    12, armory.getTotalProductionOf(musketsType));
        // #3430371 has been reverted until we can work out what arsenal
        // did that differed from magazine
        assertEquals(18, armory.getTotalProductionOf(musketsType));
    }
}
