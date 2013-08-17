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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;

import net.sf.freecol.common.model.Role;
import net.sf.freecol.server.model.ServerColony;
import net.sf.freecol.server.model.ServerUnit;
import net.sf.freecol.util.test.FreeColTestCase;

import org.w3c.dom.Document;
import org.w3c.dom.Element;


public class UnitTest extends FreeColTestCase {

    private static final BuildingType carpenterHouseType
        = spec().getBuildingType("model.building.carpenterHouse");
    private static final BuildingType churchType
        = spec().getBuildingType("model.building.chapel");

    private static final EquipmentType horsesEquipmentType
        = spec().getEquipmentType("model.equipment.horses");
    private static final EquipmentType missionaryEquipmentType
        = spec().getEquipmentType("model.equipment.missionary");
    private static final EquipmentType musketsEquipmentType
        = spec().getEquipmentType("model.equipment.muskets");
    private static final EquipmentType toolsEquipmentType
        = spec().getEquipmentType("model.equipment.tools");

    private static final GoodsType cottonType
        = spec().getGoodsType("model.goods.cotton");
    private static final GoodsType foodType
        = spec().getPrimaryFoodType();

    private static final TileType ocean
        = spec().getTileType("model.tile.ocean");
    private static final TileType plains
        = spec().getTileType("model.tile.plains");

    private static final UnitType artilleryType
        = spec().getUnitType("model.unit.artillery");
    private static final UnitType braveType
        = spec().getUnitType("model.unit.brave");
    private static final UnitType caravelType
        = spec().getUnitType("model.unit.caravel");
    private static final UnitType colonialRegularType
        = spec().getUnitType("model.unit.colonialRegular");
    private static final UnitType colonistType
        = spec().getUnitType("model.unit.freeColonist");
    private static final UnitType expertFarmerType
        = spec().getUnitType("model.unit.expertFarmer");
    private static final UnitType frigateType
        = spec().getUnitType("model.unit.frigate");
    private static final UnitType galleonType
        = spec().getUnitType("model.unit.galleon");
    private static final UnitType hardyPioneerType
        = spec().getUnitType("model.unit.hardyPioneer");
    private static final UnitType indianConvertType
        = spec().getUnitType("model.unit.indianConvert");
    private static final UnitType jesuitMissionaryType
        = spec().getUnitType("model.unit.jesuitMissionary");
    private static final UnitType kingsRegularType
        = spec().getUnitType("model.unit.kingsRegular");
    private static final UnitType merchantmanType
        = spec().getUnitType("model.unit.merchantman");
    private static final UnitType seasonedScoutType
        = spec().getUnitType("model.unit.seasonedScout");
    private static final UnitType treasureTrainType
        = spec().getUnitType("model.unit.treasureTrain");
    private static final UnitType revengerType
        = spec().getUnitType("model.unit.revenger");
    private static final UnitType undeadType
        = spec().getUnitType("model.unit.undead");
    private static final UnitType veteranSoldierType
        = spec().getUnitType("model.unit.veteranSoldier");
    private static final UnitType wagonType
        = spec().getUnitType("model.unit.wagonTrain");

    private static final Role role_default
        = spec().getRole("model.role.default");
    private static final Role role_scout
        = spec().getRole("model.role.scout");
    private static final Role role_soldier
        = spec().getRole("model.role.soldier");
    private static final Role role_dragoon
        = spec().getRole("model.role.dragoon");
    private static final Role role_pioneer
        = spec().getRole("model.role.pioneer");
    private static final Role role_missionary
        = spec().getRole("model.role.missionary");


    /**
     * Test unit for colonist status
     *
     */
    public void testIsColonist() {
        Game game = getStandardGame();
        Player dutch = game.getPlayer("model.nation.dutch");
        Player sioux = game.getPlayer("model.nation.sioux");
        Map map = getTestMap(plains, true);
        game.setMap(map);

        Tile tile1 = map.getTile(6, 8);
        Tile tile2 = map.getTile(6, 9);

        Unit merchantman = new ServerUnit(game, tile1, dutch, merchantmanType);
        assertFalse("Merchantman is not a colonist", merchantman.isColonist());

        Unit soldier = new ServerUnit(game, tile1, dutch, veteranSoldierType);
        assertTrue("A soldier is a colonist", soldier.isColonist());

        Unit brave = new ServerUnit(game, tile2, sioux, braveType);
        assertFalse("A brave is not a colonist", brave.isColonist());
    }

    public void testCanAdd() {
        Game game = getStandardGame();
        Player dutch = game.getPlayer("model.nation.dutch");

        Unit galleon = new ServerUnit(game, null, dutch, galleonType);
        Unit caravel = new ServerUnit(game, null, dutch, caravelType);
        Unit colonist = new ServerUnit(game, null, dutch, colonistType);
        Unit wagonTrain = new ServerUnit(game, null, dutch, wagonType);
        Unit treasureTrain = new ServerUnit(game, null, dutch, 
                                            treasureTrainType);

        // tests according to standard rules
        assertTrue(galleon.canAdd(colonist));
        assertTrue(galleon.canAdd(treasureTrain));

        assertFalse(galleon.canAdd(wagonTrain));
        assertFalse(galleon.canAdd(caravel));
        assertFalse(galleon.canAdd(galleon));

        assertTrue(caravel.canAdd(colonist));

        assertFalse(caravel.canAdd(wagonTrain));
        assertFalse(caravel.canAdd(treasureTrain));
        assertFalse(caravel.canAdd(caravel));
        assertFalse(caravel.canAdd(galleon));

        // Save old specification values to restore after test
        int wagonTrainOldSpace = wagonTrain.getCargoCapacity();
        int wagonTrainOldSpaceTaken = wagonTrain.getCargoCapacity();
        int caravelOldSpaceTaken = caravel.getCargoCapacity();

        // tests according to other possible rules
        wagonTrain.getType().setSpace(1);
        wagonTrain.getType().setSpaceTaken(2);
        caravel.getType().setSpaceTaken(1);

        assertTrue(galleon.canAdd(wagonTrain));
        assertTrue(caravel.canAdd(wagonTrain));
        // this may seem strange, but ships do carry smaller boats
        assertTrue(galleon.canAdd(caravel));
        assertFalse(caravel.canAdd(caravel));

        // restore values to not affect other tests
        wagonTrain.getType().setSpace(wagonTrainOldSpace);
        wagonTrain.getType().setSpaceTaken(wagonTrainOldSpaceTaken);
        caravel.getType().setSpaceTaken(caravelOldSpaceTaken);
    }

    public void testFailedAddGoods() {
        Game game = getStandardGame();
        Map map = getTestMap();
        game.setMap(map);

        Colony colony = this.getStandardColony();
        int foodInColony = 300;
        colony.addGoods(foodType, foodInColony);
        assertEquals("Setup error, colony does goods quantity wrong",
                     foodInColony, colony.getGoodsCount(foodType));

        Player dutch = game.getPlayer("model.nation.dutch");
        Unit wagonTrain = new ServerUnit(game, colony.getTile(), dutch, 
                                         wagonType);
        assertEquals("Setup error, unit should not carry anything", 0,
                     wagonTrain.getGoodsSpaceTaken());
        assertEquals("Setup error, unit has wrong initial moves",
                     wagonTrain.getInitialMovesLeft(),
                     wagonTrain.getMovesLeft());

        int tooManyGoods = colony.getGoodsCount(foodType);
        assertFalse("Can not add too many goods",
            wagonTrain.add(new Goods(game, null, foodType, tooManyGoods)));
        assertEquals("Unit should not carry anything", 0,
                     wagonTrain.getGoodsSpaceTaken());
        assertEquals("Unit moves should not have been modified",
                     wagonTrain.getInitialMovesLeft(),
                     wagonTrain.getMovesLeft());
    }

    public void testMissionary() {
        Game game = getStandardGame();
        Map map = getTestMap(plains, true);
        game.setMap(map);
        Player sioux = game.getPlayer("model.nation.sioux");
        Player dutch = game.getPlayer("model.nation.dutch");
        Tile tile = map.getTile(6, 9);

        Colony colony = getStandardColony(3);
        Building church = colony.getBuilding(churchType);
        church.upgrade();
        Unit jesuit = new ServerUnit(game, tile, dutch, jesuitMissionaryType);
        Unit colonist = new ServerUnit(game, colony, dutch, colonistType);

        // check abilities
        assertFalse(colonist.hasAbility(Ability.MISSIONARY));
        colonist.changeEquipment(missionaryEquipmentType, 1);
        assertTrue(colonist.hasAbility(Ability.MISSIONARY));
        assertFalse(colonist.hasAbility(Ability.EXPERT_MISSIONARY));
        assertTrue(jesuit.hasAbility(Ability.MISSIONARY));
        assertTrue(jesuit.hasAbility(Ability.EXPERT_MISSIONARY));

        // check mission creation
        FreeColTestCase.IndianSettlementBuilder builder
            = new FreeColTestCase.IndianSettlementBuilder(game);
        IndianSettlement s = builder.player(sioux).settlementTile(tile)
            .capital(true).build();
        s.setContacted(dutch);

        // add the missionary
        s.setMissionary(jesuit);
        assertTrue("No missionary set", s.hasMissionary());
        assertEquals("Wrong missionary set", s.getMissionary(), jesuit);
        s.setMissionary(null);
        assertFalse("Missionary not removed", s.hasMissionary());
    }

    public void testLineOfSight() {
        Game game = getStandardGame();
        Map map = getTestMap(plains, true);
        game.setMap(map);
        Player player = game.getPlayer("model.nation.dutch");
        Tile tile = map.getTile(6, 9);

        Unit frigate = new ServerUnit(game, tile, player, frigateType);
        assertEquals(2, frigate.getLineOfSight());
        assertTrue(frigate.hasAbility(Ability.NAVAL_UNIT));

        Unit revenger = new ServerUnit(game, tile, player, revengerType);
        assertEquals(3, revenger.getLineOfSight());

        Unit colonist = new ServerUnit(game, tile, player, colonistType);
        assertEquals(1, colonist.getLineOfSight());
        assertTrue(colonist.hasAbility(Ability.CAN_BE_EQUIPPED));

        assertTrue(colonist.canBeEquippedWith(horsesEquipmentType));
        colonist.changeEquipment(horsesEquipmentType, 1);
        assertEquals(2, colonist.getLineOfSight());

        // with Hernando De Soto, land units should see further
        FoundingFather father
            = spec().getFoundingFather("model.foundingFather.hernandoDeSoto");
        player.addFather(father);

        assertEquals(2, frigate.getLineOfSight());  // should not increase
        assertEquals(4, revenger.getLineOfSight()); // should get +1 bonus
        assertEquals(3, colonist.getLineOfSight()); // should get +1 bonus
    }

    public void testUnitCanBuildColony() {
        Game game = getStandardGame();
        Player dutch = game.getPlayer("model.nation.dutch");
        Player sioux = game.getPlayer("model.nation.sioux");

        Map map = getTestMap(plains, true);
        game.setMap(map);
        Tile tile1 = map.getTile(10, 4);

        Unit farmer = new ServerUnit(game, tile1, dutch, expertFarmerType);
        assertTrue(farmer.canBuildColony());

        Unit arty = new ServerUnit(game, tile1, dutch, artilleryType);
        assertFalse(arty.canBuildColony());

        Unit ship = new ServerUnit(game, tile1, dutch, galleonType);
        assertFalse(ship.canBuildColony());

        Unit treasure = new ServerUnit(game, tile1, dutch, treasureTrainType);
        assertFalse(treasure.canBuildColony());

        Unit wagon = new ServerUnit(game, tile1, dutch, wagonType);
        assertFalse(wagon.canBuildColony());

        Unit indianConvert = new ServerUnit(game, tile1, dutch,
                                            indianConvertType);
        assertFalse(indianConvert.canBuildColony());

        Unit brave = new ServerUnit(game, tile1, sioux, braveType);
        assertTrue(brave.canBuildColony());
    }

    public void testIndianDies() {
        Game game = getStandardGame();
        Map map = getTestMap();
        game.setMap(map);

        Player indianPlayer = game.getPlayer("model.nation.sioux");
        FreeColTestCase.IndianSettlementBuilder builder
            = new FreeColTestCase.IndianSettlementBuilder(game)
            .player(indianPlayer);
        IndianSettlement camp = builder.build();
        assertEquals("Indian player owns the camp", indianPlayer,
                     camp.getOwner());

        Unit brave = new ServerUnit(game, camp, indianPlayer, braveType);
        camp.addOwnedUnit(brave);

        assertEquals("Brave was not added to camp", 2,
                     camp.getUnitCount());
        assertNotNull("Brave was not added to player unit list",
                      indianPlayer.getUnitById(brave.getId()));

        // unit dies
        brave.dispose();
        assertTrue("Brave was not disposed properly",
                   brave.isDisposed());
        assertEquals("Brave was not removed from camp", 1,
                     camp.getUnitCount());
        assertNull("Brave was not removed from player unit list",
                   indianPlayer.getUnitById(brave.getId()));
    }


    public void testUnitAvailability() {
        Game game = getStandardGame();
        Player indian = game.getPlayer("model.nation.sioux");
        Player european = game.getPlayer("model.nation.dutch");
        Player king = game.getPlayer("model.nation.dutchREF");

        assertTrue(kingsRegularType.isAvailableTo(king));
        assertFalse(kingsRegularType.isAvailableTo(indian));
        assertFalse(kingsRegularType.isAvailableTo(european));

        assertFalse(colonialRegularType.isAvailableTo(king));
        assertFalse(colonialRegularType.isAvailableTo(indian));
        assertFalse(colonialRegularType.isAvailableTo(european));

        assertFalse(braveType.isAvailableTo(king));
        assertTrue(braveType.isAvailableTo(indian));
        assertFalse(braveType.isAvailableTo(european));

        assertFalse(undeadType.isAvailableTo(king));
        assertFalse(undeadType.isAvailableTo(indian));
        assertFalse(undeadType.isAvailableTo(european));

        european.addAbility(new Ability(Ability.INDEPENDENCE_DECLARED));
        assertTrue(colonialRegularType.isAvailableTo(european));
    }

    public void testDefaultEquipment() {
        assertEquals("Colonist defaults to no equipment",
                     EquipmentType.NO_EQUIPMENT,
                     colonistType.getDefaultEquipment());

        assertEquals("Pioneer type defaults to having tools",
                     toolsEquipmentType,
                     hardyPioneerType.getDefaultEquipmentType());
        assertEquals("Pioneer type default equipment", 5,
                     hardyPioneerType.getDefaultEquipment().length);

        assertEquals("Soldier type defaults to having muskets",
                     musketsEquipmentType,
                     veteranSoldierType.getDefaultEquipmentType());
        assertEquals("Soldier type default equipment", 1,
                     veteranSoldierType.getDefaultEquipment().length);

        assertEquals("Missionary type defaults to missionary dress",
                     missionaryEquipmentType,
                     jesuitMissionaryType.getDefaultEquipmentType());
        assertEquals("Missionary type default equipment", 1,
                     jesuitMissionaryType.getDefaultEquipment().length);

        assertEquals("Scout type defaults to having horses",
                     horsesEquipmentType,
                     seasonedScoutType.getDefaultEquipmentType());
        assertEquals("Scout type default equipment", 1,
                     seasonedScoutType.getDefaultEquipment().length);
    }

    public void testChangeEquipment() {
        Game game = getGame();
        game.setMap(getTestMap(true));

        Colony colony = getStandardColony(6);
        assertFalse(churchType.hasAbility(Ability.DRESS_MISSIONARY));
        Building church = colony.getBuilding(churchType);
        church.upgrade();
        assertTrue(colony.hasAbility(Ability.DRESS_MISSIONARY));

        Unit colonist = colony.getUnitList().get(0);
        assertEquals("Colonist is not a missionary", 0,
                     colonist.getEquipmentCount(missionaryEquipmentType));
        assertTrue("Changing to missionary should not dump equipment",
            colonist.changeEquipment(missionaryEquipmentType, 1).isEmpty());
        assertEquals("Colonist should now be a missionary", 1,
                     colonist.getEquipmentCount(missionaryEquipmentType));
        assertEquals("Colonist should not have muskets", 0,
                     colonist.getEquipmentCount(musketsEquipmentType));
        List<EquipmentType> remove
            = colonist.changeEquipment(musketsEquipmentType, 1);
        assertTrue("Arming the colonist should remove missionary dress",
            remove.size() == 1 && remove.get(0) == missionaryEquipmentType);
        assertEquals("Colonist should be armed", 1,
                     colonist.getEquipmentCount(musketsEquipmentType));
        assertEquals("Colonist should have missionary dress", 1,
                     colonist.getEquipmentCount(missionaryEquipmentType));
    }

    public void testUnitLocationAfterBuildingColony() {
        Game game = getStandardGame();
        Player dutch = game.getPlayer("model.nation.dutch");
        Map map = getTestMap();
        game.setMap(map);

        Tile colonyTile = map.getTile(6, 8);

        Unit soldier = new ServerUnit(game, colonyTile, dutch,
                                      veteranSoldierType);
        assertEquals("Soldier location should be the colony tile", colonyTile,
                     soldier.getLocation());
        assertEquals("Soldier tile should be the colony tile", colonyTile,
                     soldier.getTile());

        boolean found = false;
        for (Unit u : colonyTile.getUnitList()) {
            if (u == soldier) found = true;
        }
        assertTrue("Unit not found in tile", found);

        Colony colony = new ServerColony(game, dutch, "New Amsterdam",
                                         colonyTile);
        dutch.addSettlement(colony);
        nonServerBuildColony(soldier, colony);

        assertTrue("Soldier should be inside the colony",
                   soldier.getLocation() instanceof ColonyTile);
        assertEquals("Soldier tile should be the colony tile", colonyTile,
                     soldier.getTile());
        for (Unit u : colonyTile.getUnitList()) {
            if (u == soldier) fail("Unit building colony still in tile");
        }

        found = false;
        for (WorkLocation loc : colony.getCurrentWorkLocations()) {
            found |= loc.getUnitList().contains(soldier);
        }
        assertTrue("Soldier should be in a work location in the colony",
                   found);
        ColonyTile workTile = soldier.getWorkTile();
        assertTrue("Soldier should be in a work tile in the colony",
                   workTile != null);
        assertFalse("Soldier should not be working in central tile",
                    workTile == colony.getColonyTile(colonyTile));
    }

    public void testUnitLosesExperienceWithRoleChange() {
        Game game = getStandardGame();
        Map map = getTestMap();
        game.setMap(map);

        Player dutch = game.getPlayer("model.nation.dutch");

        Unit colonist = new ServerUnit(game, map.getTile(6, 8), dutch,
                                       colonistType);
        colonist.modifyExperience(10);
        assertEquals(role_default, colonist.getRole());
        assertTrue("Colonist should some initial experience",
                   colonist.getExperience() > 0);

        colonist.changeEquipment(musketsEquipmentType, 1);
        //assertEquals(spec().getRole("model.role.soldier"), colonist.getRole());
        assertEquals(role_soldier, colonist.getRole());
        assertEquals("Colonist should have lost all experience and role", 0,
                     colonist.getExperience());

        colonist.modifyExperience(10);
        colonist.changeEquipment(horsesEquipmentType, 1);
        assertEquals(role_dragoon, colonist.getRole());
        assertTrue("Colonist should not have lost experience, compatible role",
                   colonist.getExperience() > 0);
    }

    public void testCompatibleRoles() {
        assertFalse(role_soldier.isCompatibleWith(role_default));
        assertFalse(role_soldier.isCompatibleWith(role_pioneer));
        assertFalse(role_soldier.isCompatibleWith(role_missionary));
        assertTrue(role_soldier.isCompatibleWith(role_soldier));
        assertFalse(role_soldier.isCompatibleWith(role_scout));
        assertTrue(role_soldier.isCompatibleWith(role_dragoon));

        assertFalse(role_missionary.isCompatibleWith(role_default));
        assertFalse(role_missionary.isCompatibleWith(role_pioneer));
        assertTrue(role_missionary.isCompatibleWith(role_missionary));
        assertFalse(role_missionary.isCompatibleWith(role_soldier));
        assertFalse(role_missionary.isCompatibleWith(role_scout));
        assertFalse(role_missionary.isCompatibleWith(role_dragoon));
    }

    public void testOwnerChange(){
        Game game = getStandardGame();
        Map map = getTestMap();
        game.setMap(map);

        Player dutch = game.getPlayer("model.nation.dutch");
        Player french = game.getPlayer("model.nation.french");

        Unit colonist = new ServerUnit(game, map.getTile(6, 8), dutch,
                                       colonistType);
        assertEquals("Colonist should be dutch", dutch,
                     colonist.getOwner());
        assertEquals("Dutch player should have 1 unit", 1,
                     dutch.getUnits().size());
        assertEquals("French player should have no units", 0,
                     french.getUnits().size());

        // change owner
        colonist.changeOwner(french);
        assertEquals("Colonist should be french", french,
                     colonist.getOwner());
        assertEquals("Dutch player should have no units", 0,
                     dutch.getUnits().size());
        assertEquals("French player should have 1 unit", 1,
                     french.getUnits().size());
    }

    public void testCarrierOwnerChange(){
        Game game = getStandardGame();
        Map map = getTestMap(ocean);
        game.setMap(map);

        Player dutch = game.getPlayer("model.nation.dutch");
        Player french = game.getPlayer("model.nation.french");

        Unit galleon = new ServerUnit(game, map.getTile(6, 8), dutch,
                                      galleonType);
        assertEquals("Galleon should be empty", 0,
                     galleon.getUnitSpaceTaken());
        assertTrue("Galleon should be able to carry units",
                   galleon.canCarryUnits());

        Unit colonist = new ServerUnit(game, galleon, dutch, colonistType);
        assertEquals("Colonist should be aboard the galleon", galleon,
                     colonist.getLocation());
        assertEquals("Galleon is carrying wrong number of units", 1,
                     galleon.getUnitSpaceTaken());

        assertEquals("Colonist should be dutch", dutch,
                     galleon.getOwner());
        assertEquals("Colonist should be dutch", dutch,
                     colonist.getOwner());
        assertEquals("Dutch player should have 2 units", 2,
                     dutch.getUnits().size());
        assertEquals("French player should have no units", 0,
                     french.getUnits().size());

        // change carrier owner
        galleon.changeOwner(french);
        assertEquals("Galleon should be french", french,
                     galleon.getOwner());
        assertEquals("Colonist should be french", french,
                     colonist.getOwner());
        assertEquals("Dutch player should have no units", 0,
                     dutch.getUnits().size());
        assertEquals("French player should have 2 units", 2,
                     french.getUnits().size());
    }


    public void testGetMovesAsString() {
        Game game = getStandardGame();
        Map map = getTestMap();
        game.setMap(map);

        Colony colony = getStandardColony(1);
        Unit unit = colony.getUnitList().get(0);
        String initial = "/" + Integer.toString(unit.getInitialMovesLeft()/3);

        String[] expected = new String[] {
            "0", "(1/3) ", "(2/3) ", "1", "1 (1/3) ", "1 (2/3) ",
            "2", "2 (1/3) ", "2 (2/3) ", "3", "3 (1/3) ", "3 (2/3) "
        };

        for (int index = 0; index < expected.length; index++) {
            unit.setMovesLeft(index);
            String expectedString = expected[index] + initial;
            String actualString = unit.getMovesAsString();
            assertEquals(expectedString + " != " + actualString,
                expectedString, actualString);
        }
    }

    public void testTreasureTransportFee() {
        Game game = getStandardGame();
        Player dutch = game.getPlayer("model.nation.dutch");

        Unit treasureTrain = new ServerUnit(game, null, dutch,
                                            treasureTrainType);
        treasureTrain.setTreasureAmount(6000);

        spec().applyDifficultyLevel("model.difficulty.veryEasy");
        assertEquals(50 * 60, treasureTrain.getTransportFee());
        spec().applyDifficultyLevel("model.difficulty.easy");
        assertEquals(55 * 60, treasureTrain.getTransportFee());
        spec().applyDifficultyLevel("model.difficulty.medium");
        assertEquals(60 * 60, treasureTrain.getTransportFee());
        spec().applyDifficultyLevel("model.difficulty.hard");
        assertEquals(65 * 60, treasureTrain.getTransportFee());
        spec().applyDifficultyLevel("model.difficulty.veryHard");
        assertEquals(70 * 60, treasureTrain.getTransportFee());

        dutch.addFather(spec()
            .getFoundingFather("model.foundingFather.hernanCortes"));

        spec().applyDifficultyLevel("model.difficulty.veryEasy");
        assertEquals(0, treasureTrain.getTransportFee());
        spec().applyDifficultyLevel("model.difficulty.easy");
        assertEquals(0, treasureTrain.getTransportFee());
        spec().applyDifficultyLevel("model.difficulty.medium");
        assertEquals(0, treasureTrain.getTransportFee());
        spec().applyDifficultyLevel("model.difficulty.hard");
        assertEquals(0, treasureTrain.getTransportFee());
        spec().applyDifficultyLevel("model.difficulty.veryHard");
        assertEquals(0, treasureTrain.getTransportFee());
    }


    public void testCopy() {
        Game game = getStandardGame();
        Player dutch = game.getPlayer("model.nation.dutch");
        Map map = getTestMap(plains, true);
        game.setMap(map);

        Tile tile1 = map.getTile(6, 8);
        Tile tile2 = map.getTile(6, 9);

        Unit merchantman = new ServerUnit(game, tile1, dutch, merchantmanType);
        Unit soldier = new ServerUnit(game, merchantman, dutch, veteranSoldierType);
        Goods goods = new Goods(game, merchantman, cottonType, 44);
        merchantman.add(goods);
        
        Unit other = merchantman.copy(game, merchantman.getClass());

        assertFalse(merchantman == other);
        assertEquals(merchantman.getId(), other.getId());
        assertEquals(merchantman.getType(), other.getType());
        assertEquals(1, merchantman.getUnitCount());
        assertEquals(1, other.getUnitCount());
        assertEquals(44, merchantman.getGoodsCount(cottonType));
        assertEquals(44, other.getGoodsCount(cottonType));
        assertEquals(1, merchantman.getUnitCount());
        assertEquals(1, other.getUnitCount());
        assertFalse(merchantman.getUnitList().get(0)
                    == other.getUnitList().get(0));
        assertEquals(merchantman.getUnitList().get(0).getId(),
            other.getUnitList().get(0).getId());
    }

    public void testElement() {
        Game game = getStandardGame();
        Player dutch = game.getPlayer("model.nation.dutch");
        Map map = getTestMap(plains, true);
        game.setMap(map);

        Tile tile1 = map.getTile(6, 8);
        Tile tile2 = map.getTile(6, 9);

        Unit merchantman = new ServerUnit(game, tile1, dutch, merchantmanType);
        Unit soldier = new ServerUnit(game, merchantman, dutch, veteranSoldierType);
        Goods goods = new Goods(game, merchantman, cottonType, 44);
        merchantman.add(goods);

        try {
            String xml = merchantman.serialize();
            Field nextId = Game.class.getDeclaredField("nextId");
            nextId.setAccessible(true);
            int id = nextId.getInt(game);
            nextId.setInt(game, id + 1);
            xml = xml.replace(merchantman.getId(), Unit.getXMLElementTagName() + ":" + id);

            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(new ByteArrayInputStream(xml.getBytes()));
            Element element = doc.getDocumentElement();
            Unit clone = new Unit(game, element);

            assertFalse(merchantman == clone);
            assertFalse(merchantman.getId().equals(clone.getId()));
            assertEquals(merchantman.getType(), clone.getType());
            assertEquals(1, merchantman.getUnitCount());
            assertEquals(1, clone.getUnitCount());
            assertEquals(44, merchantman.getGoodsCount(cottonType));
            assertEquals(44, clone.getGoodsCount(cottonType));
            assertEquals(merchantman.getUnitList().get(0), clone.getUnitList().get(0));

        } catch(Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

    }

}
