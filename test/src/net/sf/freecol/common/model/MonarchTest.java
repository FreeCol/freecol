/**
 * Copyright (C) 2002-2024  The FreeCol Team
 *
 * This file is part of FreeCol.
 *
 * FreeCol is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * FreeCol is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with FreeCol.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.sf.freecol.common.model;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.Monarch.MonarchAction;
import net.sf.freecol.common.option.GameOptions;
import net.sf.freecol.common.util.RandomChoice;
import net.sf.freecol.util.test.FreeColTestCase;

public class MonarchTest extends FreeColTestCase {

    public void testSerialize() {
        Game game = getStandardGame();
        Player dutch = game.getPlayerByNationId("model.nation.dutch");

        try (StringWriter sw = new StringWriter();
             FreeColXMLWriter xw = new FreeColXMLWriter(sw)) {
            dutch.getMonarch().toXML(xw);
        } catch (IOException | XMLStreamException ex) {
            fail("Serialization failed: " + ex.getMessage());
        }
    }

    public void testActionChoices() {
        Game game = getStandardGame();
        game.changeMap(getTestMap());
        Player dutch = game.getPlayerByNationId("model.nation.dutch");

        assertTrue(dutch.getMonarch().getActionChoices().isEmpty());

        createStandardColony();
        game.setTurn(new Turn(100));
        
        dutch.setTax(35);
        List<RandomChoice<MonarchAction>> choices = dutch.getMonarch().getActionChoices();
        
        assertTrue(choicesContain(choices, MonarchAction.RAISE_TAX_ACT));
        assertTrue(choicesContain(choices, MonarchAction.LOWER_TAX_OTHER));
    }

    public void testActionValidityBounds() {
        Game game = getStandardGame();
        Player dutch = game.getPlayerByNationId("model.nation.dutch");
        Monarch monarch = dutch.getMonarch();
        
        int maxTax = spec().getInteger(GameOptions.MAXIMUM_TAX);
        dutch.setTax(maxTax);
        
        assertFalse(monarch.actionIsValid(MonarchAction.RAISE_TAX_ACT));

        dutch.setTax(0);
        assertFalse(monarch.actionIsValid(MonarchAction.LOWER_TAX_WAR));
    }

    public void testWarSupportTrigger() {
        Game game = getStandardGame();
        Player dutch = game.getPlayerByNationId("model.nation.dutch");
        Player french = game.getPlayerByNationId("model.nation.french");
        
        dutch.setStance(french, Stance.WAR);
        
        List<AbstractUnit> support = dutch.getMonarch().getWarSupport(french, new Random(1));
        assertNotNull(support);
    }

    public void testTaxChangeLogic() {
        Player dutch = getStandardGame().getPlayerByNationId("model.nation.dutch");
        Monarch monarch = dutch.getMonarch();
        
        dutch.setTax(20);
        
        int raised = monarch.raiseTax(new Random(1));
        assertTrue("Tax should not decrease when raising", raised >= 20);
        
        dutch.setTax(raised);
        int lowered = monarch.lowerTax(new Random(1));
        
        assertTrue("Tax should not increase when lowering", lowered <= raised);
    }

    public void testMonarchRequiredTypesExist() {
        Specification spec = spec();
        assertNotNull(spec.getRole("model.role.soldier"));
        assertNotNull(spec.getRole("model.role.dragoon"));
        assertTrue(spec.getUnitTypesWithAbility(Ability.NAVAL_UNIT).size() > 0);
    }

    public void testFullSupportChain() {
        Player dutch = getStandardGame().getPlayerByNationId("model.nation.dutch");
        List<AbstractUnit> units = dutch.getMonarch().getSupport(new Random(42), false);
        
        Force f = new Force(spec(), units, null);
        
        if (f.isUnderprovisioned()) {
            f.prepareToBoard(spec().getUnitType("model.unit.manOWar"));
        }
        
        assertFalse(f.isUnderprovisioned());
    }

    public void testDownsizeWithZeroGold() {
        Player dutch = getStandardGame().getPlayerByNationId("model.nation.dutch");
        dutch.setGold(0);

        Force mercs = new Force(spec());
        mercs.add(new AbstractUnit(spec().getUnitType("model.unit.artillery"), 
                                   Specification.DEFAULT_ROLE_ID, 5));

        int result = mercs.downsizeToPrice(dutch, new Random(42));
        assertEquals(-1, result);
    }

    public void testDownsizeToImpossibleLimit() {
        Force force = new Force(spec());
        UnitType regular = spec().getUnitType("model.unit.kingsRegular");
        force.add(new AbstractUnit(regular, Specification.DEFAULT_ROLE_ID, 10));

        force.downsizeToLimit(0.001);

        List<AbstractUnit> units = force.getUnitList();
        assertFalse(units.isEmpty());
        assertEquals(1, units.get(0).getNumber());
    }

    public void testCopyEmptyForce() {
        Force emptyOriginal = new Force(spec());
        Force copy = emptyOriginal.copy();

        assertNotNull(copy);
        assertTrue(copy.isEmpty());
        assertEquals(0, copy.getCapacity());
    }

    public void testRequiredSpecificationAbilities() {
        Specification spec = spec();
        UnitType galley = spec.getUnitType("model.unit.caravel"); 
        assertTrue(galley.hasAbility(Ability.NAVAL_UNIT));
        assertTrue(galley.getSpace() > 0);
    }

    private boolean choicesContain(List<RandomChoice<MonarchAction>> choices, MonarchAction action) {
        return choices.stream().anyMatch(c -> c.getObject() == action);
    }

    public void testShouldAddNavalUnit() {
        Game game = getStandardGame();
        Player dutch = game.getPlayerByNationId("model.nation.dutch");

        Monarch monarch = dutch.getMonarch();
        Force ref = monarch.getExpeditionaryForce();

        ref.clearLandUnits();
        ref.clearNavalUnits();

        UnitType infantry = spec().getUnitType("model.unit.kingsRegular");
        UnitType ship = spec().getUnitType("model.unit.manOWar");

        ref.add(new AbstractUnit(infantry,
            Specification.DEFAULT_ROLE_ID, 20));

        assertTrue(monarch.shouldAddNavalUnit());

        ref.add(new AbstractUnit(ship,
            Specification.DEFAULT_ROLE_ID, 4));

        assertFalse(monarch.shouldAddNavalUnit());
    }

    public void testUpdateInterventionForceMaintainsTransportCapacity() {
        Game game = getStandardGame();
        Player dutch = game.getPlayerByNationId("model.nation.dutch");

        Monarch monarch = dutch.getMonarch();

        game.setTurn(new Turn(500));

        monarch.updateInterventionForce();

        Force ivf = monarch.getInterventionForce();

        assertTrue(
            ivf.getCapacity() >= ivf.getSpaceRequired()
        );
    }

    public void testLoadMercenaryForceAffordable() {
        Game game = getStandardGame();
        Player dutch = game.getPlayerByNationId("model.nation.dutch");

        dutch.setGold(5000);

        List<AbstractUnit> mercs = new java.util.ArrayList<>();

        int price = dutch.getMonarch()
            .loadMercenaryForce(new Random(1), mercs);

        if (price >= 0) {
            assertTrue(price <= dutch.getGold());
            assertFalse(mercs.isEmpty());
        }
    }

    public void testLoadMercenariesAffordable() {
        Game game = getStandardGame();
        Player dutch = game.getPlayerByNationId("model.nation.dutch");

        dutch.setGold(2000);

        List<AbstractUnit> mercs = new java.util.ArrayList<>();

        int price = dutch.getMonarch()
            .loadMercenaries(new Random(1), mercs);

        if (price >= 0) {
            assertTrue(price <= dutch.getGold());
        }
    }

    public void testWarSupportDefenselessEnemy() {
        Game game = getStandardGame();

        Player dutch =
            game.getPlayerByNationId("model.nation.dutch");
        Player french =
            game.getPlayerByNationId("model.nation.french");

        dutch.setStance(french, Stance.WAR);

        List<AbstractUnit> support =
            dutch.getMonarch().getWarSupport(
                french, new Random(1));

        assertNotNull(support);

        if (!support.isEmpty()) {
            int totalUnits = support.stream()
                .mapToInt(AbstractUnit::getNumber)
                .sum();

            assertEquals(
                "Defenseless enemy should trigger minimal war support",
                1,
                totalUnits);
        }
    }

    public void testLoadMercenaryForceDoesNotMutateTemplate() {
        Game game = getStandardGame();
        Player dutch = game.getPlayerByNationId("model.nation.dutch");

        Monarch monarch = dutch.getMonarch();

        int before =
            monarch.getMercenaryForce()
                .getUnitList()
                .stream()
                .mapToInt(AbstractUnit::getNumber)
                .sum();

        monarch.loadMercenaryForce(
            new Random(1),
            new java.util.ArrayList<>());

        int after =
            monarch.getMercenaryForce()
                .getUnitList()
                .stream()
                .mapToInt(AbstractUnit::getNumber)
                .sum();

        assertEquals(before, after);
    }

    public void testLoadMercenaryForceZeroGold() {
        Game game = getStandardGame();
        Player dutch = game.getPlayerByNationId("model.nation.dutch");

        dutch.setGold(0);

        List<AbstractUnit> mercs = new ArrayList<>();

        int price = dutch.getMonarch()
            .loadMercenaryForce(new Random(1), mercs);

        assertEquals(-1, price);
        assertTrue(mercs.isEmpty());
    }
}