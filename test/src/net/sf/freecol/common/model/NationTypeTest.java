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

import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.option.OptionGroup;
import net.sf.freecol.util.test.FreeColTestCase;


public class NationTypeTest extends FreeColTestCase {

    public void testHasType(){

        Specification s = spec();

        // Make sure that each nation has a type
        for (Nation n : s.getNations()){
            assertNotNull(n.toString(), n.getType());
        }
    }

    public void testisRef() {

        Specification s = spec();

        assertTrue(s.getNationType("model.nationType.trade").isEuropean());
        assertTrue(s.getNationType("model.nationType.default").isEuropean());
        assertTrue(s.getNationType("model.nationType.ref").isEuropean());
        assertTrue(s.getNationType("model.nationType.cooperation").isEuropean());
        assertTrue(s.getNationType("model.nationType.immigration").isEuropean());
        assertTrue(s.getNationType("model.nationType.conquest").isEuropean());

        assertFalse(s.getNationType("model.nationType.apache").isEuropean());
        assertFalse(s.getNationType("model.nationType.sioux").isEuropean());
        assertFalse(s.getNationType("model.nationType.tupi").isEuropean());
        assertFalse(s.getNationType("model.nationType.arawak").isEuropean());
        assertFalse(s.getNationType("model.nationType.cherokee").isEuropean());
        assertFalse(s.getNationType("model.nationType.iroquois").isEuropean());
        assertFalse(s.getNationType("model.nationType.inca").isEuropean());
        assertFalse(s.getNationType("model.nationType.aztec").isEuropean());
    }

    public void testIsREF() {
        Specification s = spec();

        assertFalse(s.getNationType("model.nationType.trade").isREF());
        assertFalse(s.getNationType("model.nationType.default").isREF());
        assertTrue(s.getNationType("model.nationType.ref").isREF());
        assertFalse(s.getNationType("model.nationType.cooperation").isREF());
        assertFalse(s.getNationType("model.nationType.immigration").isREF());
        assertFalse(s.getNationType("model.nationType.conquest").isREF());

        assertFalse(s.getNationType("model.nationType.apache").isREF());
        assertFalse(s.getNationType("model.nationType.sioux").isREF());
        assertFalse(s.getNationType("model.nationType.tupi").isREF());
        assertFalse(s.getNationType("model.nationType.arawak").isREF());
        assertFalse(s.getNationType("model.nationType.cherokee").isREF());
        assertFalse(s.getNationType("model.nationType.iroquois").isREF());
        assertFalse(s.getNationType("model.nationType.inca").isREF());
        assertFalse(s.getNationType("model.nationType.aztec").isREF());
    }

    public void testSettlementType() {
        for (NationType nationType : spec().getIndianNationTypes()) {
            assertFalse(nationType.getSettlementTypes().isEmpty());
            SettlementType settlement = nationType.getSettlementType(false);
            SettlementType capital = nationType.getSettlementType(true);
            assertNotNull(nationType.getId(), settlement);
            assertNotNull(nationType.getId(), capital);
            assertFalse(nationType.getId(), capital == settlement);
        }
    }

    public void testStartingUnits() {
        int difficulty = 0;
        for (OptionGroup og : spec().getDifficultyLevels()) {
            spec().applyDifficultyLevel(og);
            for (EuropeanNationType type : spec().getEuropeanNationTypes()) {
                List<AbstractUnit> startingUnits = type.getStartingUnits();
                assertEquals("Wrong number of starting units: " + type.toString(),
                             3, startingUnits.size());
                for (AbstractUnit au : startingUnits) {
                    String unitTypeId = au.getId();
                    if ("model.role.soldier".equals(au.getRoleId())) {
                        if (difficulty == 0 || difficulty == 1
                            || "model.nationType.conquest".equals(type.getId())) {
                            assertEquals("Wrong type of soldier: " + type.toString(),
                                         "model.unit.veteranSoldier", unitTypeId);
                        } else {
                            assertFalse("Wrong type of soldier: " + type.toString(),
                                        "model.unit.veteranSoldier".equals(unitTypeId));
                        }
                    } else if ("model.role.pioneer".equals(au.getRoleId())) {
                        if ("model.nationType.cooperation".equals(type.getId())) {
                            assertEquals("Wrong type of pioneer: " + type.toString(),
                                         "model.unit.hardyPioneer", unitTypeId);
                        } else {
                            assertFalse("Wrong type of pioneer: " + type.toString(),
                                        "model.unit.hardyPioneer".equals(unitTypeId));
                        }
                    } else if (Role.isDefaultRoleId(au.getRoleId())) {
                        assertTrue("Ship is not naval: " + type.toString(),
                                   spec().getUnitType(au.getId()).hasAbility(Ability.NAVAL_UNIT));
                        if ("model.nationType.trade".equals(type.getId())
                            || "model.nationType.naval".equals(type.getId())) {
                            assertEquals("Wrong type of ship: " + type.toString(),
                                         "model.unit.merchantman", unitTypeId);
                        } else {
                            assertEquals("Wrong type of ship: " + type.toString(),
                                         "model.unit.caravel", unitTypeId);
                        }
                    }
                }
            }
            difficulty++;
        }
    }

    public void testAdvantagesThatOnlyApplyToPersons() {
        Game game = getGame();
        game.setMap(getTestMap(true));

        Colony colony = getStandardColony();
        colony.getOwner().changeNationType(spec().getNationType("model.nationType.building"));

        List<Modifier> modifiers = new ArrayList<>(colony.getOwner().getModifiers("model.goods.hammers"));
        assertEquals(1, modifiers.size());

        BuildingType carpenterHouse = spec().getBuildingType("model.building.carpenterHouse");
        assertFalse("Hammer production bonus should not apply to buildings",
                    modifiers.get(0).appliesTo(carpenterHouse));
        for (Unit unit : colony.getUnitList()) {
            assertTrue("Hammer production bonus should apply to all persons",
                       modifiers.get(0).appliesTo(unit.getType()));
        }

        colony.getOwner().changeNationType(spec().getNationType("model.nationType.furTrapping"));

        modifiers = new ArrayList<>(colony.getOwner().getModifiers("model.goods.coats"));
        assertEquals(1, modifiers.size());

        BuildingType traderHouse = spec().getBuildingType("model.building.furTraderHouse");
        assertFalse("Coat production bonus should not apply to buildings",
                    modifiers.get(0).appliesTo(traderHouse));
        for (Unit unit : colony.getUnitList()) {
            assertTrue("Coat production bonus should apply to all persons",
                       modifiers.get(0).appliesTo(unit.getType()));
        }


    }


}
