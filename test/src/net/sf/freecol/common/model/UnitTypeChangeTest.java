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

import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.server.model.ServerUnit;
import net.sf.freecol.util.test.FreeColTestCase;


public class UnitTypeChangeTest extends FreeColTestCase {

    private static final UnitType farmer
        = spec().getUnitType("model.unit.expertFarmer");


    public void testEmptyScope() {
        UnitTypeChange change = new UnitTypeChange();

        assertTrue("A new change has empty scopes",
                   change.getScopes().isEmpty());

        // empty scope applies to all players
        for (Player player : getStandardGame().getPlayers()) {
            assertTrue("Empty scopes apply to all players",
                       change.appliesTo(player));
        }
    }

    public void testAbilityScope() {
        Game game = getStandardGame();
        Player dutch = game.getPlayerByNationId("model.nation.dutch");
        Player inca = game.getPlayerByNationId("model.nation.inca");

        UnitType gardener = new UnitType("gardener", spec());

        Scope scope = new Scope();
        scope.setAbilityId(Ability.NATIVE);

        UnitTypeChange.ChangeType education = UnitTypeChange.ChangeType.EDUCATION;
        UnitTypeChange change = new UnitTypeChange();
        change.setNewUnitType(farmer);
        change.getChangeTypes().put(education, 100);
        List<Scope> scopes = new ArrayList<>(change.getScopes());
        scopes.add(scope);
        change.setScopes(scopes);

        List<UnitTypeChange> ch = new ArrayList<>(gardener.getTypeChanges());
        ch.add(change);
        gardener.setTypeChanges(ch);

        assertTrue(gardener.canBeUpgraded(farmer, education));
        assertEquals(null, gardener.getUnitTypeChange(education, dutch));
        assertEquals(farmer, gardener.getTargetType(education, inca));
        assertFalse(change.appliesTo(dutch));
        assertTrue(change.appliesTo(inca));

        scope.setMatchNegated(true);
        assertTrue(change.appliesTo(dutch));
        assertFalse(change.appliesTo(inca));
        assertEquals(farmer, gardener.getTargetType(education, dutch));
        assertEquals(null, gardener.getTargetType(education, inca));

    }

    public void testCreation() {
        Game game = getStandardGame();
        Player dutch = game.getPlayerByNationId("model.nation.dutch");

        UnitType gardener = new UnitType("gardener", spec());

        UnitTypeChange.ChangeType creation = UnitTypeChange.ChangeType.CREATION;
        UnitTypeChange change = new UnitTypeChange();
        change.setNewUnitType(farmer);
        change.getChangeTypes().put(creation, 100);

        List<UnitTypeChange> ch = new ArrayList<>(gardener.getTypeChanges());
        ch.add(change);
        gardener.setTypeChanges(ch);

        assertTrue(gardener.canBeUpgraded(farmer, creation));
        assertTrue(change.appliesTo(dutch));
        assertEquals(farmer, gardener.getTargetType(creation, dutch));

        Unit gardenerUnit = new ServerUnit(game, null, dutch, gardener);
        assertEquals(farmer, gardenerUnit.getType());

    }

    public void testEquality() {
        for (UnitType unitType : spec().getUnitTypeList()) {
            for (UnitTypeChange change : unitType.getTypeChanges()) {
                UnitType newUnitType = change.getNewUnitType();
                assertTrue(newUnitType == spec().getUnitType(newUnitType.getId()));
            }
        }
    }
}
