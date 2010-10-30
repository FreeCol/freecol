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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.server.model.ServerUnit;
import net.sf.freecol.util.test.FreeColTestCase;


public class FoundingFatherTest extends FreeColTestCase {

    private static GoodsType musketsType
        = spec().getGoodsType("model.goods.muskets");

    private static UnitType servantType
        = spec().getUnitType("model.unit.indenturedServant");
    private static UnitType colonistType
        = spec().getUnitType("model.unit.freeColonist");
    private static UnitType statesmanType
        = spec().getUnitType("model.unit.elderStatesman");


    public void testFeatures() {
        Game game = getStandardGame();
        Player dutch = game.getPlayer("model.nation.dutch");

        FoundingFather father1 = new FoundingFather("father1", spec());
        Ability ability = new Ability("some.new.ability");
        spec().addAbility(ability);
        father1.addAbility(ability);
        dutch.addFather(father1);

        assertTrue(dutch.hasAbility("some.new.ability"));

        FoundingFather father2 = new FoundingFather("father2", spec());
        Modifier modifier = new Modifier("some.new.modifier", father2, 2f, Modifier.Type.ADDITIVE);
        father2.addModifier(modifier);
        spec().addModifier(modifier);
        dutch.addFather(father2);

        Set<Modifier> modifierSet = dutch.getFeatureContainer().getModifierSet("some.new.modifier");
        assertEquals(1, modifierSet.size());
        assertEquals(2f, modifierSet.iterator().next().getValue());
        assertEquals(4f, FeatureContainer.applyModifierSet(2, null, modifierSet));

        FoundingFather father3 = new FoundingFather("father3", spec());
        father3.addModifier(new Modifier("some.new.modifier", father3, 2f, Modifier.Type.ADDITIVE));
        dutch.addFather(father3);

        assertFalse(dutch.getFeatureContainer().getModifierSet("some.new.modifier").isEmpty());
        assertEquals(6f, dutch.getFeatureContainer().applyModifier(2, "some.new.modifier"));

        FoundingFather father4 = new FoundingFather("father4", spec());
        Ability ability2 = new Ability("some.new.ability", false);
        assertFalse(ability.equals(ability2));
        assertFalse(ability.hashCode() == ability2.hashCode());
        father4.addAbility(ability2);
        dutch.addFather(father4);

        assertFalse(dutch.getFeatureContainer().hasAbility("some.new.ability"));

    }

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

    public void testAddAllFathers() {
        // check that all fathers can be added
    	Game game = getGame();
    	game.setMap(getTestMap(true));
    	
        Colony colony = getStandardColony(4);
        Player player = colony.getOwner();
        
        for (FoundingFather father : spec().getFoundingFathers()) {
            player.addFather(father);
        }
    }

    public void testMinuit() {
        Game game = getGame();
        game.setMap(getTestMap(true));
    	
        Colony colony = getStandardColony();
        Unit unit = colony.getRandomUnit();
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

        assertTrue(player.hasAbility("model.ability.addTaxToBells"));
        assertFalse(player.getFeatureContainer().getModifierSet("model.goods.bells").isEmpty());
                   
        player.setTax(30);
        // TODO: find out why the following changes anything
        colony.getModifierSet("model.goods.bells");

        int expected = (int) (3 * 6 * 1.3f + 1);
        assertEquals(expected, townHall.getProductionOf(bells));
    }
    
    public void testRevere() {
        Game game = getGame();
        game.setMap(getTestMap());
        
        Colony colony = getStandardColony();
        Player player = colony.getOwner();
        Unit colonist = colony.getRandomUnit();
           
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


    public void testBellsRequired() {

        int[] expectedValues = new int[] {
            40, 201, 442, 763, 1164, 1645 , 2206, 2847, 3568, 4369,
            5250, 6211, 7252, 8373, 9574, 10855, 12216, 13657, 15178,
            16779, 18460, 20221, 22062, 23983, 25984
        };

    	Game game = getGame();
        Player dutch = game.getPlayer("model.nation.dutch");

        //assertEquals(2, getGame().getGameOptions().getInteger(GameOptions.DIFFICULTY));
        assertEquals(40, spec().getIntegerOption("model.option.foundingFatherFactor").getValue());

        for (int index = 0; index < expectedValues.length; index++) {
            assertEquals(index, dutch.getFatherCount());
            assertEquals(expectedValues[index], dutch.getTotalFoundingFatherCost());
            FoundingFather father = new FoundingFather("father" + index, spec());
            dutch.addFather(father);
        }

    }

    public void testAvailableTo() {
        // this feature is not used at the moment
    	Game game = getGame();
        for (FoundingFather father : spec().getFoundingFathers()) {
            for (Player player : game.getPlayers()) {
                assertEquals(player.getNationID(), player.isEuropean(), father.isAvailableTo(player));
            }
        }

        Player dutch = game.getPlayer("model.nation.dutch");
        Player french = game.getPlayer("model.nation.french");
        FoundingFather newFather = new FoundingFather("father", spec());

        Scope dutchScope = new Scope();
        dutchScope.setMethodName("getNationID");
        dutchScope.setMethodValue("model.nation.dutch");
        assertTrue(dutchScope.appliesTo(dutch));
        newFather.getScopes().add(dutchScope);

        Scope frenchScope = new Scope();
        frenchScope.setMethodName("getNationType");
        frenchScope.setMethodValue("model.nationType.cooperation");
        assertTrue(frenchScope.appliesTo(french));
        newFather.getScopes().add(frenchScope);

        for (Player player : game.getPlayers()) {
            assertEquals(player.getNationID(), (player == french || player == dutch),
                         newFather.isAvailableTo(player));
        }


    }


}
