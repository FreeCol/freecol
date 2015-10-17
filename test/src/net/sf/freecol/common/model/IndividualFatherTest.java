/**
 *  Copyright (C) 2002-2015  The FreeCol Team
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
import java.util.Set;

import net.sf.freecol.common.model.Direction;
import net.sf.freecol.common.model.UnitLocation.NoAddReason;
import net.sf.freecol.server.control.ChangeSet;
import net.sf.freecol.server.model.ServerPlayer;
import net.sf.freecol.server.model.ServerUnit;
import net.sf.freecol.util.test.FreeColTestCase;


/**
 * Test cases for individual Founding Fathers.
 *
 * FIXME: there should be a test case for each Founding Father.
 */
public class IndividualFatherTest extends FreeColTestCase {

    private static final BuildableType customHouseType
        = spec().getBuildingType("model.building.customHouse");
    private static final BuildingType townHallType
        = spec().getBuildingType("model.building.townHall");

    private static final FoundingFather bartolomeDeLasCasas
        = spec().getFoundingFather("model.foundingFather.bartolomeDeLasCasas");
    private static final FoundingFather hernanCortes
        = spec().getFoundingFather("model.foundingFather.hernanCortes");
    private static final FoundingFather janDeWitt
        = spec().getFoundingFather("model.foundingFather.janDeWitt");
    private static final FoundingFather jeanDeBrebeuf
        = spec().getFoundingFather("model.foundingFather.fatherJeanDeBrebeuf");
    private static final FoundingFather paulRevere
        = spec().getFoundingFather("model.foundingFather.paulRevere");
    private static final FoundingFather peterMinuit
        = spec().getFoundingFather("model.foundingFather.peterMinuit");
    private static final FoundingFather peterStuyvesant
        = spec().getFoundingFather("model.foundingFather.peterStuyvesant");
    private static final FoundingFather simonBolivar
        = spec().getFoundingFather("model.foundingFather.simonBolivar");
    private static final FoundingFather thomasJefferson
        = spec().getFoundingFather("model.foundingFather.thomasJefferson");
    private static final FoundingFather thomasPaine
        = spec().getFoundingFather("model.foundingFather.thomasPaine");
    private static final FoundingFather williamBrewster
        = spec().getFoundingFather("model.foundingFather.williamBrewster");

    private static final GoodsType bellsType
        = spec().getGoodsType("model.goods.bells");
    private static final GoodsType horsesType
        = spec().getGoodsType("model.goods.horses");
    private static final GoodsType musketsType
        = spec().getGoodsType("model.goods.muskets");

    private static final Role soldierRole
        = spec().getRole("model.role.soldier");
    private static final Role missionaryRole
        = spec().getRole("model.role.missionary");

    private static final UnitType servantType
        = spec().getUnitType("model.unit.indenturedServant");
    private static final UnitType colonistType
        = spec().getUnitType("model.unit.freeColonist");
    private static final UnitType statesmanType
        = spec().getUnitType("model.unit.elderStatesman");


    public void testBolivar() {
        Game game = getGame();
        game.setMap(getTestMap(true));

        final int pop = 5;
        Colony colony = getStandardColony(pop);
        Player player = colony.getOwner();
        List<AbstractGoods> empty = new ArrayList<>();
        Building townHall = colony.getBuilding(townHallType);

        // Zero to start
        assertEquals(0, colony.getLiberty());
        assertEquals(0, colony.getSoL());

        // Add enough to raise 2 out of 5 to rebels => 40%
        int inc = 2 * Colony.LIBERTY_PER_REBEL;
        colony.addLiberty(inc);
        assertEquals(inc, colony.getLiberty());
        assertEquals(100 * inc / (pop * Colony.LIBERTY_PER_REBEL),
                     colony.getSoL());

        // Add Bolivar and check that percentage is 20% higher
        player.addFather(simonBolivar);
        colony.addLiberty(0); // provoke recalculation
        assertEquals(inc, colony.getLiberty());
        assertEquals(100 * inc / (pop * Colony.LIBERTY_PER_REBEL) + 20,
                     colony.getSoL());

        // Is the modifier present
        Set<Modifier> modifierSet = player.getModifiers(Modifier.SOL);
        assertEquals(1, modifierSet.size());
        Modifier bolivarModifier = modifierSet.iterator().next();
        assertEquals(simonBolivar, bolivarModifier.getSource());

        // Check that SoL stops at 100%
        colony.addLiberty(pop * Colony.LIBERTY_PER_REBEL);
        assertEquals(inc + pop * Colony.LIBERTY_PER_REBEL,
                     player.getLiberty());
        assertEquals(100, player.getSoL());
    }

    public void testBrebeuf() {
        Game game = getGame();
        Player dutch = game.getPlayerByNationId("model.nation.dutch");
        String ability = Ability.EXPERT_MISSIONARY;

        assertTrue(jeanDeBrebeuf.hasAbility(ability));
        assertFalse(dutch.hasAbility(ability));

        Unit missionary = new ServerUnit(game, null, dutch, 
                                         colonistType, missionaryRole);
        assertEquals(missionaryRole, missionary.getRole());
        assertTrue(missionary.hasAbility(Ability.ESTABLISH_MISSION));
        assertFalse(missionary.hasAbility(ability));

        dutch.addFather(jeanDeBrebeuf);
        assertTrue(dutch.hasAbility(ability));
        assertTrue(missionary.hasAbility(ability));
    }

    public void testBrewster() {
        Game game = getGame();
        Player dutch = game.getPlayerByNationId("model.nation.dutch");

        String ability = Ability.CAN_RECRUIT_UNIT;
        assertTrue(dutch.hasAbility(ability));

        for (UnitType unitType : spec().getUnitTypeList()) {
            if (unitType.isRecruitable()) {
                assertTrue("Unable to recruit " + unitType.toString(),
                    dutch.hasAbility(ability, unitType));
            }
        }

        dutch.addFather(williamBrewster);
        // ability is no longer general, but limited to certain unit types
        assertFalse(dutch.hasAbility(ability));

        for (UnitType unitType : spec().getUnitTypeList()) {
            if (unitType.isRecruitable()) {
                if (unitType.getSkill() < 0) {
                    assertFalse("Able to recruit " + unitType.toString(),
                        dutch.hasAbility(ability, unitType));
                } else {
                    assertTrue("Unable to recruit " + unitType.toString(),
                        dutch.hasAbility(ability, unitType));
                }
            }
        }
    }

    public void testCortes() {
        Game game = getGame();
        game.setMap(getTestMap(true));

        Colony colony = getStandardColony(4);
        Player player = colony.getOwner();
        Unit unit = colony.getUnitList().get(0);

        NationType inca = spec().getNationType("model.nationType.inca");
        SettlementType incaCity
            = inca.getSettlementType("model.settlement.inca");

        RandomRange range = incaCity.getPlunderRange(unit);
        assertEquals(2100, range.getFactor());

        player.addFather(hernanCortes);

        range = incaCity.getPlunderRange(unit);
        assertEquals(3100, range.getFactor());
    }

    public void testDeLasCasas() {
        Game game = getGame();
        game.setMap(getTestMap(true));

        java.util.Map<UnitType, UnitType> upgrades = bartolomeDeLasCasas.getUpgrades();

        assertFalse(upgrades.isEmpty());

        for (java.util.Map.Entry<UnitType, UnitType> entry : upgrades.entrySet()) {
            assertEquals(entry.getKey(), spec().getUnitType(entry.getKey().getId()));
            assertEquals(entry.getValue(), spec().getUnitType(entry.getValue().getId()));
        }

        Colony colony = getStandardColony(4);
        ServerPlayer player = (ServerPlayer)colony.getOwner();
        Unit unit = colony.getUnitList().get(0);

        java.util.Map.Entry<UnitType, UnitType> entry = upgrades.entrySet().iterator().next();
        unit.setType(entry.getKey());

        player.csAddFoundingFather(bartolomeDeLasCasas, null, new ChangeSet());
        assertEquals(unit.getType(), entry.getValue());
    }

    public void testDeWitt() {
        Game game = getGame();
        Player dutch = game.getPlayerByNationId("model.nation.dutch");
        Player french = game.getPlayerByNationId("model.nation.french");
        dutch.getMarket().setArrears(musketsType, 1);

        assertFalse(dutch.canTrade(musketsType, Market.Access.EUROPE));
        assertFalse(dutch.canTrade(musketsType, Market.Access.CUSTOM_HOUSE));

        dutch.addFather(janDeWitt);

        assertFalse(dutch.canTrade(musketsType, Market.Access.EUROPE));
        assertFalse(dutch.canTrade(musketsType, Market.Access.CUSTOM_HOUSE));

        dutch.setStance(french, Stance.WAR);

        assertFalse(dutch.canTrade(musketsType, Market.Access.EUROPE));
        assertFalse(dutch.canTrade(musketsType, Market.Access.CUSTOM_HOUSE));

        dutch.setStance(french, Stance.PEACE);

        assertFalse(dutch.canTrade(musketsType, Market.Access.EUROPE));
        assertTrue(dutch.canTrade(musketsType, Market.Access.CUSTOM_HOUSE));
    }

    public void testJefferson() {
        Game game = getGame();
        game.setMap(getTestMap(true));

        Set<Modifier> jeffersonModifiers
            = thomasJefferson.getModifiers("model.goods.bells");
        assertEquals(1, jeffersonModifiers.size());
        Modifier modifier = jeffersonModifiers.iterator().next();
        assertTrue(modifier.appliesTo(townHallType));

        Colony colony = getStandardColony(4);
        Player player = colony.getOwner();
        Building townHall = colony.getBuilding(townHallType);
        clearWorkLocation(townHall);
        Unit unit = colony.getFirstUnit();
        townHall.add(unit);

        assertEquals(0, player.getModifiers("model.goods.bells").size());
        assertEquals(0, colony.getModifiers("model.goods.bells").size());
        int expected = 4;
        assertEquals(expected, townHall.getTotalProductionOf(bellsType));

        player.addFather(thomasJefferson);
        expected += expected * 0.5; // Add Jefferson bonus
        assertEquals(1, player.getModifiers("model.goods.bells").size());
        assertEquals(0, colony.getModifiers("model.goods.bells").size());
        assertEquals(1, townHall.getProductionModifiers(bellsType, null).size());
        assertEquals(expected, townHall.getTotalProductionOf(bellsType));
    }

    public void testMinuit() {
        Game game = getGame();
        game.setMap(getTestMap(true));

        Colony colony = getStandardColony();
        Unit unit = colony.getUnitList().get(0);
        Player player = colony.getOwner();
        Player iroquois = getGame().getPlayerByNationId("model.nation.iroquois");
        Tile colonyCenterTile = colony.getTile();
        Tile disputedTile = colonyCenterTile.getNeighbourOrNull(Direction.N);
        Tile settlementTile = disputedTile.getNeighbourOrNull(Direction.N);
        assertNull(settlementTile.getOwner());

        FreeColTestCase.IndianSettlementBuilder builder
            = new FreeColTestCase.IndianSettlementBuilder(getGame());
        IndianSettlement indianSettlement
            = builder.player(iroquois).settlementTile(settlementTile)
            .skillToTeach(null).build();

        // Set up disputed tile
        ColonyTile colonyTile = colony.getColonyTile(disputedTile);
        for (Unit u : colonyTile.getUnitList()) {
            u.setLocation(colony.getTile());
        }
        disputedTile.changeOwnership(iroquois, indianSettlement);

        assertNotNull(settlementTile.getSettlement());
        assertTrue(player.getLandPrice(disputedTile) > 0);
        assertFalse(colony.getColonyTile(disputedTile).canAdd(unit));

        player.addFather(peterMinuit);

        assertEquals("Tile should be zero cost",
            0, player.getLandPrice(disputedTile));
        assertEquals("Should still have to claim the tile",
            NoAddReason.CLAIM_REQUIRED, colonyTile.getNoAddReason(unit));
    }

    public void testPaine() {
        Game game = getGame();
        game.setMap(getTestMap(true));
        Colony colony = getStandardColony(4);
        Player player = colony.getOwner();
        List<AbstractGoods> empty = new ArrayList<>();
        Building townHall = colony.getBuilding(townHallType);
        clearWorkLocation(townHall);

        Unit statesman1 = colony.getUnitList().get(0);
        Unit statesman2 = colony.getUnitList().get(1);
        Unit statesman3 = colony.getUnitList().get(2);

        statesman1.setType(statesmanType);
        statesman1.setLocation(townHall);
        assertEquals(6 + 1, townHall.getTotalProductionOf(bellsType));

        statesman2.setType(statesmanType);
        statesman2.setLocation(townHall);
        assertEquals(2 * 6 + 1, townHall.getTotalProductionOf(bellsType));

        statesman3.setType(statesmanType);
        statesman3.setLocation(townHall);
        assertEquals(3 * 6 + 1, townHall.getTotalProductionOf(bellsType));

        player.setTax(20);
        assertEquals(3 * 6 + 1, townHall.getTotalProductionOf(bellsType));

        player.addFather(thomasPaine);
        player.recalculateBellsBonus();

        assertTrue(player.hasAbility(Ability.ADD_TAX_TO_BELLS));
        Set<Modifier> modifierSet = player.getModifiers("model.goods.bells");
        assertEquals(1, modifierSet.size());

        Modifier paineModifier = modifierSet.iterator().next();
        assertEquals(thomasPaine, paineModifier.getSource());
        assertEquals(player.getTax(), (int) paineModifier.getValue());

        int expected = (int) (3 * 6 * 1.2f + 1);
        assertEquals(expected, townHall.getTotalProductionOf(bellsType));

        player.setTax(30);
        player.recalculateBellsBonus();

        expected = (int) (3 * 6 * 1.3f + 1);
        assertEquals(expected, townHall.getTotalProductionOf(bellsType));
    }

    public void testRevere() {
        Game game = getGame();
        game.setMap(getTestMap());

        Colony colony = getStandardColony();
        Player player = colony.getOwner();
        Unit colonist = colony.getUnitList().get(0);

        assertNull("No Revere, no auto-equip.",
                   colonist.getAutomaticRole());

        // Add Revere to congress
        player.addFather(paulRevere);

        assertNull("No muskets, no auto-equip",
                   colonist.getAutomaticRole());

        // Add muskets
        colony.addGoods(musketsType, 100);

        assertEquals("Auto equip to soldier role.", soldierRole,
                     colonist.getAutomaticRole());

        // Add horses, but still expect soldier
        colony.addGoods(horsesType, 100);

        assertEquals("Auto equip to soldier role despite horses.", soldierRole,
                     colonist.getAutomaticRole());
    }

    public void testStuyvesant() {
        Game game = getGame();
        game.setMap(getTestMap(true));

        Colony colony = getStandardColony(4);
        Player player = colony.getOwner();

        // The custom house is not buildable initially
        assertFalse(colony.canBuild(customHouseType));

        // But it should become available after Peter Stuyvesant has
        // joined continental congress
        player.addFather(peterStuyvesant);
        assertTrue(colony.canBuild(customHouseType));
    }
}
