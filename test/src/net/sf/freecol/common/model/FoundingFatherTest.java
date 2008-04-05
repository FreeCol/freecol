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
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.util.test.FreeColTestCase;

public class FoundingFatherTest extends FreeColTestCase {

    private static UnitType servantType = spec().getUnitType("model.unit.indenturedServant");
    private static UnitType colonistType = spec().getUnitType("model.unit.freeColonist");
    private static UnitType statesmanType = spec().getUnitType("model.unit.elderStatesman");


    public void testFeatures() {

        Game game = getStandardGame();
        Player dutch = game.getPlayer("model.nation.dutch");

        FoundingFather father1 = new FoundingFather(111);
        spec().getAbilityKeys().add("some.new.ability");
        father1.addAbility(new Ability("some.new.ability"));
        dutch.addFather(father1);

        assertTrue(dutch.hasAbility("some.new.ability"));

        FoundingFather father2 = new FoundingFather(112);
        spec().getModifierKeys().add("some.new.modifier");
        father2.addModifier(new Modifier("some.new.modifier", 2f, Modifier.Type.ADDITIVE));
        dutch.addFather(father2);

        Set<Modifier> modifierSet = dutch.getFeatureContainer().getModifierSet("some.new.modifier");
        assertEquals(1, modifierSet.size());
        assertEquals(2f, modifierSet.iterator().next().getValue());
        assertEquals(4f, FeatureContainer.applyModifierSet(2, null, modifierSet));

        FoundingFather father3 = new FoundingFather(113);
        father3.addModifier(new Modifier("some.new.modifier", 2f, Modifier.Type.ADDITIVE));
        dutch.addFather(father3);

        assertFalse(dutch.getFeatureContainer().getModifierSet("some.new.modifier").isEmpty());
        assertEquals(6f, dutch.getFeatureContainer().applyModifier(2, "some.new.modifier"));

        FoundingFather father4 = new FoundingFather(114);
        father4.addAbility(new Ability("some.new.ability", false));
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
        FoundingFather father = new FoundingFather(111);
        father.setUnits(units);

        /** this doesn't work because we haven't got a real model controller
        assertEquals(2, dutch.getUnits().size());
        assertEquals(colonistType, dutch.getUnits().get(0).getType());
        assertEquals(statesmanType, dutch.getUnits().get(1).getType());
        */

    }

    public void testUpgrades() {

        Colony colony = getStandardColony(4);
        colony.getUnitList().get(0).setType(colonistType);
        colony.getUnitList().get(1).setType(colonistType);
        colony.getUnitList().get(2).setType(colonistType);
        colony.getUnitList().get(3).setType(servantType);
        
        FoundingFather father = new FoundingFather(111);
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

    public void testBuildingEvent() {

        BuildingType press = spec().getBuildingType("model.building.PrintingPress");

        Colony colony = getStandardColony(4);
        assertEquals(null, colony.getBuilding(press));

        FoundingFather father = new FoundingFather(111);
        Map<String, String> events = new HashMap<String, String>();
        events.put("model.event.freeBuilding", "model.building.PrintingPress");
        father.setEvents(events);
        colony.getOwner().addFather(father);

        assertTrue(colony.getBuilding(press) != null);

    }

    public void testBuildingBonus() {

        BuildingType press = spec().getBuildingType("model.building.PrintingPress");

        Game game = getStandardGame();
        Player dutch = game.getPlayer("model.nation.dutch");

        FoundingFather father = new FoundingFather(111);
        Modifier priceBonus = new Modifier("model.modifier.buildingPriceBonus", -100f, Modifier.Type.PERCENTAGE);
        Scope pressScope = new Scope();
        pressScope.setType("model.building.PrintingPress");
        List<Scope> scopeList = new ArrayList<Scope>();
        scopeList.add(pressScope);
        priceBonus.setScopes(scopeList);
        father.addModifier(priceBonus);
        dutch.addFather(father);

        Colony colony = getStandardColony(4);

        assertTrue(colony.getBuilding(press) != null);

    }
    
    public void testPeterStuyvesant() {
        
        Colony colony = getStandardColony(4);
        Player player = colony.getOwner();
        
        // The custom house is not buildable initially
        BuildableType customHouse = spec().getBuildingType("model.building.CustomHouse");
        assertFalse(colony.canBuild(customHouse));
        
        // But it should become available after Peter Stuyvesant has joined continental congress
        FoundingFather father = spec().getFoundingFather("model.foundingFather.peterStuyvesant");
        player.addFather(father);
        assertTrue(colony.canBuild(customHouse));
    }

    public void testAddAllFathers() {
        // check that all fathers can be added
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
        
        Colony colony = getStandardColony(2);
        Player player = colony.getOwner();
        assertEquals(2, colony.getUnitCount());
        
        // the colony has no stockade initially
        BuildingType stockadeType = spec().getBuildingType("model.building.Stockade");
        Building b = colony.getBuilding(stockadeType);
        assertNull(b);
        
        // adding LaSalle should have no effect when population is 2
        FoundingFather father = spec().getFoundingFather("model.foundingFather.laSalle");
        player.addFather(father);
        b = colony.getBuilding(stockadeType);
        assertNull(b);
        
        // increasing population to 3 should give access to stockade
        UnitType pioneerType = FreeCol.getSpecification().getUnitType("model.unit.hardyPioneer");
        Unit unit = new Unit(getGame(), colony.getTile(), player, pioneerType, UnitState.ACTIVE, pioneerType.getDefaultEquipment());
        // set the unit as a farmer in the colony
        unit.setWorkType(Goods.FOOD);
        ColonyTile farmLand = colony.getVacantColonyTileFor(unit, Goods.FOOD);
        unit.setLocation(farmLand);
        b = colony.getBuilding(stockadeType);
        assertNotNull(b);
     }

}
