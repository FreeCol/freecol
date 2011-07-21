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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.freecol.common.model.FoundingFather.FoundingFatherType;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.server.model.ServerUnit;
import net.sf.freecol.util.test.FreeColTestCase;

/**
 * Test cases for individual Founding Fathers. TODO: add a test case
 * for each Founding Father.
 */
public class IndividualFatherTest extends FreeColTestCase {

    private static GoodsType musketsType
        = spec().getGoodsType("model.goods.muskets");

    private static UnitType servantType
        = spec().getUnitType("model.unit.indenturedServant");
    private static UnitType colonistType
        = spec().getUnitType("model.unit.freeColonist");
    private static UnitType statesmanType
        = spec().getUnitType("model.unit.elderStatesman");


    public void testPeterStuyvesant() {
    	Game game = getGame();
    	game.setMap(getTestMap(true));

        Colony colony = getStandardColony(4);
        Player player = colony.getOwner();

        // The custom house is not buildable initially
        BuildableType customHouse = spec().getBuildingType("model.building.customHouse");
        assertFalse(colony.canBuild(customHouse));

        // But it should become available after Peter Stuyvesant has joined continental congress
        FoundingFather father = spec().getFoundingFather("model.foundingFather.peterStuyvesant");
        player.addFather(father);
        assertTrue(colony.canBuild(customHouse));
    }

    public void testHernanCortes() {
    	Game game = getGame();
    	game.setMap(getTestMap(true));

        Colony colony = getStandardColony(4);
        Player player = colony.getOwner();
        Unit unit = colony.getUnitList().get(0);

        NationType inca = spec().getNationType("model.nationType.inca");
        SettlementType incaCity = inca.getSettlementType("model.settlement.inca");

        RandomRange range = incaCity.getPlunderRange(unit);
        assertEquals(2100, range.getFactor());

        FoundingFather father = spec().getFoundingFather("model.foundingFather.hernanCortes");
        player.addFather(father);

        range = incaCity.getPlunderRange(unit);
        assertEquals(3100, range.getFactor());

    }

    public void testMinuit() {
        Game game = getGame();
        game.setMap(getTestMap(true));

        Colony colony = getStandardColony();
        Unit unit = colony.getUnitList().get(0);
        Player player = colony.getOwner();
        Player iroquois = getGame().getPlayer("model.nation.iroquois");
        Tile colonyCenterTile = colony.getTile();
        Tile disputedTile = colonyCenterTile.getNeighbourOrNull(Direction.N);
        Tile settlementTile = disputedTile.getNeighbourOrNull(Direction.N);
        assertNull(settlementTile.getOwner());

        FreeColTestCase.IndianSettlementBuilder builder = new FreeColTestCase.IndianSettlementBuilder(getGame());
        IndianSettlement indianSettlement = builder.player(iroquois).settlementTile(settlementTile)
            .skillToTeach(null).build();

        disputedTile.setOwner(iroquois);
        disputedTile.setOwningSettlement(indianSettlement);

        assertNotNull(settlementTile.getSettlement());
        assertTrue(player.getLandPrice(disputedTile) > 0);
        assertFalse(colony.getColonyTile(disputedTile).canAdd(unit));

        FoundingFather minuit
            = spec().getFoundingFather("model.foundingFather.peterMinuit");
        player.addFather(minuit);

        assertEquals(0, player.getLandPrice(disputedTile));
        assertTrue(colony.getColonyTile(disputedTile).canAdd(unit));
    }

    public void testPaine() {
    	Game game = getGame();
    	game.setMap(getTestMap(true));

        Colony colony = getStandardColony(4);
        Player player = colony.getOwner();
        GoodsType bells = spec().getGoodsType("model.goods.bells");
        Building townHall = colony.getBuilding(spec().getBuildingType("model.building.townHall"));
        List<AbstractGoods> empty = new ArrayList<AbstractGoods>();

        Unit statesman1 = colony.getUnitList().get(0);
        Unit statesman2 = colony.getUnitList().get(1);
        Unit statesman3 = colony.getUnitList().get(2);

        statesman1.setType(spec().getUnitType("model.unit.elderStatesman"));
        statesman1.setLocation(townHall);
        assertEquals(6 + 1, townHall.getProductionOf(bells));

        statesman2.setType(spec().getUnitType("model.unit.elderStatesman"));
        statesman2.setLocation(townHall);
        assertEquals(2 * 6 + 1, townHall.getProductionOf(bells));

        statesman3.setType(spec().getUnitType("model.unit.elderStatesman"));
        statesman3.setLocation(townHall);
        assertEquals(3 * 6 + 1, townHall.getProductionOf(bells));

        player.setTax(20);
        assertEquals(3 * 6 + 1, townHall.getProductionOf(bells));

        FoundingFather paine = spec().getFoundingFather("model.foundingFather.thomasPaine");
        player.addFather(paine);
        player.recalculateBellsBonus();

        assertTrue(player.hasAbility("model.ability.addTaxToBells"));
        Set<Modifier> modifierSet = player.getFeatureContainer().getModifierSet("model.goods.bells");
        assertEquals(1, modifierSet.size());

        Modifier paineModifier = modifierSet.iterator().next();
        assertEquals(paine, paineModifier.getSource());
        assertEquals(player.getTax(), (int) paineModifier.getValue());

        int expected = (int) (3 * 6 * 1.2f + 1);
        assertEquals(expected, townHall.getProductionOf(bells));

        player.setTax(30);
        player.recalculateBellsBonus();

        expected = (int) (3 * 6 * 1.3f + 1);
        assertEquals(expected, townHall.getProductionOf(bells));
    }

    public void testRevere() {
        Game game = getGame();
        game.setMap(getTestMap());

        Colony colony = getStandardColony();
        Player player = colony.getOwner();
        Unit colonist = colony.getUnitList().get(0);

        String errMsg = "Unit should not be able to automatically arm, Revere not in congress yet";
        assertTrue(errMsg, colonist.getAutomaticEquipment() == null);

        // adding Revere to congress
        FoundingFather father = spec().getFoundingFather("model.foundingFather.paulRevere");
        player.addFather(father);

        errMsg = "Unit should not be able to automatically arm, no muskets available";
        assertTrue(errMsg, colonist.getAutomaticEquipment() == null);

        colony.addGoods(musketsType, 100);

        errMsg = "Unit be able to automatically arm, has muskets and Paul Revere";
        assertFalse(errMsg, colonist.getAutomaticEquipment() == null);
    }

    public void testDeWitt() {
        Game game = getGame();
        Player dutch = game.getPlayer("model.nation.dutch");
        Player french = game.getPlayer("model.nation.french");
        dutch.getMarket().setArrears(musketsType, 1);

        assertFalse(dutch.canTrade(musketsType, Market.Access.EUROPE));
        assertFalse(dutch.canTrade(musketsType, Market.Access.CUSTOM_HOUSE));

        FoundingFather father = spec().getFoundingFather("model.foundingFather.janDeWitt");
        dutch.addFather(father);

        assertFalse(dutch.canTrade(musketsType, Market.Access.EUROPE));
        assertFalse(dutch.canTrade(musketsType, Market.Access.CUSTOM_HOUSE));

        dutch.setStance(french, Player.Stance.WAR);

        assertFalse(dutch.canTrade(musketsType, Market.Access.EUROPE));
        assertFalse(dutch.canTrade(musketsType, Market.Access.CUSTOM_HOUSE));

        dutch.setStance(french, Player.Stance.PEACE);

        assertFalse(dutch.canTrade(musketsType, Market.Access.EUROPE));
        assertTrue(dutch.canTrade(musketsType, Market.Access.CUSTOM_HOUSE));
    }

    public void testBrebeuf() {

    	Game game = getGame();
        Player dutch = game.getPlayer("model.nation.dutch");
        FoundingFather brebeuf = spec().getFoundingFather("model.foundingFather.fatherJeanDeBrebeuf");

        UnitType colonist = spec().getUnitType("model.unit.freeColonist");
        String ability = "model.ability.expertMissionary";

        assertTrue(brebeuf.hasAbility(ability));
        assertFalse(dutch.hasAbility(ability));

        game.setMap(getTestMap());
        FreeColTestCase.IndianSettlementBuilder builder = new FreeColTestCase.IndianSettlementBuilder(game);
        IndianSettlement camp = builder.build();
        EquipmentType bible = spec().getEquipmentType("model.equipment.missionary");

        Unit missionary = new ServerUnit(game, null, dutch, colonist,
                                   UnitState.ACTIVE, bible);
        camp.setMissionary(missionary);

        assertTrue(bible.hasAbility("model.ability.missionary"));
        assertTrue(missionary.hasAbility("model.ability.missionary"));

        dutch.addFather(brebeuf);
        assertTrue(dutch.hasAbility(ability));
        assertTrue(missionary.hasAbility(ability));

    }

    public void testBrewster() {

    	Game game = getGame();
        Player dutch = game.getPlayer("model.nation.dutch");

        String ability = "model.ability.canRecruitUnit";
        assertTrue(dutch.hasAbility(ability));

        for (UnitType unitType : spec().getUnitTypeList()) {
            if (unitType.isRecruitable()) {
                assertTrue("Unable to recruit " + unitType.toString(),
                           dutch.getFeatureContainer().hasAbility(ability, unitType));
            }
        }

        dutch.addFather(spec().getFoundingFather("model.foundingFather.williamBrewster"));
        // ability is no longer general, but limited to certain unit types
        assertFalse(dutch.hasAbility(ability));

        for (UnitType unitType : spec().getUnitTypeList()) {
            if (unitType.isRecruitable()) {
                if (unitType.getSkill() < 0) {
                    assertFalse("Able to recruit " + unitType.toString(),
                                dutch.getFeatureContainer().hasAbility(ability, unitType));
                } else {
                    assertTrue("Unable to recruit " + unitType.toString(),
                               dutch.getFeatureContainer().hasAbility(ability, unitType));
                }
            }
        }

    }

    public void testJefferson() {
    	Game game = getGame();
    	game.setMap(getTestMap(true));

        FoundingFather jefferson = spec().getFoundingFather("model.foundingFather.thomasJefferson");
        assertEquals(1, jefferson.getModifierSet("model.goods.bells").size());
        Modifier modifier = jefferson.getModifierSet("model.goods.bells").iterator().next();
        BuildingType townHallType = spec().getBuildingType("model.building.townHall");
        assertTrue(modifier.appliesTo(townHallType));

        Colony colony = getStandardColony(4);
        Player player = colony.getOwner();
        Building townHall = colony.getBuilding(townHallType);
        townHall.add(colony.getUnitList().get(0));

        assertEquals(0, player.getModifierSet("model.goods.bells").size());
        assertEquals(1, colony.getModifierSet("model.goods.bells").size());
        assertEquals(4, townHall.getProduction());

        player.addFather(jefferson);
        assertEquals(1, player.getModifierSet("model.goods.bells").size());
        assertEquals(2, colony.getModifierSet("model.goods.bells").size());
        assertEquals(2, townHall.getProductionModifiers().size());
        assertEquals(5, townHall.getProduction());

    }

    public void lasCasas() {
    	Game game = getGame();
    	game.setMap(getTestMap(true));

        FoundingFather lasCasas = spec().getFoundingFather("model.foundingFather.bartolomeDeLasCasas");
        Map<UnitType, UnitType> upgrades = lasCasas.getUpgrades();

        assertFalse(upgrades.isEmpty());

        for (Map.Entry<UnitType, UnitType> entry : upgrades.entrySet()) {
            assertEquals(entry.getKey(), spec().getUnitType(entry.getKey().getId()));
            assertEquals(entry.getValue(), spec().getUnitType(entry.getValue().getId()));
        }

        Colony colony = getStandardColony(4);
        Player player = colony.getOwner();
        Unit unit = colony.getUnitList().get(0);

        Map.Entry<UnitType, UnitType> entry = upgrades.entrySet().iterator().next();
        unit.setType(entry.getKey());

        player.addFather(lasCasas);
        assertEquals(unit.getType(), entry.getValue());

    }


}
