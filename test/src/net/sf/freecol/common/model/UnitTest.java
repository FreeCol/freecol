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

import java.util.List;

import net.sf.freecol.common.model.Unit.Role;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.server.model.ServerColony;
import net.sf.freecol.server.model.ServerUnit;
import net.sf.freecol.util.test.FreeColTestCase;


public class UnitTest extends FreeColTestCase {

    private static final BuildingType carpenterHouseType
        = spec().getBuildingType("model.building.carpenterHouse");

    private static final EquipmentType horsesType
        = spec().getEquipmentType("model.equipment.horses");
    private static final EquipmentType musketsType
        = spec().getEquipmentType("model.equipment.muskets");
    private static final EquipmentType toolsType
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
    private static final UnitType caravelType
        = spec().getUnitType("model.unit.caravel");
    private static final UnitType colonistType
        = spec().getUnitType("model.unit.freeColonist");
    private static final UnitType expertFarmerType
        = spec().getUnitType("model.unit.expertFarmer");
    private static final UnitType galleonType
        = spec().getUnitType("model.unit.galleon");
    private static final UnitType hardyPioneerType
        = spec().getUnitType("model.unit.hardyPioneer");
    private static final UnitType soldierType
        = spec().getUnitType("model.unit.veteranSoldier");
    private static final UnitType wagonType
        = spec().getUnitType("model.unit.wagonTrain");


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

        Unit merchantman = new ServerUnit(game, tile1, dutch, spec().getUnitType("model.unit.merchantman"),
                                    UnitState.ACTIVE);

        assertFalse("Merchantman isnt a colonist",merchantman.isColonist());

        Unit soldier = new ServerUnit(game, tile1, dutch, spec().getUnitType("model.unit.veteranSoldier"),
                                UnitState.ACTIVE);

        assertTrue("A soldier is a colonist",soldier.isColonist());

        UnitType braveType = spec().getUnitType("model.unit.brave");
        Unit brave = new ServerUnit(game, tile2, sioux, braveType,
                                    UnitState.ACTIVE);
        assertFalse("A brave is not a colonist", brave.isColonist());
    }

    public void testCanAdd() {

        Game game = getStandardGame();
        Player dutch = game.getPlayer("model.nation.dutch");

        Unit galleon = new ServerUnit(game, null, dutch, spec().getUnitType("model.unit.galleon"),
                                      UnitState.ACTIVE);
        Unit caravel = new ServerUnit(game, null, dutch, spec().getUnitType("model.unit.caravel"),
                                      UnitState.ACTIVE);
        Unit colonist = new ServerUnit(game, null, dutch, colonistType,
                                       UnitState.ACTIVE);
        Unit wagonTrain = new ServerUnit(game, null, dutch, spec().getUnitType("model.unit.wagonTrain"),
                                         UnitState.ACTIVE);
        Unit treasureTrain = new ServerUnit(game, null, dutch, spec().getUnitType("model.unit.treasureTrain"),
                                      UnitState.ACTIVE);

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
        int wagonTrainOldSpace = wagonTrain.getType().getSpace();
        int wagonTrainOldSpaceTaken = wagonTrain.getType().getSpace();
        int caravelOldSpaceTaken = caravel.getType().getSpace();

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

    public void testFailedAddGoods(){
        Game game = getStandardGame();
        Map map = getTestMap();
        game.setMap(map);

        Colony colony = this.getStandardColony();
        int foodInColony = 300;
        colony.addGoods(foodType, foodInColony);
        assertEquals("Setup error, colony does not have expected goods quantities",foodInColony,colony.getGoodsCount(foodType));

        Player dutch = game.getPlayer("model.nation.dutch");
        Unit wagonTrain = new ServerUnit(game, colony.getTile(), dutch, spec().getUnitType("model.unit.wagonTrain"),
                                   UnitState.ACTIVE);
        int initialMoves = wagonTrain.getInitialMovesLeft();
        assertEquals("Setup error, unit has wrong initial moves", initialMoves, wagonTrain.getMovesLeft());
        assertTrue("Setup error, unit should not carry anything", wagonTrain.getGoodsCount() == 0);

        Goods tooManyGoods = colony.goodsContainer.getGoods(foodType);
        try{
            wagonTrain.add(tooManyGoods);
            fail("Should have thrown an IllegalStateException");
        }
        catch(IllegalStateException e){
            assertTrue("Unit should not carry anything", wagonTrain.getGoodsCount() == 0);
            assertEquals("Unit moves should not have been modified", initialMoves, wagonTrain.getMovesLeft());
        }
    }

    public void testMissionary() {
        Game game = getStandardGame();
        Map map = getTestMap(plains, true);
        game.setMap(map);
        Player sioux = game.getPlayer("model.nation.sioux");
        Player dutch = game.getPlayer("model.nation.dutch");
        Tile tile = map.getTile(6, 9);
        UnitType missionaryType = spec().getUnitType("model.unit.jesuitMissionary");

        Colony colony = getStandardColony(3);
        BuildingType churchType = spec().getBuildingType("model.building.chapel");
        Building church = colony.getBuilding(churchType);
        church.upgrade();
        Unit jesuit = new ServerUnit(game, tile, dutch, missionaryType, UnitState.ACTIVE);
        Unit colonist = new ServerUnit(game, colony, dutch, colonistType, UnitState.ACTIVE);
        // check abilities
        assertFalse(colonist.hasAbility("model.ability.missionary"));
        colonist.changeEquipment(spec().getEquipmentType("model.equipment.missionary"), 1);
        assertTrue(colonist.hasAbility("model.ability.missionary"));
        assertFalse(colonist.hasAbility("model.ability.expertMissionary"));
        assertTrue(jesuit.hasAbility("model.ability.missionary"));
        assertTrue(jesuit.hasAbility("model.ability.expertMissionary"));
        // check mission creation
        FreeColTestCase.IndianSettlementBuilder builder = new FreeColTestCase.IndianSettlementBuilder(game);
        IndianSettlement s = builder.player(sioux).settlementTile(tile).capital(true).isVisitedByPlayer(dutch, true).build();

        // add the missionary
        s.setMissionary(jesuit);
        assertTrue("No missionary set",s.getMissionary() != null);
        assertEquals("Wrong missionary set", s.getMissionary(), jesuit);
        s.setMissionary(null);
        assertTrue("Missionary not removed",s.getMissionary() == null);
    }

    public void testLineOfSight() {
        Game game = getStandardGame();
        Map map = getTestMap(plains, true);
        game.setMap(map);
        Player player = game.getPlayer("model.nation.dutch");
        Tile tile = map.getTile(6, 9);

        UnitType frigateType = spec().getUnitType("model.unit.frigate");
        Unit frigate = new ServerUnit(game, tile, player, frigateType, UnitState.ACTIVE);
        assertEquals(2, frigate.getLineOfSight());
        assertTrue(frigate.hasAbility("model.ability.navalUnit"));

        UnitType revengerType = spec().getUnitType("model.unit.revenger");
        Unit revenger = new ServerUnit(game, tile, player, revengerType, UnitState.ACTIVE);
        assertEquals(3, revenger.getLineOfSight());

        Unit colonist = new ServerUnit(game, tile, player, colonistType, UnitState.ACTIVE);
        assertEquals(1, colonist.getLineOfSight());
        assertTrue(colonist.hasAbility("model.ability.canBeEquipped"));

        EquipmentType horses = spec().getEquipmentType("model.equipment.horses");
        assertTrue(colonist.canBeEquippedWith(horses));
        colonist.changeEquipment(horses, 1);
        assertEquals(2, colonist.getLineOfSight());

        // with Hernando De Soto, land units should see further
        FoundingFather father = spec().getFoundingFather("model.foundingFather.hernandoDeSoto");
        player.addFather(father);

        assertEquals(2, frigate.getLineOfSight());  // should not increase
        assertEquals(4, revenger.getLineOfSight()); // should get +1 bonus
        assertEquals(3, colonist.getLineOfSight()); // should get +1 bonus
    }

    public void testDisposingUnits() {
        Game game = getStandardGame();
        Map map = getTestMap(plains, true);
        game.setMap(map);
        Player player = game.getPlayer("model.nation.dutch");
        Tile tile = map.getTile(6, 9);

        UnitType frigateType = spec().getUnitType("model.unit.frigate");
        Unit frigate = new ServerUnit(game, tile, player, frigateType, UnitState.ACTIVE);
        Unit colonist = new ServerUnit(game, frigate, player, colonistType, UnitState.ACTIVE);

        tile.disposeAllUnits();
        assertTrue(frigate.isDisposed());
        assertTrue(colonist.isDisposed());
        assertEquals(0, frigate.getUnitCount());
        assertEquals(0, tile.getUnitCount());
    }

    public void testUnitCanBuildColony() {

        Game game = getStandardGame();

        Player dutch = game.getPlayer("model.nation.dutch");
        Player sioux = game.getPlayer("model.nation.sioux");

        Map map = getTestMap(plains, true);
        game.setMap(map);
        Tile tile1 = map.getTile(10, 4);

        UnitType farmerType = spec().getUnitType("model.unit.expertFarmer");
        Unit farmer = new ServerUnit(game, tile1, dutch, farmerType, UnitState.ACTIVE, farmerType.getDefaultEquipment());
        assertTrue(farmer.canBuildColony());

        UnitType artyType = spec().getUnitType("model.unit.artillery");
        Unit arty = new ServerUnit(game, tile1, dutch, artyType, UnitState.ACTIVE, artyType.getDefaultEquipment());
        assertFalse(arty.canBuildColony());

        UnitType shipType = spec().getUnitType("model.unit.galleon");
        Unit ship = new ServerUnit(game, tile1, dutch, shipType, UnitState.ACTIVE, shipType.getDefaultEquipment());
        assertFalse(ship.canBuildColony());

        UnitType treasureType = spec().getUnitType("model.unit.treasureTrain");
        Unit treasure = new ServerUnit(game, tile1, dutch, treasureType,
                                       UnitState.ACTIVE,
                                       treasureType.getDefaultEquipment());
        assertFalse(treasure.canBuildColony());

        UnitType wagonType = spec().getUnitType("model.unit.wagonTrain");
        Unit wagon = new ServerUnit(game, tile1, dutch, wagonType,
                                    UnitState.ACTIVE,
                                    wagonType.getDefaultEquipment());
        assertFalse(wagon.canBuildColony());

        UnitType indianConvertType = spec().getUnitType("model.unit.indianConvert");
        Unit indianConvert = new ServerUnit(game, tile1, dutch,
                                            indianConvertType,
                                            UnitState.ACTIVE,
                                            indianConvertType.getDefaultEquipment());
        assertFalse(indianConvert.canBuildColony());

        UnitType braveType = spec().getUnitType("model.unit.brave");
        Unit brave = new ServerUnit(game, tile1, sioux, braveType,
                                    UnitState.ACTIVE,
                                    braveType.getDefaultEquipment());
        assertTrue(brave.canBuildColony());
    }

    public void testIndianDies() {
        Game game = getStandardGame();
        Map map = getTestMap();
        game.setMap(map);

        Player indianPlayer = game.getPlayer("model.nation.sioux");

        FreeColTestCase.IndianSettlementBuilder builder = new FreeColTestCase.IndianSettlementBuilder(game);
        IndianSettlement camp = builder.build();

        UnitType indianBraveType = spec().getUnitType("model.unit.brave");
        Unit brave = new ServerUnit(game, camp, indianPlayer, indianBraveType,
                                    UnitState.ACTIVE,
                                    indianBraveType.getDefaultEquipment());
        camp.addOwnedUnit(brave);

        assertEquals("Brave wasnt added to camp",2, camp.getUnitCount());
        assertFalse("Brave wasnt added to player unit list",indianPlayer.getUnit(brave.getId()) == null);

        // unit dies
        brave.dispose();

        assertTrue("Brave wasnt disposed properly",brave.isDisposed());
        assertEquals("Brave wasnt removed from camp",1, camp.getUnitCount());
        assertTrue("Brave wasnt removed from player unit list",indianPlayer.getUnit(brave.getId()) == null);
    }


    public void testUnitAvailability() {
        Game game = getStandardGame();

        Player indian = game.getPlayer("model.nation.sioux");
        Player european = game.getPlayer("model.nation.dutch");
        Player king = game.getPlayer("model.nation.dutchREF");
        UnitType regular = spec().getUnitType("model.unit.kingsRegular");
        assertTrue(regular.isAvailableTo(king));
        assertFalse(regular.isAvailableTo(indian));
        assertFalse(regular.isAvailableTo(european));
        UnitType colonial = spec().getUnitType("model.unit.colonialRegular");
        assertFalse(colonial.isAvailableTo(king));
        assertFalse(colonial.isAvailableTo(indian));
        assertFalse(colonial.isAvailableTo(european));
        UnitType brave = spec().getUnitType("model.unit.brave");
        assertFalse(brave.isAvailableTo(king));
        assertTrue(brave.isAvailableTo(indian));
        assertFalse(brave.isAvailableTo(european));
        UnitType undead = spec().getUnitType("model.unit.undead");
        assertFalse(undead.isAvailableTo(king));
        assertFalse(undead.isAvailableTo(indian));
        assertFalse(undead.isAvailableTo(european));

        european.getFeatureContainer().addAbility(new Ability("model.ability.independenceDeclared"));
        assertTrue(colonial.isAvailableTo(european));

    }

    public void testDefaultEquipment() {

        assertEquals(EquipmentType.NO_EQUIPMENT, colonistType.getDefaultEquipment());

        assertEquals(spec().getEquipmentType("model.equipment.tools"), hardyPioneerType.getDefaultEquipmentType());
        assertEquals(5, hardyPioneerType.getDefaultEquipment().length);

        UnitType soldier = spec().getUnitType("model.unit.veteranSoldier");
        assertEquals(spec().getEquipmentType("model.equipment.muskets"), soldier.getDefaultEquipmentType());
        assertEquals(1, soldier.getDefaultEquipment().length);

        UnitType missionary = spec().getUnitType("model.unit.jesuitMissionary");
        assertEquals(spec().getEquipmentType("model.equipment.missionary"), missionary.getDefaultEquipmentType());
        assertEquals(1, missionary.getDefaultEquipment().length);

        UnitType scout = spec().getUnitType("model.unit.seasonedScout");
        assertEquals(spec().getEquipmentType("model.equipment.horses"), scout.getDefaultEquipmentType());
        assertEquals(1, scout.getDefaultEquipment().length);

    }

    public void testChangeEquipment() {
        Game game = getGame();
        game.setMap(getTestMap(true));

        Colony colony = getStandardColony(6);
        BuildingType churchType = spec().getBuildingType("model.building.chapel");
        assertFalse(churchType.hasAbility("model.ability.dressMissionary"));

        Building church = colony.getBuilding(churchType);
        church.upgrade();
        assertTrue(colony.hasAbility("model.ability.dressMissionary"));

        Unit colonist = colony.getRandomUnit();
        EquipmentType missionaryEquipmentType = spec().getEquipmentType("model.equipment.missionary");
        EquipmentType muskets = spec().getEquipmentType("model.equipment.muskets");
        assertEquals(0, colonist.getEquipmentCount(missionaryEquipmentType));
        assertTrue(colonist.changeEquipment(missionaryEquipmentType, 1).isEmpty());
        assertEquals(1, colonist.getEquipmentCount(missionaryEquipmentType));
        assertEquals(0, colonist.getEquipmentCount(muskets));
        List<EquipmentType> remove = colonist.changeEquipment(muskets, 1);
        assertTrue(remove.size() == 1
                   && remove.get(0) == missionaryEquipmentType);
        assertEquals(1, colonist.getEquipmentCount(muskets));
        assertEquals(1, colonist.getEquipmentCount(missionaryEquipmentType));
    }

    public void testUnitLocationAfterBuildingColony() {
        Game game = getStandardGame();
        Player dutch = game.getPlayer("model.nation.dutch");
        Map map = getTestMap();
        game.setMap(map);

        Tile colonyTile = map.getTile(6, 8);

        UnitType veteranType = spec().getUnitType("model.unit.veteranSoldier");
        Unit soldier = new ServerUnit(game, colonyTile, dutch, veteranType,
                                      UnitState.ACTIVE);

        assertTrue("soldier location should be the colony tile",soldier.getLocation() == colonyTile);
        assertTrue("soldier tile should be the colony tile",soldier.getTile() == colonyTile);
        //Boolean found = false;
        boolean found = false;
        for (Unit u : colonyTile.getUnitList()){
            if(u == soldier){
                found = true;
            }
        }
        assertTrue("Unit not found in tile",found);

        Colony colony = new ServerColony(game, dutch, "New Amsterdam", colonyTile);
        nonServerBuildColony(soldier, colony);

        assertFalse("soldier should be inside the colony",soldier.getLocation() == colonyTile);
        // There is some inconsistence with the results below
        // Unit.getTile() gives the location tile even though it isnt in the tile itself
        // This may lead to some confusion
        assertTrue("soldier should get the location tile as the colony tile",soldier.getTile() == colonyTile);
        for (Unit u : colonyTile.getUnitList()){
            if(u == soldier){
                fail("Unit building colony still in tile");
            }
        }

        found = false;
        for(WorkLocation loc : colony.getWorkLocations()){
            if(loc.getUnitList().contains(soldier)){
                found = true;
            }
        }
        assertTrue("Soldier should be in a work location in the colony",found);
        ColonyTile workTile = soldier.getWorkTile();
        assertTrue("Soldier should be in a work tile in the colony",workTile != null);
        assertFalse("Soldier should not be working in central tile",workTile == colony.getColonyTile(colonyTile));
    }

    public void testUnitLosesExperienceWithRoleChange() {
        Game game = getStandardGame();
        Map map = getTestMap();
        game.setMap(map);

        Player dutch = game.getPlayer("model.nation.dutch");
        Unit colonist = new ServerUnit(game, map.getTile(6, 8), dutch, colonistType,
                                       UnitState.ACTIVE);

        colonist.modifyExperience(10);
        assertEquals(Role.DEFAULT, colonist.getRole());
        assertTrue("Colonist should some initial experience",colonist.getExperience() > 0);

        colonist.changeEquipment(musketsType, 1);
        assertEquals(Role.SOLDIER, colonist.getRole());
        assertTrue("Colonist should have lost all experience, different role",colonist.getExperience() == 0);

        colonist.modifyExperience(10);
        colonist.changeEquipment(horsesType, 1);
        assertEquals(Role.DRAGOON, colonist.getRole());
        assertTrue("Colonist should not have lost experience, compatible role",colonist.getExperience() > 0);
    }

    public void testCompatibleRoles() {
        assertFalse(Role.SOLDIER.isCompatibleWith(Role.DEFAULT));
        assertFalse(Role.SOLDIER.isCompatibleWith(Role.PIONEER));
        assertFalse(Role.SOLDIER.isCompatibleWith(Role.MISSIONARY));
        assertTrue(Role.SOLDIER.isCompatibleWith(Role.SOLDIER));
        assertFalse(Role.SOLDIER.isCompatibleWith(Role.SCOUT));
        assertTrue(Role.SOLDIER.isCompatibleWith(Role.DRAGOON));

        assertFalse(Role.MISSIONARY.isCompatibleWith(Role.DEFAULT));
        assertFalse(Role.MISSIONARY.isCompatibleWith(Role.PIONEER));
        assertTrue(Role.MISSIONARY.isCompatibleWith(Role.MISSIONARY));
        assertFalse(Role.MISSIONARY.isCompatibleWith(Role.SOLDIER));
        assertFalse(Role.MISSIONARY.isCompatibleWith(Role.SCOUT));
        assertFalse(Role.MISSIONARY.isCompatibleWith(Role.DRAGOON));
    }

    public void testNewRole() {
        assertEquals(Role.PIONEER, Role.DEFAULT.newRole(Role.PIONEER));
        assertEquals(Role.MISSIONARY, Role.DEFAULT.newRole(Role.MISSIONARY));
        assertEquals(Role.SOLDIER, Role.DEFAULT.newRole(Role.SOLDIER));
        assertEquals(Role.SCOUT, Role.DEFAULT.newRole(Role.SCOUT));
        assertEquals(Role.DRAGOON, Role.DEFAULT.newRole(Role.DRAGOON));

        assertEquals(Role.PIONEER, Role.SOLDIER.newRole(Role.PIONEER));
        assertEquals(Role.MISSIONARY, Role.SOLDIER.newRole(Role.MISSIONARY));
        assertEquals(Role.DRAGOON, Role.SOLDIER.newRole(Role.SCOUT));
        assertEquals(Role.DRAGOON, Role.SCOUT.newRole(Role.SOLDIER));
    }


    public void testOwnerChange(){
        Game game = getStandardGame();
        Map map = getTestMap();
        game.setMap(map);

        Player dutch = game.getPlayer("model.nation.dutch");
        Player french = game.getPlayer("model.nation.french");

        Unit colonist = new ServerUnit(game, map.getTile(6, 8), dutch, colonistType,
                                       UnitState.ACTIVE);

        assertTrue("Colonist should be dutch",colonist.getOwner() == dutch);
        assertTrue("Dutch player should have 1 unit",dutch.getUnits().size() == 1);
        assertTrue("French player should have no units",french.getUnits().size() == 0);
        // change owner
        colonist.setOwner(french);
        assertTrue("Colonist should be french",colonist.getOwner() == french);
        assertTrue("Dutch player should have no units",dutch.getUnits().size() == 0);
        assertTrue("French player should have 1 unit",french.getUnits().size() == 1);
    }

    public void testCarrierOwnerChange(){
        Game game = getStandardGame();
        Map map = getTestMap(ocean);
        game.setMap(map);

        Player dutch = game.getPlayer("model.nation.dutch");
        Player french = game.getPlayer("model.nation.french");

        Unit galleon = new ServerUnit(game, map.getTile(6, 8), dutch, galleonType,
                                      UnitState.ACTIVE);
        assertTrue("Galleon should be empty",galleon.getUnitCount() == 0);
        assertTrue("Galleon should be able to carry units",galleon.canCarryUnits());
        Unit colonist = new ServerUnit(game, galleon, dutch, colonistType,
                                       UnitState.SENTRY);
        assertTrue("Colonist should be aboard the galleon",colonist.getLocation() == galleon);
        assertEquals("Wrong number of units th galleon is carrying",1,galleon.getUnitCount());

        assertTrue("Colonist should be dutch",galleon.getOwner() == dutch);
        assertTrue("Colonist should be dutch",colonist.getOwner() == dutch);
        assertTrue("Dutch player should have 2 units",dutch.getUnits().size() == 2);
        assertTrue("French player should have no units",french.getUnits().size() == 0);

        // change carrier owner
        galleon.setOwner(french);
        assertTrue("Galleon should be french",galleon.getOwner() == french);
        assertTrue("Colonist should be french",colonist.getOwner() == french);
        assertTrue("Dutch player should have no units",dutch.getUnits().size() == 0);
        assertTrue("French player should have 2 units",french.getUnits().size() == 2);
    }


    public void testGetMovesAsString() {

        Game game = getStandardGame();
        Map map = getTestMap();
        game.setMap(map);

        Colony colony = getStandardColony(1);
        Unit unit = colony.getUnitList().get(0);
        String initial = "/" + Integer.toString(unit.getInitialMovesLeft() / 3);

        String[] expected = new String[] {
            "0", "(1/3) ", "(2/3) ", "1", "1 (1/3) ", "1 (2/3) ",
            "2", "2 (1/3) ", "2 (2/3) ", "3", "3 (1/3) ", "3 (2/3) "
        };

        for (int index = 0; index < expected.length; index++) {
            unit.setMovesLeft(index);
            String expectedString = expected[index] + initial;
            String actualString = unit.getMovesAsString();
            assertEquals(expectedString + " != " + actualString, expectedString, actualString );
        }
    }

    public void testTreasureTransportFee() {

        Game game = getStandardGame();
        Player dutch = game.getPlayer("model.nation.dutch");

        Unit treasureTrain = new ServerUnit(game, null, dutch, spec().getUnitType("model.unit.treasureTrain"),
                                            UnitState.ACTIVE);
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

        dutch.addFather(spec().getFoundingFather("model.foundingFather.hernanCortes"));

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

}
