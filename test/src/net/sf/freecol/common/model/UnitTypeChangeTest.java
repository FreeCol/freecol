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

import net.sf.freecol.util.test.FreeColTestCase;

public class UnitTypeChangeTest extends FreeColTestCase {


    public void testEmptyScope() {
    	
        UnitTypeChange change = new UnitTypeChange();

        assertTrue(change.getScopes().isEmpty());

        // empty scope applies to all players
        for (Player player : getStandardGame().getPlayers()) {
            assertTrue(change.appliesTo(player));
        }

    }

    public void testAbilityScope() {

        Game game = getStandardGame();
        Player dutch = game.getPlayer("model.nation.dutch");
        Player inca = game.getPlayer("model.nation.inca");

        UnitType gardener = new UnitType("gardener", spec());
        UnitType farmer = spec().getUnitType("model.unit.expertFarmer");

        Scope scope = new Scope();
        scope.setAbilityID("model.ability.native");

        UnitTypeChange.ChangeType education = UnitTypeChange.ChangeType.EDUCATION;
        UnitTypeChange change = new UnitTypeChange();
        change.setNewUnitType(farmer);
        change.getChangeTypes().add(education);
        change.getScopes().add(scope);

        gardener.getTypeChanges().add(change);

        assertTrue(gardener.canBeUpgraded(farmer, education));
        assertEquals(null, gardener.getUnitTypeChange(education, dutch));
        assertEquals(farmer, gardener.getUnitTypeChange(education, inca));
        assertFalse(change.appliesTo(dutch));
        assertTrue(change.appliesTo(inca));

        scope.setMatchNegated(true);
        assertTrue(change.appliesTo(dutch));
        assertFalse(change.appliesTo(inca));
        assertEquals(farmer, gardener.getUnitTypeChange(education, dutch));
        assertEquals(null, gardener.getUnitTypeChange(education, inca));

    }

    public void testCreation() {

        Game game = getStandardGame();
        Player dutch = game.getPlayer("model.nation.dutch");

        UnitType gardener = new UnitType("gardener", spec());
        UnitType farmer = spec().getUnitType("model.unit.expertFarmer");

        UnitTypeChange.ChangeType creation = UnitTypeChange.ChangeType.CREATION;
        UnitTypeChange change = new UnitTypeChange();
        change.setNewUnitType(farmer);
        change.getChangeTypes().add(creation);

        gardener.getTypeChanges().add(change);

        assertTrue(gardener.canBeUpgraded(farmer, creation));
        assertTrue(change.appliesTo(dutch));
        assertEquals(farmer, gardener.getUnitTypeChange(creation, dutch));

        Unit gardenerUnit = new Unit(game, dutch, gardener);
        assertEquals(farmer, gardenerUnit.getType());

    }

    public void testEnterColony() {

        Game game = getStandardGame();
    	game.setMap(getTestMap(true));
        Player dutch = game.getPlayer("model.nation.dutch");
        Colony colony = getStandardColony();

        UnitType gardener = new UnitType("gardener", spec());
        gardener.setSkill(0);
        UnitType farmer = spec().getUnitType("model.unit.expertFarmer");

        UnitTypeChange.ChangeType enterColony = UnitTypeChange.ChangeType.ENTER_COLONY;
        UnitTypeChange change = new UnitTypeChange();
        change.setNewUnitType(farmer);
        change.getChangeTypes().add(enterColony);

        gardener.getTypeChanges().add(change);

        assertTrue(gardener.canBeUpgraded(farmer, enterColony));
        assertTrue(change.appliesTo(dutch));
        assertEquals(farmer, gardener.getUnitTypeChange(enterColony, dutch));

        Unit gardenerUnit = new Unit(game, dutch, gardener);
        assertEquals(gardener, gardenerUnit.getType());
        assertEquals(farmer, gardenerUnit.getType().getUnitTypeChange(enterColony, dutch));
        assertNotNull(colony.getVacantWorkLocationFor(gardenerUnit));

        gardenerUnit.setLocation(colony.getVacantWorkLocationFor(gardenerUnit));
        assertEquals(farmer, gardenerUnit.getType());

    }

}
