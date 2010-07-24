/**
 *  Copyright (C) 2002-2008  The FreeCol Team
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

package net.sf.freecol.server.control;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.BuildingType;
import net.sf.freecol.common.model.CombatModel;
import net.sf.freecol.common.model.CombatModel.CombatResult;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.EquipmentType;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Player.PlayerType;
import net.sf.freecol.common.model.Player.Stance;
import net.sf.freecol.common.model.SimpleCombatModel;
import net.sf.freecol.common.model.Tension;
import net.sf.freecol.common.model.Tension.Level;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.UnitTypeChange.ChangeType;
import net.sf.freecol.common.util.Utils;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.ServerTestHelper;
import net.sf.freecol.server.model.ServerPlayer;
import net.sf.freecol.util.test.FreeColTestCase;
import net.sf.freecol.util.test.MockMapGenerator;
import net.sf.freecol.util.test.MockPseudoRandom;


public class InGameControllerTest extends FreeColTestCase {
    BuildingType schoolHouseType = spec().getBuildingType("model.building.Schoolhouse");
    TileType plains = spec().getTileType("model.tile.plains");
    UnitType braveType = spec().getUnitType("model.unit.brave");
    UnitType colonistType = spec().getUnitType("model.unit.freeColonist");
    UnitType colonialType = spec().getUnitType("model.unit.colonialRegular");
    UnitType veteranType = spec().getUnitType("model.unit.veteranSoldier");
    UnitType pettyCriminalType = spec().getUnitType("model.unit.pettyCriminal");
    UnitType indenturedServantType = spec().getUnitType("model.unit.indenturedServant");
    UnitType kingsRegularType = spec().getUnitType("model.unit.kingsRegular");
    UnitType indianConvertType = spec().getUnitType("model.unit.indianConvert");
    UnitType hardyPioneerType = spec().getUnitType("model.unit.hardyPioneer");
    UnitType wagonTrainType = spec().getUnitType("model.unit.wagonTrain");
    UnitType caravelType = spec().getUnitType("model.unit.caravel");
    UnitType galleonType = spec().getUnitType("model.unit.galleon");
    UnitType privateerType = spec().getUnitType("model.unit.privateer");
    UnitType missionaryType = spec().getUnitType("model.unit.jesuitMissionary");
    UnitType artilleryType = spec().getUnitType("model.unit.artillery");
    UnitType damagedArtilleryType = spec().getUnitType("model.unit.damagedArtillery");
    UnitType treasureTrainType = spec().getUnitType("model.unit.treasureTrain");
    TileType ocean = spec().getTileType("model.tile.ocean");
    GoodsType cottonType = spec().getGoodsType("model.goods.cotton");
    GoodsType musketType = spec().getGoodsType("model.goods.muskets");
    GoodsType horsesType = spec().getGoodsType("model.goods.horses");
    EquipmentType tools = spec().getEquipmentType("model.equipment.tools");
    EquipmentType horses = spec().getEquipmentType("model.equipment.horses");
    EquipmentType muskets = spec().getEquipmentType("model.equipment.muskets");
    EquipmentType indianMuskets = spec().getEquipmentType("model.equipment.indian.muskets");
    EquipmentType indianHorses = spec().getEquipmentType("model.equipment.indian.horses");

    FreeColServer server = null;
    SimpleCombatModel combatModel = new SimpleCombatModel();


    public void tearDown() throws Exception {
        if(server != null) {
            // must make sure that the server is stopped
            ServerTestHelper.stopServer(server);
            server = null;
        }
        super.tearDown();
    }

    private Game start(Map map) {
        if (server == null) {
            server = ServerTestHelper.startServer(false, true);
        }
        server.setMapGenerator(new MockMapGenerator(map));
        PreGameController pgc = (PreGameController) server.getController();
        try {
            pgc.startGame();
        } catch (FreeColException e) {
            fail("Failed to start game");
        }
        Game game = server.getGame();
        FreeColTestCase.setGame(game);
        return game;
    }

    /**
     * Repeatedly ask the CombatModel for an attack result until it
     * gives the primary one we want (WIN, LOSE, NO_RESULT).
     */
    private List<CombatResult> fakeAttackResult(CombatResult result,
                                                FreeColGameObject attacker,
                                                FreeColGameObject defender)
    {
        List<CombatResult> crs;
        final float delta = 0.02f;
        CombatModel.CombatOdds combatOdds
            = combatModel.calculateCombatOdds(attacker, defender);
        float f = combatOdds.win;
        MockPseudoRandom mr = new MockPseudoRandom();
        List<Integer> number = new ArrayList<Integer>();
        number.add(-1);
        do {
            f += (result == CombatResult.WIN) ? -delta : delta;
            if (f < 0.0f || f >= 1.0f) {
                throw new IllegalStateException("f out of range: "
                                                + Float.toString(f));
            }
            number.set(0, new Integer((int)(Integer.MAX_VALUE * f)));
            mr.setNextNumbers(number, true);
            crs = combatModel.generateAttackResult(mr, attacker, defender);
        } while (crs.get(0) != result);
        return crs;
    }


    public void testDeclarationOfWarFromPeace() {
        Game game = start(getTestMap());
        InGameController igc = (InGameController) server.getController();
        Player dutch = game.getPlayer("model.nation.dutch");
        Player french = game.getPlayer("model.nation.french");
        int initialTensionValue = 500;

        // Setup
        dutch.setStance(french, Stance.PEACE);
        french.setStance(dutch, Stance.PEACE);
        dutch.setTension(french, new Tension(initialTensionValue));
        french.setTension(dutch, new Tension(initialTensionValue));

        // Verify initial conditions
        int initialDutchTension = dutch.getTension(french).getValue();
        int initialFrenchTension = french.getTension(dutch).getValue();
        assertEquals("The Dutch must be at peace with the French",
                     Stance.PEACE, dutch.getStance(french));
        assertEquals("The French must be at peace with the Dutch",
                     Stance.PEACE, french.getStance(dutch));
        assertEquals("Wrong initial dutch tension",
                     initialTensionValue, initialDutchTension);
        assertEquals("Wrong initial french tension",
                     initialTensionValue, initialFrenchTension);

        // French declare war
        igc.sendChangeStance(french, Stance.WAR, dutch, true);

        // Verify stance
        assertTrue("The Dutch should be at war with the French",
                   dutch.getStance(french) == Stance.WAR);
        assertTrue("The French should be at war with the Dutch",
                   french.getStance(dutch) == Stance.WAR);

        // Verify tension
        int currDutchTension = dutch.getTension(french).getValue();
        int currFrenchTension = french.getTension(dutch).getValue();
        int expectedDutchTension = Math.min(Tension.TENSION_MAX,
            initialDutchTension + Tension.WAR_MODIFIER);
        int expectedFrenchTension = Math.min(Tension.TENSION_MAX,
            initialFrenchTension + Tension.WAR_MODIFIER);
        assertEquals("Wrong dutch tension",
                     expectedDutchTension, currDutchTension);
        assertEquals("Wrong french tension",
                     expectedFrenchTension, currFrenchTension);
    }

    public void testCreateMission() {
        Game game = start(getTestMap());
        Map map = game.getMap();
        InGameController igc = (InGameController) server.getController();
        Player dutch = game.getPlayer("model.nation.dutch");
        Player french = game.getPlayer("model.nation.french");

        FreeColTestCase.IndianSettlementBuilder builder
            = new FreeColTestCase.IndianSettlementBuilder(game);
        IndianSettlement camp = builder.build();
        Tile tile = camp.getTile().getNeighbourOrNull(Map.Direction.N);
        Unit dutchJesuit = new Unit(game, tile, dutch, missionaryType,
                                    UnitState.ACTIVE);
        Unit frenchJesuit = new Unit(game, tile, french, missionaryType,
                                     UnitState.ACTIVE);

        // Set Dutch tension to HATEFUL
        Tension tension = new Tension(Level.HATEFUL.getLimit());
        camp.setAlarm(dutch, tension);
        assertEquals("Wrong camp alarm", tension, camp.getAlarm(dutch));

        // Mission establishment should fail for the Dutch
        igc.establishMission((ServerPlayer) dutch, dutchJesuit, camp);
        assertTrue("Dutch Jesuit should be dead",
                   dutchJesuit.isDisposed());
        assertTrue("Indian settlement should not have a mission",
                   camp.getMissionary() == null);

        // But succeed for the French
        igc.establishMission((ServerPlayer) french, frenchJesuit, camp);
        assertTrue("French Jesuit should not be dead",
                   !frenchJesuit.isDisposed());
        assertTrue("Indian settlement should have a mission",
                   camp.getMissionary() != null);
    }

    public void testDumpGoods() {
        Game game = start(getTestMap(ocean));
        Map map = game.getMap();
        InGameController igc = (InGameController) server.getController();
        Player dutch = game.getPlayer("model.nation.dutch");

        Tile tile = map.getTile(1, 1);
        Unit privateer = new Unit(game, tile, dutch, privateerType,
                                  UnitState.ACTIVE);
        assertEquals("Privateer should not carry anything",
                     0, privateer.getGoodsCount());
        Goods cotton = new Goods(game,privateer,cottonType,100);
        privateer.add(cotton);
        assertEquals("Privateer should carry cotton",
                     1, privateer.getGoodsCount());
        assertTrue("Cotton should be aboard privateer",
                   cotton.getLocation() == privateer);

        // Moving a good to a null location means dumping the good
        igc.moveGoods(cotton, null);

        assertEquals("Privateer should no longer carry cotton",
                     0, privateer.getGoodsCount());
        assertNull("Cotton should have no location",
                   cotton.getLocation());
    }

    public void testCashInTreasure() {
        Game game = start(getCoastTestMap(plains, true));
        Map map = game.getMap();
        InGameController igc = (InGameController) server.getController();
        Player dutch = game.getPlayer("model.nation.dutch");

        Tile tile = map.getTile(10, 4);
        Unit ship = new Unit(game, tile, dutch, galleonType,
                             UnitState.ACTIVE,
                             galleonType.getDefaultEquipment());
        Unit treasure = new Unit(game, tile, dutch, treasureTrainType,
                                 UnitState.ACTIVE,
                                 treasureTrainType.getDefaultEquipment());
        assertTrue("Treasure train can carry treasure",
                   treasure.canCarryTreasure());
        treasure.setTreasureAmount(100);
        assertFalse("Can not cash in treasure from a tile",
                    treasure.canCashInTreasureTrain());
        treasure.setLocation(ship);
        assertFalse("Can not cash in treasure from a ship",
                    treasure.canCashInTreasureTrain());

        // Succeed in Europe
        ship.setLocation(dutch.getEurope());
        assertTrue("Can cash in treasure in Europe",
                   treasure.canCashInTreasureTrain());
        int fee = treasure.getTransportFee();
        assertEquals("Cash in transport fee is zero in Europe",
                     0, fee);
        int oldGold = dutch.getGold();
        igc.cashInTreasureTrain((ServerPlayer) dutch, treasure);
        assertEquals("Cash in increases gold by the treasure amount",
                     100, dutch.getGold() - oldGold);

        // Succeed from a port
        Colony port = getStandardColony(1, 9, 4);
        assertFalse("Standard colony is not landlocked",
                    port.isLandLocked());
        assertTrue("Standard colony is connected to Europe",
                   port.isConnected());
        treasure.setLocation(port);
        assertTrue("Can cash in treasure from a port",
                   treasure.canCashInTreasureTrain());

        // Fail from a landlocked colony
        Colony inland = getStandardColony(1, 7, 7);
        assertTrue("Inland colony is landlocked",
                   inland.isLandLocked());
        assertFalse("Inland colony is not connected to Europe",
                    inland.isConnected());
        treasure.setLocation(inland);
        assertFalse("Can not cash in treasure from inland colony",
                    treasure.canCashInTreasureTrain());

        // Fail from a colony with a port but no connection to Europe
        map.getTile(5, 5).setType(spec().getTileType("model.tile.lake"));
        Colony lake = getStandardColony(1, 4, 5);
        assertFalse("Lake colony is not landlocked",
                    lake.isLandLocked());
        assertFalse("Lake colony is not connected to Europe",
                    lake.isConnected());
        treasure.setLocation(lake);
        assertFalse("Can not cash in treasure from lake colony",
                    treasure.canCashInTreasureTrain());
    }

    public void testEmbark() {
        Game game = start(getCoastTestMap(plains));
        Map map = game.getMap();
        InGameController igc = (InGameController) server.getController();
        Player dutch = game.getPlayer("model.nation.dutch");

        Tile landTile = map.getTile(9, 9);
        Tile seaTile = map.getTile(10, 9);
        Unit colonist = new Unit(game, landTile, dutch, colonistType,
                                 UnitState.ACTIVE);
        Unit galleon = new Unit(game, seaTile, dutch, galleonType,
                                UnitState.ACTIVE);
        Unit caravel = new Unit(game, seaTile, dutch, caravelType,
                                UnitState.ACTIVE);
        caravel.getType().setSpaceTaken(2);
        Unit wagon = new Unit(game, landTile, dutch, wagonTrainType,
                              UnitState.ACTIVE);

        // Can not put ship on carrier
        igc.embarkUnit((ServerPlayer) dutch, caravel, galleon);
        assertTrue("Caravel can not be put on galleon",
                   caravel.getLocation() == seaTile);

        // Can not put wagon on galleon at its normal size
        wagon.getType().setSpaceTaken(12);
        igc.embarkUnit((ServerPlayer) dutch, wagon, galleon);
        assertTrue("Large wagon can not be put on galleon",
                   wagon.getLocation() == landTile);

        // but we can if it is made smaller
        wagon.getType().setSpaceTaken(2);
        igc.embarkUnit((ServerPlayer) dutch, wagon, galleon);
        assertTrue("Wagon should now fit on galleon",
                   wagon.getLocation() == galleon);
        assertEquals("Embarked wagon should be in SENTRY state",
                     UnitState.SENTRY, wagon.getState());

        // Can put colonist on carrier
        igc.embarkUnit((ServerPlayer) dutch, colonist, caravel);
        assertTrue("Colonist should embark on caravel",
                   colonist.getLocation() == caravel);
        assertEquals("Embarked colonist should be in SENTRY state",
                     UnitState.SENTRY, colonist.getState());
    }

    public void testClearSpecialty() {
        Game game = start(getTestMap());
        Map map = game.getMap();
        InGameController igc = (InGameController) server.getController();

        Player dutch = game.getPlayer("model.nation.dutch");
        Unit unit = new Unit(game, map.getTile(5, 8), dutch, hardyPioneerType,
                             UnitState.ACTIVE);
        assertTrue("Unit should be a hardy pioneer",
                   unit.getType() == hardyPioneerType);

        // Basic function
        igc.clearSpeciality((ServerPlayer) dutch, unit);
        assertTrue("Unit should be cleared of its specialty",
                    unit.getType() != hardyPioneerType);

        // Can not clear speciality while teaching
        Colony colony = getStandardColony();
        Building school = new Building(game, colony, schoolHouseType);
        colony.addBuilding(school);
        Unit teacher = new Unit(game, school, colony.getOwner(),
                                hardyPioneerType, UnitState.ACTIVE);
        assertTrue("Unit should be a hardy pioneer",
                   teacher.getType() == hardyPioneerType);
        igc.clearSpeciality((ServerPlayer) dutch, teacher);
        assertTrue("Teacher specialty cannot be cleared",
                   teacher.getType() == hardyPioneerType);
    }

    public void testAtackedNavalUnitIsDamaged() {
        Game game = start(getTestMap(ocean));
        Map map = game.getMap();
        InGameController igc = (InGameController) server.getController();
        List<CombatResult> crs;
        Player dutch = game.getPlayer("model.nation.dutch");
        Player french = game.getPlayer("model.nation.french");

        dutch.setStance(french, Stance.WAR);
        french.setStance(dutch, Stance.WAR);
        assertEquals("Dutch should be at war with french",
                     dutch.getStance(french), Stance.WAR);
        assertEquals("French should be at war with dutch",
                     french.getStance(dutch), Stance.WAR);

        Tile tile1 = map.getTile(5, 8);
        tile1.setExploredBy(dutch, true);
        tile1.setExploredBy(french, true);
        Tile tile2 = map.getTile(4, 8);
        tile2.setExploredBy(dutch, true);
        tile2.setExploredBy(french, true);
        Tile tile3 = map.getTile(6, 8);
        tile3.setExploredBy(dutch, true);
        tile3.setExploredBy(french, true);
        Unit galleon = new Unit(game, tile1, dutch, galleonType,
                                UnitState.ACTIVE);
        Unit privateer = new Unit(game, tile2, french, privateerType,
                                  UnitState.ACTIVE);
        assertEquals("Galleon should be empty",
                     galleon.getGoodsCount(), 0);
        Goods cargo = new Goods(game,galleon,musketType,100);
        galleon.add(cargo);
        assertEquals("Galleon should be loaded",
                     galleon.getGoodsCount(), 1);
        assertFalse("Galleon should not be repairing",
                    galleon.isUnderRepair());
        galleon.setDestination(tile3);
        assertEquals("Wrong destination for Galleon",
                     tile3, galleon.getDestination());
        galleon.getTile().setConnected(true);
        assertEquals("Galleon repair location is Europe",
                     dutch.getRepairLocation(galleon), dutch.getEurope());

        // Privateer should win, loot and damage the galleon
        crs = fakeAttackResult(CombatResult.WIN, privateer, galleon);
        assertTrue("Privateer v galleon failed", crs.size() == 3
                   && crs.get(0) == CombatResult.WIN
                   && crs.get(1) == CombatResult.LOOT_SHIP
                   && crs.get(2) == CombatResult.DAMAGE_SHIP_ATTACK);
        igc.combat((ServerPlayer) dutch, privateer, galleon, crs);

        assertTrue("Galleon should be in Europe repairing",
                   galleon.isUnderRepair());
        assertEquals("Galleon should be empty",
                     galleon.getGoodsCount(), 0);
        assertTrue("Galleon should no longer have a destination",
                   galleon.getDestination() == null);
    }

    public void testUnarmedAttack() {
        Game game = start(getTestMap(plains));
        Map map = game.getMap();
        InGameController igc = (InGameController) server.getController();
        List<CombatResult> crs;
        Player dutch = game.getPlayer("model.nation.dutch");
        Player french = game.getPlayer("model.nation.french");

        dutch.getFeatureContainer()
            .addAbility(new Ability("model.ability.independenceDeclared"));
        Tile tile1 = map.getTile(5, 8);
        tile1.setExploredBy(dutch, true);
        tile1.setExploredBy(french, true);
        Tile tile2 = map.getTile(4, 8);
        tile2.setExploredBy(dutch, true);
        tile2.setExploredBy(french, true);
        // Create Colonial Regular with default equipment
        Unit colonial = new Unit(game, tile1, dutch, colonialType,
                                 UnitState.ACTIVE, EquipmentType.NO_EQUIPMENT);
        assertEquals("Must be Colonial Regular",
                     colonialType, colonial.getType());
        assertEquals("Only has default offence",
                     UnitType.DEFAULT_OFFENCE, colonial.getType().getOffence());
        assertEquals("Only has default defence",
                     UnitType.DEFAULT_DEFENCE, colonial.getType().getDefence());
        assertFalse("Not an offensive unit",
                    colonial.isOffensiveUnit());
        // Create Veteran Soldier with default equipment
        Unit soldier = new Unit(game, tile2, french, veteranType,
                                UnitState.ACTIVE);
        assertTrue("Veteran is armed",
                   soldier.isArmed());
        assertTrue("Veteran is an offensive unit",
                   soldier.isOffensiveUnit());
        assertEquals("Unarmed Colonial Regular can not attack!",
                     Unit.MoveType.MOVE_NO_ATTACK_CIVILIAN,
                     colonial.getMoveType(tile2));

        // Veteran attacks and captures the Colonial Regular
        crs = fakeAttackResult(CombatResult.WIN, soldier, colonial);
        assertTrue("Soldier v Colonial failed", crs.size() == 2
                   && crs.get(0) == CombatResult.WIN
                   && crs.get(1) == CombatResult.CAPTURE_UNIT);
        igc.combat((ServerPlayer) french, soldier, colonial, crs);

        assertEquals("Colonial Regular should be captured",
                     french, colonial.getOwner());
        assertEquals("Colonial Regular is moved to the Veterans tile",
                     tile2, colonial.getTile());
        assertEquals("Colonial Regular is demoted",
                     colonistType, colonial.getType());
    }

    public void testAttackColonyWithVeteran() {
        Game game = start(getTestMap(true));
        Map map = game.getMap();
        InGameController igc = (InGameController) server.getController();
        List<CombatResult> crs;
        Player dutch = game.getPlayer("model.nation.dutch");
        Player french = game.getPlayer("model.nation.french");
        Colony colony = getStandardColony();

        Tile tile2 = map.getTile(4, 8);
        tile2.setExploredBy(dutch, true);
        tile2.setExploredBy(french, true);
        dutch.getFeatureContainer()
            .addAbility(new Ability("model.ability.independenceDeclared"));
        Unit colonist = colony.getUnitIterator().next();
        colonist.setType(colonialType);
        assertEquals("Colonist should be Colonial Regular",
                     colonialType, colonist.getType());
        Unit defender = new Unit(getGame(), colony.getTile(), dutch,
                                 veteranType, UnitState.ACTIVE,
                                 horses, muskets);
        Unit attacker = new Unit(getGame(), tile2, french, veteranType,
                                 UnitState.ACTIVE, horses, muskets);
        assertEquals("Colony defender is Veteran Soldier",
                     defender, colony.getTile().getDefendingUnit(attacker));

        // Attacker wins and defender loses horses
        crs = fakeAttackResult(CombatResult.WIN, attacker, defender);
        assertTrue("Veteran v Colony (1) failed", crs.size() == 2
                   && crs.get(0) == CombatResult.WIN
                   && crs.get(1) == CombatResult.LOSE_EQUIP);
        igc.combat((ServerPlayer) french, attacker, defender, crs);

        assertTrue("Attacker should be mounted",
                   attacker.isMounted());
        assertTrue("Attacker should be armed",
                   attacker.isArmed());
        assertEquals("Attacker should be a Veteran Soldier",
                     veteranType, attacker.getType());
        assertFalse("Defender should not be mounted",
                    defender.isMounted());
        assertTrue("Defender should be armed",
                   defender.isArmed());
        assertEquals("Defender should be a Veteran Soldier",
                     veteranType, defender.getType());
        assertEquals("Defender is still the best colony defender",
                     defender, colony.getTile().getDefendingUnit(attacker));

        // Attacker loses and loses horses
        crs = fakeAttackResult(CombatResult.LOSE, attacker, defender);
        assertTrue("Veteran v Colony (2) failed ", crs.size() == 2
                   && crs.get(0) == CombatResult.LOSE
                   && crs.get(1) == CombatResult.LOSE_EQUIP);
        igc.combat((ServerPlayer) french, attacker, defender, crs);

        assertFalse("Attacker should not be mounted",
                    attacker.isMounted());
        assertTrue("Attacker should be armed",
                   attacker.isArmed());
        assertEquals("Attacker should be a Veteran Soldier",
                     veteranType, attacker.getType());
        assertFalse("Defender should not be mounted",
                    defender.isMounted());
        assertTrue("Defender should be armed",
                   defender.isArmed());
        assertEquals("Defender should be a Veteran Soldier",
                     veteranType, defender.getType());
        assertEquals("Defender is still the best colony defender",
                     defender, colony.getTile().getDefendingUnit(attacker));

        // Attacker wins and defender loses muskets
        crs = fakeAttackResult(CombatResult.WIN, attacker, defender);
        assertTrue("Veteran v Colony (3) failed", crs.size() == 2
                   && crs.get(0) == CombatResult.WIN
                   && crs.get(1) == CombatResult.LOSE_EQUIP);
        igc.combat((ServerPlayer) french, attacker, defender, crs);

        assertFalse("Attacker should not be mounted",
                    attacker.isMounted());
        assertTrue("Attacker should be armed",
                   attacker.isArmed());
        assertEquals("Attacker should be a Veteran Soldier",
                     veteranType, attacker.getType());
        assertFalse("Defender should not be mounted",
                    defender.isMounted());
        assertFalse("Defender should not be armed",
                    defender.isArmed());
        assertEquals("Defender should be a Veteran Soldier",
                     veteranType, defender.getType());
        assertFalse("Defender should not be a defensive unit",
                    defender.isDefensiveUnit());

        // Attacker wins and captures the settlement
        crs = fakeAttackResult(CombatResult.WIN, attacker, defender);
        assertTrue("Veteran v Colony (4) failed", crs.size() == 2
                   && crs.get(0) == CombatResult.WIN
                   && crs.get(1) == CombatResult.CAPTURE_COLONY);
        igc.combat((ServerPlayer) french, attacker, defender, crs);

        assertFalse("Attacker should not be mounted",
                    attacker.isMounted());
        assertTrue("Attacker should be armed",
                   attacker.isArmed());
        assertEquals("Attacker should be a Veteran Soldier",
                     veteranType, attacker.getType());
        assertFalse("Defender should not be mounted",
                    defender.isMounted());
        assertFalse("Defender should not be armed",
                    defender.isArmed());
        assertEquals("Defender should be demoted",
                     colonistType, defender.getType());
        assertEquals("Attacker should be on the colony tile",
                     colony.getTile(), attacker.getTile());
        assertEquals("Defender should be on the colony tile",
                     colony.getTile(), defender.getTile());
        assertEquals("Colony should be owned by the attacker",
                     attacker.getOwner(), colony.getOwner());
        assertEquals("Colony colonist should be demoted",
                     colonist.getType(), colonistType);
    }

    public void testAttackColonyWithBrave() {
        Game game = start(getTestMap(true));
        Map map = game.getMap();
        InGameController igc = (InGameController) server.getController();
        List<CombatResult> crs;
        Player dutch = game.getPlayer("model.nation.dutch");
        Player inca = game.getPlayer("model.nation.inca");
        Colony colony = getStandardColony(1, 5, 8);

        Tile tile2 = map.getTile(4, 8);
        tile2.setExploredBy(dutch, true);
        tile2.setExploredBy(inca, true);
        Unit colonist = colony.getUnitIterator().next();
        Unit defender = new Unit(getGame(), colony.getTile(), dutch,
                                 veteranType, UnitState.ACTIVE,
                                 horses, muskets);
        Unit attacker = new Unit(getGame(), tile2, inca, braveType,
                                 UnitState.ACTIVE, indianHorses, indianMuskets);
        assertTrue("Attacker should be armed",
                   attacker.isArmed());
        assertTrue("Attacker should be mounted",
                   attacker.isMounted());
        assertTrue("Inca is indian",
                   inca.isIndian());
        assertEquals("Defender is the colony best defender",
                     defender, colony.getTile().getDefendingUnit(attacker));

        // Attacker wins and defender loses horses
        crs = fakeAttackResult(CombatResult.WIN, attacker, defender);
        assertTrue("Brave v Colony (1) failed", crs.size() == 2
                   && crs.get(0) == CombatResult.WIN
                   && crs.get(1) == CombatResult.LOSE_EQUIP);
        igc.combat((ServerPlayer) inca, attacker, defender, crs);

        assertEquals("Colony size should be 1",
                     1, colony.getUnitCount());
        assertTrue("Attacker should be mounted",
                   attacker.isMounted());
        assertTrue("Attacker should be armed",
                   attacker.isArmed());
        assertEquals("Attacker should be a brave",
                     braveType, attacker.getType());
        assertFalse("Defender should not be mounted",
                    defender.isMounted());
        assertTrue("Defender should be armed",
                   defender.isArmed());
        assertEquals("Defender should be Veteran Soldier",
                     veteranType, defender.getType());
        assertTrue("Defender should be a defensive unit",
                   defender.isDefensiveUnit());
        assertEquals("Defender is the colony best defender",
                     defender, colony.getTile().getDefendingUnit(attacker));

        // Attacker wins and defender loses muskets
        crs = fakeAttackResult(CombatResult.WIN, attacker, defender);
        assertTrue("Brave v Colony (2) failed", crs.size() == 2
                   && crs.get(0) == CombatResult.WIN
                   && crs.get(1) == CombatResult.LOSE_EQUIP);
        igc.combat((ServerPlayer) inca, attacker, defender, crs);

        assertEquals("Colony size should be 1",
                     1, colony.getUnitCount());
        assertTrue("Attacker should be mounted",
                   attacker.isMounted());
        assertTrue("Attacker should be armed",
                   attacker.isArmed());
        assertEquals("Attacker should be a brave",
                     braveType, attacker.getType());
        assertFalse("Defender should not be mounted",
                    defender.isMounted());
        assertFalse("Defender should not be armed",
                    defender.isArmed());
        assertEquals("Defender should be Veteran Soldier",
                     veteranType, defender.getType());
        assertFalse("Defender should not be a defensive unit",
                    defender.isDefensiveUnit());

        // Attacker wins and slaughters the defender
        crs = fakeAttackResult(CombatResult.WIN, attacker, defender);
        assertTrue("Brave v Colony (3) failed", crs.size() == 2
                   && crs.get(0) == CombatResult.WIN
                   && crs.get(1) == CombatResult.SLAUGHTER_UNIT);
        igc.combat((ServerPlayer) inca, attacker, defender, crs);

        assertEquals("Colony size should be 1",
                     1, colony.getUnitCount());
        assertTrue("Attacker should be mounted",
                   attacker.isMounted());
        assertTrue("Attacker should be armed",
                   attacker.isArmed());
        assertEquals("Attacker should be a brave",
                     braveType, attacker.getType());
        assertTrue("Defender should be disposed",
                   defender.isDisposed());
        assertFalse("Colony should not be disposed",
                    colony.isDisposed());
        defender = colony.getDefendingUnit(attacker);

        // Attacker pillages, burning building
        assertFalse("Colony should not be pillageable",
                    colony.canBePillaged(attacker));
        Building school = new Building(game, colony, schoolHouseType);
        colony.addBuilding(school);
        assertTrue("Colony has school, should be pillageable",
                   colony.canBePillaged(attacker));
        crs = fakeAttackResult(CombatResult.WIN, attacker, defender);
        assertTrue("Brave v Colony (4) failed", crs.size() == 2
                   && crs.get(0) == CombatResult.WIN
                   && crs.get(1) == CombatResult.PILLAGE_COLONY);
        igc.combat((ServerPlayer) inca, attacker, defender, crs);

        assertTrue("Attacker should be mounted",
                   attacker.isMounted());
        assertTrue("Attacker should be armed",
                   attacker.isArmed());
        assertEquals("Attacker should be a brave",
                     braveType, attacker.getType());
        assertTrue("Colony should not be disposed",
                   !colony.isDisposed());
        assertTrue("Colony should not have a school",
                   colony.getBurnableBuildingList().isEmpty());

        // Attacker pillages, damaging ship
        assertFalse("Colony should not be pillageable",
                    colony.canBePillaged(attacker));
        Unit privateer = new Unit(game, colony.getTile(), dutch, privateerType,
                                  UnitState.ACTIVE);
        colony.getTile().setConnected(false); // no repair possible
        assertTrue("Colony has ship, should be pillageable",
                   colony.canBePillaged(attacker));
        crs = fakeAttackResult(CombatResult.WIN, attacker, defender);
        assertTrue("Brave v Colony (5) failed", crs.size() == 2
                   && crs.get(0) == CombatResult.WIN
                   && crs.get(1) == CombatResult.PILLAGE_COLONY);
        igc.combat((ServerPlayer) inca, attacker, defender, crs);

        assertTrue("Attacker should be mounted",
                   attacker.isMounted());
        assertTrue("Attacker should be armed",
                   attacker.isArmed());
        assertEquals("Attacker should be a brave",
                     braveType, attacker.getType());
        assertTrue("Colony should not be disposed",
                   !colony.isDisposed());
        assertTrue("Ship should be disposed",
                   privateer.isDisposed());

        // Attacker pillages, stealing goods
        assertFalse("Colony should not be pillageable",
                    colony.canBePillaged(attacker));
        colony.addGoods(cottonType, 100);
        assertTrue("Colony has goods, should be pillageable",
                   colony.canBePillaged(attacker));
        crs = fakeAttackResult(CombatResult.WIN, attacker, defender);
        assertTrue("Brave v Colony (6) failed", crs.size() == 2
                   && crs.get(0) == CombatResult.WIN
                   && crs.get(1) == CombatResult.PILLAGE_COLONY);
        igc.combat((ServerPlayer) inca, attacker, defender, crs);

        assertTrue("Attacker should be mounted",
                   attacker.isMounted());
        assertTrue("Attacker should be armed",
                   attacker.isArmed());
        assertEquals("Attacker should be a brave",
                     braveType, attacker.getType());
        assertTrue("Colony should not be disposed",
                   !colony.isDisposed());
        assertTrue("Colony should have lost cotton",
                   colony.getGoodsCount(cottonType) < 100);
        colony.removeGoods(cottonType);

        // Attacker pillages, stealing gold
        assertFalse("Colony should not be pillageable",
                    colony.canBePillaged(attacker));
        dutch.setGold(100);
        assertTrue("Dutch have gold, colony should be pillageable",
                   colony.canBePillaged(attacker));
        crs = fakeAttackResult(CombatResult.WIN, attacker, defender);
        assertTrue("Brave v Colony (7) failed", crs.size() == 2
                   && crs.get(0) == CombatResult.WIN
                   && crs.get(1) == CombatResult.PILLAGE_COLONY);
        igc.combat((ServerPlayer) inca, attacker, defender, crs);

        assertTrue("Attacker should be mounted",
                   attacker.isMounted());
        assertTrue("Attacker should be armed",
                   attacker.isArmed());
        assertEquals("Attacker should be a brave",
                     braveType, attacker.getType());
        assertTrue("Colony should not be disposed",
                   !colony.isDisposed());
        assertTrue("Dutch should have lost gold",
                   dutch.getGold() < 100);
        dutch.setGold(0);
        assertFalse("Colony should not be pillageable",
                    colony.canBePillaged(attacker));

        // Attacker wins and destroys the colony
        crs = fakeAttackResult(CombatResult.WIN, attacker, defender);
        assertTrue("Brave v Colony (8) failed", crs.size() == 2
                   && crs.get(0) == CombatResult.WIN
                   && crs.get(1) == CombatResult.DESTROY_COLONY);
        igc.combat((ServerPlayer) inca, attacker, defender, crs);

        assertTrue("Attacker should be mounted",
                   attacker.isMounted());
        assertTrue("Attacker should be armed",
                   attacker.isArmed());
        assertEquals("Attacker should be a brave",
                     braveType, attacker.getType());
        assertTrue("Colony should be disposed",
                    colony.isDisposed());
        assertEquals("Attacker should have moved into the colony tile",
                     colony.getTile(), attacker.getTile());
    }

    public void testLoseColonyDefenceWithRevere() {
        Game game = start(getTestMap(true));
        Map map = game.getMap();
        InGameController igc = (InGameController) server.getController();
        List<CombatResult> crs;
        Player dutch = game.getPlayer("model.nation.dutch");
        Player inca =  game.getPlayer("model.nation.inca");
        Colony colony = getStandardColony();

        dutch.setStance(inca, Stance.WAR);
        inca.setStance(dutch, Stance.WAR);
        Tile tile2 = map.getTile(4, 8);
        tile2.setExploredBy(dutch, true);
        tile2.setExploredBy(inca, true);
        Unit colonist = colony.getUnitIterator().next();
        Unit attacker = new Unit(getGame(), tile2, inca, braveType,
                                 UnitState.ACTIVE, indianHorses, indianMuskets);
        assertEquals("Colonist should be the colony best defender",
                     colonist, colony.getDefendingUnit(attacker));
        dutch.addFather(spec()
                        .getFoundingFather("model.foundingFather.paulRevere"));
        java.util.Map<GoodsType,Integer> goodsAdded
            = new HashMap<GoodsType,Integer>();
        for (AbstractGoods goods : muskets.getGoodsRequired()) {
            colony.addGoods(goods);
            goodsAdded.put(goods.getType(), goods.getAmount());
        }

        // Attacker wins, defender autoequips, but loses the muskets
        crs = fakeAttackResult(CombatResult.WIN, attacker, colonist);
        assertTrue("Inca v Colony failed", crs.size() == 3
                   && crs.get(0) == CombatResult.WIN
                   && crs.get(1) == CombatResult.AUTOEQUIP_UNIT
                   && crs.get(2) == CombatResult.LOSE_AUTOEQUIP);
        igc.combat((ServerPlayer) inca, attacker, colonist, crs);

        assertFalse("Colonist should not be disposed",
                    colonist.isDisposed());
        assertFalse("Colonist should not be captured",
                    colonist.getOwner() == attacker.getOwner());
        for (AbstractGoods goods : muskets.getGoodsRequired()) {
            boolean goodsLost = colony.getGoodsCount(goods.getType())
                < goodsAdded.get(goods.getType());
            assertTrue("Colony should have lost " + goods.getType().toString(),
                       goodsLost);
        }
    }

    public void testPioneerDiesNotLosesEquipment() {
        Game game = start(getTestMap());
        Map map = game.getMap();
        InGameController igc = (InGameController) server.getController();
        List<CombatResult> crs;
        Player dutch = game.getPlayer("model.nation.dutch");
        Player french = game.getPlayer("model.nation.french");

        Tile tile1 = map.getTile(5, 8);
        tile1.setExploredBy(dutch, true);
        tile1.setExploredBy(french, true);
        Tile tile2 = map.getTile(4, 8);
        tile2.setExploredBy(dutch, true);
        tile2.setExploredBy(french, true);
        Unit pioneer = new Unit(game, tile1, dutch, colonistType,
                                UnitState.ACTIVE);
        Unit soldier = new Unit(game, tile2, french, veteranType,
                                UnitState.ACTIVE);
        soldier.equipWith(muskets, true);
        soldier.equipWith(horses, true);
        soldier.setMovesLeft(1);
        pioneer.equipWith(tools, true);

        // Soldier wins and kills the pioneer
        crs = fakeAttackResult(CombatResult.WIN, soldier, pioneer);
        assertTrue("Soldier v Pioneer failed", crs.size() == 2
                   && crs.get(0) == CombatResult.WIN
                   && crs.get(1) == CombatResult.SLAUGHTER_UNIT);
        igc.combat((ServerPlayer) french, soldier, pioneer, crs);

        assertTrue("Pioneer should be dead",
                   pioneer.isDisposed());
    }

    public void testScoutDiesNotLosesEquipment() {
        Game game = start(getTestMap());
        Map map = game.getMap();
        InGameController igc = (InGameController) server.getController();
        List<CombatResult> crs;
        Player dutch = game.getPlayer("model.nation.dutch");
        Player french = game.getPlayer("model.nation.french");

        Tile tile1 = map.getTile(5, 8);
        tile1.setExploredBy(dutch, true);
        tile1.setExploredBy(french, true);
        Tile tile2 = map.getTile(4, 8);
        tile2.setExploredBy(dutch, true);
        tile2.setExploredBy(french, true);
        Unit scout = new Unit(game, tile1, dutch, colonistType,
                              UnitState.ACTIVE);
        Unit soldier = new Unit(game, tile2, french, veteranType,
                                UnitState.ACTIVE);
        soldier.equipWith(horses, true);
        soldier.equipWith(muskets, true);
        scout.setMovesLeft(1);
        scout.equipWith(horses, true);

        // Soldier wins and kills the scout
        crs = fakeAttackResult(CombatResult.WIN, soldier, scout);
        assertTrue("Soldier v scout failed", crs.size() == 2
                   && crs.get(0) == CombatResult.WIN
                   && crs.get(1) == CombatResult.SLAUGHTER_UNIT);
        igc.combat((ServerPlayer) french, soldier, scout, crs);

        assertTrue("Scout should be dead",
                   scout.isDisposed());
    }

    public void testPromotion() {
        Game game = start(getTestMap(plains));
        Map map = game.getMap();
        InGameController igc = (InGameController) server.getController();
        List<CombatResult> crs;
        Player dutch = game.getPlayer("model.nation.dutch");
        Player french = game.getPlayer("model.nation.french");

        // UnitType promotion
        assertEquals("Criminals should promote to servants",
                     indenturedServantType, pettyCriminalType
                     .getUnitTypeChange(ChangeType.PROMOTION, dutch));
        assertEquals("Servants should promote to colonists",
                     colonistType, indenturedServantType
                     .getUnitTypeChange(ChangeType.PROMOTION, dutch));
        assertEquals("Colonists should promote to Veterans",
                     veteranType, colonistType
                     .getUnitTypeChange(ChangeType.PROMOTION, dutch));
        assertEquals("Veterans should not promote to Colonials (yet)",
                     null, veteranType
                     .getUnitTypeChange(ChangeType.PROMOTION, dutch));
        // Only independent players can own colonial regulars
        assertEquals("Colonials should not be promotable",
                     null, colonialType
                     .getUnitTypeChange(ChangeType.PROMOTION, dutch));
        assertEquals("Artillery should not be promotable",
                     null, artilleryType
                     .getUnitTypeChange(ChangeType.PROMOTION, dutch));
        assertEquals("Kings regulars should not be promotable",
                     null, kingsRegularType
                     .getUnitTypeChange(ChangeType.PROMOTION, dutch));
        assertEquals("Indian converts should not be promotable",
                     null, indianConvertType
                     .getUnitTypeChange(ChangeType.PROMOTION, dutch));
        Tile tile1 = map.getTile(5, 8);
        tile1.setExploredBy(dutch, true);
        tile1.setExploredBy(french, true);
        Tile tile2 = map.getTile(4, 8);
        tile2.setExploredBy(dutch, true);
        tile2.setExploredBy(french, true);
        Unit unit = new Unit(game, tile1, dutch, pettyCriminalType,
                             UnitState.ACTIVE);
        Unit soldier = new Unit(game, tile2, french, colonistType,
                                UnitState.ACTIVE);
        unit.equipWith(muskets, true);
        soldier.equipWith(muskets, true);
        // Enable automatic promotion
        dutch.getFeatureContainer()
            .addAbility(new Ability("model.ability.automaticPromotion"));

        // Criminal -> Servant
        crs = fakeAttackResult(CombatResult.WIN, unit, soldier);
        assertTrue("Criminal promotion failed", crs.size() == 3
                   && crs.get(0) == CombatResult.WIN
                   && crs.get(1) == CombatResult.LOSE_EQUIP
                   && crs.get(2) == CombatResult.PROMOTE_UNIT);
        igc.combat((ServerPlayer) dutch, unit, soldier, crs);

        assertEquals("Criminal should be promoted to servant",
                     unit.getType(), indenturedServantType);

        // Servant -> Colonist
        soldier.equipWith(muskets, true);
        crs = fakeAttackResult(CombatResult.WIN, unit, soldier);
        assertTrue("Servant promotion failed", crs.size() == 3
                   && crs.get(0) == CombatResult.WIN
                   && crs.get(1) == CombatResult.LOSE_EQUIP
                   && crs.get(2) == CombatResult.PROMOTE_UNIT);
        igc.combat((ServerPlayer) dutch, unit, soldier, crs);

        assertEquals("Servant should be promoted to colonist",
                     unit.getType(), colonistType);

        // Colonist -> Veteran
        soldier.equipWith(muskets, true);
        crs = fakeAttackResult(CombatResult.WIN, unit, soldier);
        assertTrue("Colonist promotion failed", crs.size() == 3
                   && crs.get(0) == CombatResult.WIN
                   && crs.get(1) == CombatResult.LOSE_EQUIP
                   && crs.get(2) == CombatResult.PROMOTE_UNIT);
        igc.combat((ServerPlayer) dutch, unit, soldier, crs);

        assertEquals("Colonist should be promoted to Veteran",
                     unit.getType(), veteranType);

        // Further upgrading a VeteranSoldier to ColonialRegular
        // should only work once independence is declared.  Must set
        // the new nation name or combat crashes in message generation.
        assertFalse("Colonial Regulars should not yet be available",
                    colonialType.isAvailableTo(dutch));
        dutch.setPlayerType(PlayerType.REBEL);
        dutch.getFeatureContainer()
            .addAbility(new Ability("model.ability.independenceDeclared"));
        dutch.setIndependentNationName("Vrije Nederlands");
        assertTrue("Colonial Regulars should be available",
                   colonialType.isAvailableTo(dutch));
        assertEquals("Veterans should promote to Colonial Regulars",
                     colonialType, veteranType
                     .getUnitTypeChange(ChangeType.PROMOTION, dutch));

        // Veteran -> Colonial Regular
        soldier.equipWith(muskets, true);
        crs = fakeAttackResult(CombatResult.WIN, unit, soldier);
        assertTrue("Veteran promotion failed", crs.size() == 3
                   && crs.get(0) == CombatResult.WIN
                   && crs.get(1) == CombatResult.LOSE_EQUIP
                   && crs.get(2) == CombatResult.PROMOTE_UNIT);
        igc.combat((ServerPlayer) dutch, unit, soldier, crs);

        assertEquals("Veteran should be promoted to Colonial Regular",
                     unit.getType(), colonialType);

        // No further promotion should work
        soldier.equipWith(muskets, true);
        crs = fakeAttackResult(CombatResult.WIN, unit, soldier);
        assertTrue("Colonial Regular over-promotion failed",
                   crs.size() == 2
                   && crs.get(0) == CombatResult.WIN
                   && crs.get(1) == CombatResult.LOSE_EQUIP);
        igc.combat((ServerPlayer) dutch, unit, soldier, crs);

        assertEquals("Colonial Regular should still be Colonial Regular",
                     unit.getType(), colonialType);
    }

    public void testColonistDemotedBySoldier() {
        Game game = start(getTestMap(plains));
        Map map = game.getMap();
        InGameController igc = (InGameController) server.getController();
        List<CombatResult> crs;
        Player dutch = game.getPlayer("model.nation.dutch");
        Player french = game.getPlayer("model.nation.french");

        Tile tile1 = map.getTile(5, 8);
        tile1.setExploredBy(dutch, true);
        tile1.setExploredBy(french, true);
        Tile tile2 = map.getTile(4, 8);
        tile2.setExploredBy(dutch, true);
        tile2.setExploredBy(french, true);
        Unit colonist = new Unit(game, tile1, dutch, colonistType,
                                 UnitState.ACTIVE);
        assertTrue("Colonists should be capturable",
                   colonist.hasAbility("model.ability.canBeCaptured"));
        Unit soldier = new Unit(game, tile2, french, colonistType,
                                UnitState.ACTIVE);
        assertTrue("Soldier should be capturable",
                   soldier.hasAbility("model.ability.canBeCaptured"));
        soldier.equipWith(muskets, true);
        assertFalse("Armed soldier should not be capturable",
                    soldier.hasAbility("model.ability.canBeCaptured"));

        // Colonist loses and is captured
        crs = fakeAttackResult(CombatResult.LOSE, colonist, soldier);
        assertTrue("Colonist v Soldier failed", crs.size() == 2
                   && crs.get(0) == CombatResult.LOSE
                   && crs.get(1) == CombatResult.CAPTURE_UNIT);
        igc.combat((ServerPlayer) dutch, colonist, soldier, crs);

        assertEquals("Colonist should still be a colonist",
                     colonistType, colonist.getType());
        assertEquals("Colonist should be captured",
                     french, colonist.getOwner());
        assertEquals("Colonist should have moved to the soldier tile",
                     tile2, colonist.getTile());
    }

    public void testSoldierDemotedBySoldier() {
        Game game = start(getTestMap(plains));
        Map map = game.getMap();
        InGameController igc = (InGameController) server.getController();
        List<CombatResult> crs;
        Player dutch = game.getPlayer("model.nation.dutch");
        Player french = game.getPlayer("model.nation.french");

        Tile tile1 = map.getTile(5, 8);
        tile1.setExploredBy(dutch, true);
        tile1.setExploredBy(french, true);
        Tile tile2 = map.getTile(4, 8);
        tile2.setExploredBy(dutch, true);
        tile2.setExploredBy(french, true);
        Unit soldier1 = new Unit(game, tile1, dutch, colonistType,
                                 UnitState.ACTIVE);
        soldier1.equipWith(muskets, true);
        Unit soldier2 = new Unit(game, tile2, french, colonistType,
                                 UnitState.ACTIVE);
        soldier2.equipWith(muskets, true);

        // Soldier loses and loses muskets
        crs = fakeAttackResult(CombatResult.LOSE, soldier1, soldier2);
        assertTrue("Soldier should lose equipment", crs.size() == 2
                   && crs.get(0) == CombatResult.LOSE
                   && crs.get(1) == CombatResult.LOSE_EQUIP);
        igc.combat((ServerPlayer) dutch, soldier1, soldier2, crs);

        assertEquals("Soldier should be a colonist",
                     colonistType, soldier1.getType());
        assertEquals("Soldier should still be Dutch",
                     dutch, soldier1.getOwner());
        assertEquals("Soldier should not have moved",
                     tile1, soldier1.getTile());
        assertTrue("Soldier should have lost equipment",
                   soldier1.getEquipment().isEmpty());

        // Soldier loses and is captured
        crs = fakeAttackResult(CombatResult.LOSE, soldier1, soldier2);
        assertTrue("Soldier v soldier failed", crs.size() == 2
                   && crs.get(0) == CombatResult.LOSE
                   && crs.get(1) == CombatResult.CAPTURE_UNIT);
        igc.combat((ServerPlayer) dutch, soldier1, soldier2, crs);

        assertEquals("Soldier should be a colonist",
                     colonistType, soldier1.getType());
        assertEquals("Soldier should now be French",
                     french, soldier1.getOwner());
        assertEquals("Soldier should have moved",
                     tile2, soldier1.getTile());
    }

    public void testDragoonDemotedBySoldier() {
        Game game = start(getTestMap(plains));
        Map map = game.getMap();
        InGameController igc = (InGameController) server.getController();
        List<CombatResult> crs;
        Player dutch = game.getPlayer("model.nation.dutch");
        Player french = game.getPlayer("model.nation.french");

        Tile tile1 = map.getTile(5, 8);
        tile1.setExploredBy(dutch, true);
        tile1.setExploredBy(french, true);
        Tile tile2 = map.getTile(4, 8);
        tile2.setExploredBy(dutch, true);
        tile2.setExploredBy(french, true);
        Unit dragoon = new Unit(game, tile1, dutch, colonistType,
                                UnitState.ACTIVE);
        dragoon.equipWith(muskets, true);
        dragoon.equipWith(horses, true);
        dragoon.newTurn();
        assertEquals("Dragoon has 12 moves",
                     12, dragoon.getInitialMovesLeft());
        assertEquals("Dragoon has 12 moves left",
                     12, dragoon.getMovesLeft());
        Unit soldier = new Unit(game, tile2, french, colonistType,
                                UnitState.ACTIVE);
        soldier.equipWith(muskets, true);

        // Dragoon loses and loses horses
        crs = fakeAttackResult(CombatResult.LOSE, dragoon, soldier);
        assertTrue("Dragoon v soldier (1) failed", crs.size() == 2
                   && crs.get(0) == CombatResult.LOSE
                   && crs.get(1) == CombatResult.LOSE_EQUIP);
        igc.combat((ServerPlayer) dutch, dragoon, soldier, crs);

        assertEquals("Dragoon should be a dragoon",
                     colonistType, dragoon.getType());
        assertEquals("Dragoon should be Dutch",
                     dutch, dragoon.getOwner());
        assertEquals("Dragoon should be on Tile1",
                     tile1, dragoon.getTile());
        assertEquals("Dragoon should have equipment",
                     1, dragoon.getEquipment().size());
        assertEquals("Dragoon should have muskets",
                     1, dragoon.getEquipment().getCount(muskets));
        assertEquals("Dragoon has 3 moves",
                     3, dragoon.getInitialMovesLeft());
        assertEquals("Dragoon has 0 moves left",
                     0, dragoon.getMovesLeft());

        crs = fakeAttackResult(CombatResult.LOSE, dragoon, soldier);
        assertTrue("Dragoon v soldier (2) failed", crs.size() == 2
                   && crs.get(0) == CombatResult.LOSE
                   && crs.get(1) == CombatResult.LOSE_EQUIP);
        igc.combat((ServerPlayer) dutch, dragoon, soldier, crs);

        assertEquals("Dragoon should be a dragoon",
                     colonistType, dragoon.getType());
        assertEquals("Dragoon should be Dutch",
                     dutch, dragoon.getOwner());
        assertEquals("Dragoon should be on Tile1",
                     tile1, dragoon.getTile());
        assertTrue("Dragoon should have no equipment",
                   dragoon.getEquipment().isEmpty());

        crs = fakeAttackResult(CombatResult.LOSE, dragoon, soldier);
        assertTrue("Dragoon v soldier (3) failed", crs.size() == 2
                   && crs.get(0) == CombatResult.LOSE
                   && crs.get(1) == CombatResult.CAPTURE_UNIT);
        igc.combat((ServerPlayer) dutch, dragoon, soldier, crs);

        assertEquals("Dragoon should be demoted",
                     colonistType, dragoon.getType());
        assertEquals("Dragoon should be French",
                     french, dragoon.getOwner());
        assertEquals("Dragoon should be on Tile2",
                     tile2, dragoon.getTile());
    }

    public void testDragoonDemotedByBrave() {
        Game game = start(getTestMap(plains));
        Map map = game.getMap();
        InGameController igc = (InGameController) server.getController();
        List<CombatResult> crs;
        Player dutch = game.getPlayer("model.nation.dutch");
        Player inca = game.getPlayer("model.nation.inca");

        Tile tile1 = map.getTile(5, 8);
        tile1.setExploredBy(dutch, true);
        tile1.setExploredBy(inca, true);
        Tile tile2 = map.getTile(4, 8);
        tile2.setExploredBy(dutch, true);
        tile2.setExploredBy(inca, true);
        // Build indian settlements
        FreeColTestCase.IndianSettlementBuilder builder
            = new FreeColTestCase.IndianSettlementBuilder(game);
        builder.player(inca).settlementTile(map.getTile(1, 1))
            .capital(true).skillToTeach(null);
        IndianSettlement settlement1 = builder.build();
        builder.reset().player(inca).settlementTile(map.getTile(8, 8))
            .skillToTeach(null);
        IndianSettlement settlement2 = builder.build();
        Unit dragoon = new Unit(game, tile1, dutch, colonistType,
                                UnitState.ACTIVE);
        dragoon.equipWith(muskets, true);
        dragoon.equipWith(horses, true);
        Unit brave = new Unit(game, tile2, inca, braveType, UnitState.ACTIVE);
        brave.setIndianSettlement(settlement1);

        // Dragoon loses and brave captures its horses
        crs = fakeAttackResult(CombatResult.LOSE, dragoon, brave);
        assertTrue("Dragoon v Brave (1) failed", crs.size() == 2
                   && crs.get(0) == CombatResult.LOSE
                   && crs.get(1) == CombatResult.CAPTURE_EQUIP);
        igc.combat((ServerPlayer) dutch, dragoon, brave, crs);

        assertEquals("Colonist should be a dragoon",
                     colonistType, dragoon.getType());
        assertEquals("Dragoon should be Dutch",
                     dutch, dragoon.getOwner());
        assertEquals("Dragoon should be on Tile1",
                     tile1, dragoon.getTile());
        assertEquals("Dragoon should have equipment",
                     1, dragoon.getEquipment().size());
        assertEquals("Dragoon should have muskets",
                     1, dragoon.getEquipment().getCount(muskets));
        assertEquals("Brave should have equipment",
                     1, brave.getEquipment().size());
        assertEquals("Brave should have Indian Horses",
                     1, brave.getEquipment().getCount(indianHorses));
        assertEquals("Braves settlement should have 25 Indian Horses",
                     25, settlement1.getGoodsCount(horsesType));
        assertEquals("Other settlement should not have horses",
                     0, settlement2.getGoodsCount(horsesType));

        // Dragoon loses and brave captures its muskets
        crs = fakeAttackResult(CombatResult.LOSE, dragoon, brave);
        assertTrue("Dragoon v Brave (2) failed", crs.size() == 2
                   && crs.get(0) == CombatResult.LOSE
                   && crs.get(1) == CombatResult.CAPTURE_EQUIP);
        igc.combat((ServerPlayer) dutch, dragoon, brave, crs);

        assertEquals("Colonist should be a dragoon",
                     colonistType, dragoon.getType());
        assertEquals("Dragoon should be Dutch",
                     dutch, dragoon.getOwner());
        assertEquals("Dragoon should be on Tile1",
                     tile1, dragoon.getTile());
        assertTrue("Dragoon should not have equipment",
                   dragoon.getEquipment().isEmpty());
        assertEquals("Brave should have more equipment",
                     2, brave.getEquipment().size());
        assertEquals("Brave should have Indian Horses",
                     1, brave.getEquipment().getCount(indianHorses));
        assertEquals("Brave should have Indian Muskets",
                     1, brave.getEquipment().getCount(indianMuskets));
        assertEquals("Braves settlement should have 25 Indian Muskets",
                     25, settlement1.getGoodsCount(musketType));
        assertEquals("Other settlement should not have muskets",
                     0, settlement2.getGoodsCount(musketType));

        // Dragoon loses and is slaughtered
        crs = fakeAttackResult(CombatResult.LOSE, dragoon, brave);
        assertTrue("Dragoon v Brave (3) failed", crs.size() == 2
                   && crs.get(0) == CombatResult.LOSE
                   && crs.get(1) == CombatResult.SLAUGHTER_UNIT);
        igc.combat((ServerPlayer) dutch, dragoon, brave, crs);

        assertTrue("Dragoon should be disposed",
                   dragoon.isDisposed());
    }

    public void testScoutDemotedBySoldier() {
        Game game = start(getTestMap(plains));
        Map map = game.getMap();
        InGameController igc = (InGameController) server.getController();
        List<CombatResult> crs;

        Player dutch = game.getPlayer("model.nation.dutch");
        Player french = game.getPlayer("model.nation.french");
        Tile tile1 = map.getTile(5, 8);
        tile1.setExploredBy(dutch, true);
        tile1.setExploredBy(french, true);
        Tile tile2 = map.getTile(4, 8);
        tile2.setExploredBy(dutch, true);
        tile2.setExploredBy(french, true);
        Unit scout = new Unit(game, tile1, dutch, colonistType,
                              UnitState.ACTIVE);
        scout.equipWith(horses, true);
        Unit soldier = new Unit(game, tile2, french, colonistType,
                                UnitState.ACTIVE);
        soldier.equipWith(muskets, true);

        // Scout loses and is slaughtered
        crs = fakeAttackResult(CombatResult.LOSE, scout, soldier);
        assertTrue("Scout v Soldier failed", crs.size() == 2
                   && crs.get(0) == CombatResult.LOSE
                   && crs.get(1) == CombatResult.SLAUGHTER_UNIT);
        igc.combat((ServerPlayer) dutch, scout, soldier, crs);

        assertTrue("Scout should be disposed",
                   scout.isDisposed());
    }

    public void testVeteranSoldierDemotedBySoldier() {
        Game game = start(getTestMap(plains));
        Map map = game.getMap();
        InGameController igc = (InGameController) server.getController();
        List<CombatResult> crs;

        Player dutch = game.getPlayer("model.nation.dutch");
        Player french = game.getPlayer("model.nation.french");
        Tile tile1 = map.getTile(5, 8);
        tile1.setExploredBy(dutch, true);
        tile1.setExploredBy(french, true);
        Tile tile2 = map.getTile(4, 8);
        tile2.setExploredBy(dutch, true);
        tile2.setExploredBy(french, true);
        Unit soldier1 = new Unit(game, tile1, dutch, veteranType,
                                 UnitState.ACTIVE);
        soldier1.equipWith(muskets, true);
        Unit soldier2 = new Unit(game, tile2, french, colonistType,
                                 UnitState.ACTIVE);
        soldier2.equipWith(muskets, true);
        assertEquals("Veterans should become colonists on capture",
                     colonistType, veteranType
                     .getUnitTypeChange(ChangeType.CAPTURE, dutch));

        // Soldier loses and loses equipment
        crs = fakeAttackResult(CombatResult.LOSE, soldier1, soldier2);
        assertTrue("Soldier v Soldier failed", crs.size() == 2
                   && crs.get(0) == CombatResult.LOSE
                   && crs.get(1) == CombatResult.LOSE_EQUIP);
        igc.combat((ServerPlayer) dutch, soldier1, soldier2, crs);

        assertEquals("Soldier1 should be a Veteran",
                     veteranType, soldier1.getType());
        assertEquals("Soldier1 should be Dutch",
                     dutch, soldier1.getOwner());
        assertEquals("Soldier1 should be on Tile1",
                     tile1, soldier1.getTile());
        assertTrue("Soldier1 should not have equipment",
                   soldier1.getEquipment().isEmpty());

        // Soldier1 loses and is captured
        crs = fakeAttackResult(CombatResult.LOSE, soldier1, soldier2);
        assertTrue("Soldier1 v Soldier2 failed", crs.size() == 2
                   && crs.get(0) == CombatResult.LOSE
                   && crs.get(1) == CombatResult.CAPTURE_UNIT);
        igc.combat((ServerPlayer) dutch, soldier1, soldier2, crs);

        assertEquals("Soldier1 should not be a colonist",
                     colonistType, soldier1.getType());
        assertEquals("Soldier1 should be French",
                     french, soldier1.getOwner());
        assertEquals("Soldier1 should be have moved",
                     tile2, soldier1.getTile());
    }

    public void testArtilleryDemotedBySoldier() {
        Game game = start(getTestMap(plains));
        Map map = game.getMap();
        InGameController igc = (InGameController) server.getController();
        List<CombatResult> crs;

        Player dutch = game.getPlayer("model.nation.dutch");
        Player french = game.getPlayer("model.nation.french");
        Tile tile1 = map.getTile(5, 8);
        tile1.setExploredBy(dutch, true);
        tile1.setExploredBy(french, true);
        Tile tile2 = map.getTile(4, 8);
        tile2.setExploredBy(dutch, true);
        tile2.setExploredBy(french, true);
        Unit artillery = new Unit(game, tile1, dutch, artilleryType,
                                  UnitState.ACTIVE);
        Unit soldier = new Unit(game, tile2, french, colonistType,
                                UnitState.ACTIVE);
        soldier.equipWith(muskets, true);
        assertEquals("Artillery should demote to damaged artillery",
                     damagedArtilleryType, artilleryType
                     .getUnitTypeChange(ChangeType.DEMOTION, dutch));

        // Artillery loses and is demoted
        crs = fakeAttackResult(CombatResult.LOSE, artillery, soldier);
        assertTrue("Artillery v Soldier (1) failed", crs.size() == 2
                   && crs.get(0) == CombatResult.LOSE
                   && crs.get(1) == CombatResult.DEMOTE_UNIT);
        igc.combat((ServerPlayer) dutch, artillery, soldier, crs);

        assertEquals("Artillery should be damaged artillery",
                     damagedArtilleryType, artillery.getType());
        assertEquals("Artillery should be Dutch",
                     dutch, artillery.getOwner());
        assertEquals("Artillery should be on Tile1",
                     tile1, artillery.getTile());

        // Artillery loses and is slaughtered
        crs = fakeAttackResult(CombatResult.LOSE, artillery, soldier);
        assertTrue("Artillery v Soldier (2) failed", crs.size() == 2
                   && crs.get(0) == CombatResult.LOSE
                   && crs.get(1) == CombatResult.SLAUGHTER_UNIT);
        igc.combat((ServerPlayer) dutch, artillery, soldier, crs);

        assertTrue("Artillery should be disposed",
                   artillery.isDisposed());
    }

}
