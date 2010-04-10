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
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.util.test.FreeColTestCase;

public class FoundingFatherTest extends FreeColTestCase {

    private static UnitType servantType = spec().getUnitType("model.unit.indenturedServant");
    private static UnitType colonistType = spec().getUnitType("model.unit.freeColonist");
    private static UnitType statesmanType = spec().getUnitType("model.unit.elderStatesman");

    private static GoodsType musketsType = spec().getGoodsType("model.goods.muskets");

    public void testFeatures() {

        Game game = getStandardGame();
        Player dutch = game.getPlayer("model.nation.dutch");

        FoundingFather father1 = new FoundingFather();
        Ability ability = new Ability("some.new.ability");
        spec().addAbility(ability);
        father1.addAbility(ability);
        dutch.addFather(father1);

        assertTrue(dutch.hasAbility("some.new.ability"));

        FoundingFather father2 = new FoundingFather();
        Modifier modifier = new Modifier("some.new.modifier", father2, 2f, Modifier.Type.ADDITIVE);
        father2.addModifier(modifier);
        spec().addModifier(modifier);
        dutch.addFather(father2);

        Set<Modifier> modifierSet = dutch.getFeatureContainer().getModifierSet("some.new.modifier");
        assertEquals(1, modifierSet.size());
        assertEquals(2f, modifierSet.iterator().next().getValue());
        assertEquals(4f, FeatureContainer.applyModifierSet(2, null, modifierSet));

        FoundingFather father3 = new FoundingFather();
        father3.addModifier(new Modifier("some.new.modifier", father3, 2f, Modifier.Type.ADDITIVE));
        dutch.addFather(father3);

        assertFalse(dutch.getFeatureContainer().getModifierSet("some.new.modifier").isEmpty());
        assertEquals(6f, dutch.getFeatureContainer().applyModifier(2, "some.new.modifier"));

        FoundingFather father4 = new FoundingFather();
        Ability ability2 = new Ability("some.new.ability", false);
        assertFalse(ability.equals(ability2));
        assertFalse(ability.hashCode() == ability2.hashCode());
        father4.addAbility(ability2);
        dutch.addFather(father4);

        assertFalse(dutch.getFeatureContainer().hasAbility("some.new.ability"));

    }

    public void testUnits() {

        Game game = getStandardGame();
        Player dutch = game.getPlayer("model.nation.dutch");

        assertTrue(dutch.getUnits().isEmpty());

        List<AbstractUnit> units = new ArrayList<AbstractUnit>();
        units.add(new AbstractUnit(colonistType, Unit.Role.DEFAULT, 1));
        units.add(new AbstractUnit(statesmanType, Unit.Role.DEFAULT, 1));
        FoundingFather father = new FoundingFather();
        father.setUnits(units);

        /** this doesn't work because we haven't got a real model controller
            assertEquals(2, dutch.getUnits().size());
            assertEquals(colonistType, dutch.getUnits().get(0).getType());
            assertEquals(statesmanType, dutch.getUnits().get(1).getType());
        */

    }

    public void testUpgrades() {
    	Game game = getGame();
    	game.setMap(getTestMap(true));

        Colony colony = getStandardColony(4);
        colony.getUnitList().get(0).setType(colonistType);
        colony.getUnitList().get(1).setType(colonistType);
        colony.getUnitList().get(2).setType(colonistType);
        colony.getUnitList().get(3).setType(servantType);
        
        FoundingFather father = new FoundingFather();
        Map<UnitType, UnitType> upgrades = new HashMap<UnitType, UnitType>();
        upgrades.put(servantType, colonistType);
        upgrades.put(colonistType, statesmanType);
        father.setUpgrades(upgrades);
        colony.getOwner().addFather(father);

        assertEquals(statesmanType, colony.getUnitList().get(0).getType());
        assertEquals(statesmanType, colony.getUnitList().get(1).getType());
        assertEquals(statesmanType, colony.getUnitList().get(2).getType());
        assertEquals(colonistType, colony.getUnitList().get(3).getType());

    }

    /*
    public void testBuildingEvent() {
    	Game game = getGame();
    	game.setMap(getTestMap(true));

        BuildingType press = spec().getBuildingType("model.building.printingPress");

        Colony colony = getStandardColony(4);
        assertEquals(null, colony.getBuilding(press));

        FoundingFather father = new FoundingFather();
        Map<String, String> events = new HashMap<String, String>();
        events.put("model.event.freeBuilding", "model.building.printingPress");
        father.setEvents(events);
        colony.getOwner().addFather(father);

        // doesn't work without server anymore
        //assertTrue(colony.getBuilding(press) != null);

    }
    */

    public void testBuildingBonus() {
        BuildingType press = spec().getBuildingType("model.building.printingPress");

    	Game game = getGame();
    	game.setMap(getTestMap(true));
    	
        Player dutch = game.getPlayer("model.nation.dutch");

        FoundingFather father = new FoundingFather();
        Modifier priceBonus = new Modifier("model.modifier.buildingPriceBonus", -100f, Modifier.Type.PERCENTAGE);
        Scope pressScope = new Scope();
        pressScope.setType("model.building.printingPress");
        List<Scope> scopeList = new ArrayList<Scope>();
        scopeList.add(pressScope);
        priceBonus.setScopes(scopeList);
        father.addModifier(priceBonus);
        dutch.addFather(father);

        Colony colony = getStandardColony(4);

        assertTrue(colony.getBuilding(press) != null);

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

    /*    
          public void testPocahontas() {
        
          Colony colony = getStandardColony(4);
          Player player = colony.getOwner();
                
          FoundingFather father = spec().getFoundingFather("model.foundingFather.pocahontas");
          player.addFather(father);
          }
    */
    
    public void testLaSalle() {
    	Game game = getGame();
    	game.setMap(getTestMap(true));
        
        Colony colony = getStandardColony(2);
        Player player = colony.getOwner();
        assertEquals(2, colony.getUnitCount());
        
        // the colony has no stockade initially
        BuildingType stockadeType = spec().getBuildingType("model.building.stockade");
        Building b = colony.getBuilding(stockadeType);
        assertNull(b);
        
        // adding LaSalle should have no effect when population is 2
        FoundingFather father = spec().getFoundingFather("model.foundingFather.laSalle");
        assertEquals("model.building.stockade", father.getEvents().get(0).getValue());
        player.addFather(father);
        b = colony.getBuilding(stockadeType);
        assertNull(b);
        
        // increasing population to 3 should give access to stockade
        UnitType pioneerType = FreeCol.getSpecification().getUnitType("model.unit.hardyPioneer");
        Unit unit = new Unit(getGame(), colony.getTile(), player, pioneerType, UnitState.ACTIVE, 
                             pioneerType.getDefaultEquipment());
        // set the unit as a farmer in the colony
        GoodsType foodType = spec().getGoodsType("model.goods.food");
        unit.setWorkType(foodType);
        ColonyTile farmLand = colony.getVacantColonyTileFor(unit, true, foodType);
        unit.setLocation(farmLand);
        b = colony.getBuilding(stockadeType);
        assertNotNull(b);
    }

    public void testMinuit() {
    	Game game = getGame();
    	game.setMap(getTestMap(true));
    	
        Colony colony = getStandardColony();
        Unit unit = colony.getRandomUnit();
        Player player = colony.getOwner();
        Player iroquois = getGame().getPlayer("model.nation.iroquois");
        Tile colonyCenterTile = colony.getTile();
        Tile disputedTile = getGame().getMap().getNeighbourOrNull(Direction.N, colonyCenterTile);
        Tile settlementTile = getGame().getMap().getNeighbourOrNull(Direction.NE, colonyCenterTile);
        
        FreeColTestCase.IndianSettlementBuilder builder = new FreeColTestCase.IndianSettlementBuilder(getGame());
        IndianSettlement indianSettlement = builder.player(iroquois).settlementTile(settlementTile)
            .skillToTeach(null).build();

        disputedTile.setOwner(iroquois);
        disputedTile.setOwningSettlement(indianSettlement);

        assertNotNull(settlementTile.getSettlement());
        assertTrue(player.getLandPrice(disputedTile) > 0);
        assertFalse(colony.getColonyTile(disputedTile).canAdd(unit));
        assertFalse(colony.getColonyTile(settlementTile).canAdd(unit));

        FoundingFather minuit = spec().getFoundingFather("model.foundingFather.peterMinuit");
        player.addFather(minuit);

        assertEquals(0, player.getLandPrice(disputedTile));
        assertTrue(colony.getColonyTile(disputedTile).canAdd(unit));
        assertFalse(colony.getColonyTile(settlementTile).canAdd(unit));

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
        dutch.setArrears(musketsType);

        assertFalse(dutch.canTrade(musketsType, Market.EUROPE));
        assertFalse(dutch.canTrade(musketsType, Market.CUSTOM_HOUSE));

        FoundingFather father = spec().getFoundingFather("model.foundingFather.janDeWitt");
        dutch.addFather(father);

        assertFalse(dutch.canTrade(musketsType, Market.EUROPE));
        assertFalse(dutch.canTrade(musketsType, Market.CUSTOM_HOUSE));

        dutch.setStance(french, Player.Stance.WAR);

        assertFalse(dutch.canTrade(musketsType, Market.EUROPE));
        assertFalse(dutch.canTrade(musketsType, Market.CUSTOM_HOUSE));

        dutch.setStance(french, Player.Stance.PEACE);

        assertFalse(dutch.canTrade(musketsType, Market.EUROPE));
        assertTrue(dutch.canTrade(musketsType, Market.CUSTOM_HOUSE));

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
            dutch.addFather(new FoundingFather());
        }

    }


}
