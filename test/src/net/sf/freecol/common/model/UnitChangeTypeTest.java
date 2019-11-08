/**
 *  Copyright (C) 2002-2019  The FreeCol Team
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
import net.sf.freecol.common.model.UnitChangeType;
import net.sf.freecol.server.model.ServerUnit;
import net.sf.freecol.util.test.FreeColTestCase;
import static net.sf.freecol.common.util.CollectionUtils.*;


public class UnitChangeTypeTest extends FreeColTestCase {

    private static final UnitType farmer
        = spec().getUnitType("model.unit.expertFarmer");


    public void testEmptyScope() {
        UnitChangeType uct = spec().getUnitChangeType(UnitChangeType.EDUCATION);

        assertEquals("Education has no scopes", 0, count(uct.getScopes()));

        // empty scope applies to all players
        for (Player player : getStandardGame().getPlayerList(alwaysTrue())) {
            assertTrue("Empty scopes apply to all players",
                       uct.appliesTo(player));
        }
    }

    public void testAbilityScope() {
        Game game = getStandardGame();
        Player dutch = game.getPlayerByNationId("model.nation.dutch");
        Player inca = game.getPlayerByNationId("model.nation.inca");

        UnitType gardenerType = new UnitType("gardener", spec());
        gardenerType.setSkill(0);
        
        assertNull(gardenerType.getTeachingType(farmer));

        addUnitTypeChange(UnitChangeType.EDUCATION, gardenerType, farmer,
                          100, -1);

        assertEquals(farmer, gardenerType.getTeachingType(farmer));

        Scope scope = new Scope();
        scope.setAbilityId(Ability.NATIVE);
        spec().getUnitChangeType(UnitChangeType.EDUCATION).addScope(scope);
        
        assertEquals(farmer, gardenerType.getTeachingType(farmer));
        Unit gardenerUnit = new ServerUnit(game, null, dutch, gardenerType);
        assertNull(gardenerUnit.getTeachingType(farmer));

        scope.setMatchNegated(true);
        assertEquals(farmer, gardenerUnit.getTeachingType(farmer));

        spec().getUnitChangeType(UnitChangeType.EDUCATION)
            .removeScope(scope);
        spec().getUnitChangeType(UnitChangeType.EDUCATION)
            .deleteUnitChanges(gardenerType);
    }

    public void testCreation() {
        Game game = getStandardGame();
        Player dutch = game.getPlayerByNationId("model.nation.dutch");

        UnitType gardenerType = new UnitType("gardener", spec());

        assertEquals(0, count(spec().getUnitChanges(UnitChangeType.CREATION,
                                                    gardenerType)));
        
        addUnitTypeChange(UnitChangeType.CREATION, gardenerType, farmer,
                          100, -1);

        assertEquals(1, count(spec().getUnitChanges(UnitChangeType.CREATION,
                                                    gardenerType)));
        assertNotNull(spec().getUnitChange(UnitChangeType.CREATION,
                                           gardenerType));
        assertNotNull(spec().getUnitChange(UnitChangeType.CREATION,
                                           gardenerType, farmer));

        Unit gardenerUnit = new ServerUnit(game, null, dutch, gardenerType);
        assertEquals(farmer, gardenerUnit.getType());

        spec().getUnitChangeType(UnitChangeType.CREATION)
            .deleteUnitChanges(gardenerType);
    }
}
