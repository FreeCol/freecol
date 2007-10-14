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

import net.sf.freecol.FreeCol;
import net.sf.freecol.util.test.FreeColTestCase;

public class ScopeTest extends FreeColTestCase {


    UnitType carpenter = FreeCol.getSpecification().getUnitType("model.unit.masterCarpenter");
    UnitType frigate = FreeCol.getSpecification().getUnitType("model.unit.frigate");

    public void testEmptyScope(){
    	
        Scope testScope = new Scope();

        // empty scope applies to all
        assertTrue(testScope.appliesTo(carpenter));
        assertTrue(testScope.appliesTo(frigate));
    }

    public void testTypeScope() {

        Scope testScope = new Scope();

        testScope.setType("model.unit.frigate");
        assertTrue(testScope.appliesTo(frigate));
        assertFalse(testScope.appliesTo(carpenter));

        testScope.setType("model.unit.masterCarpenter");
        assertFalse(testScope.appliesTo(frigate));
        assertTrue(testScope.appliesTo(carpenter));

    }

    public void testAbilityScope() {

        Scope testScope = new Scope();

        testScope.setAbilityID("model.ability.navalUnit");
        assertEquals(frigate.hasAbility("model.ability.navalUnit"),
                     testScope.appliesTo(frigate));
        assertEquals(carpenter.hasAbility("model.ability.navalUnit"),
                     testScope.appliesTo(carpenter));

    }
    
    public void testMethodScope() {

        Scope testScope = new Scope();

        testScope.setMethodName("getLineOfSight");
        testScope.setMethodValue("1");
        assertTrue(frigate.getLineOfSight() != 1);
        assertFalse(testScope.appliesTo(frigate));
        assertTrue(carpenter.getLineOfSight() == 1);
        assertTrue(testScope.appliesTo(carpenter));

    }

    public void testCombinedScope() {

        Scope testScope = new Scope();

        testScope.setType("model.unit.frigate");
        testScope.setAbilityID("model.ability.navalUnit");
        testScope.setMethodName("getLineOfSight");
        testScope.setMethodValue("2");
        assertTrue(testScope.appliesTo(frigate));

        testScope.setMethodValue("1");
        assertFalse(testScope.appliesTo(frigate));

        testScope.setMethodValue("2");
        testScope.setAbilityID("model.ability.foundColony");
        assertFalse(testScope.appliesTo(frigate));
    }

    public void testEquality() {

        Scope testScope1 = new Scope();
        testScope1.setType("model.unit.frigate");
        testScope1.setAbilityID("model.ability.navalUnit");
        testScope1.setMethodName("getLineOfSight");
        testScope1.setMethodValue("2");
        assertTrue(testScope1.equals(testScope1));

        Scope testScope2 = new Scope();
        testScope2.setType("model.unit.frigate");
        testScope2.setAbilityID("model.ability.navalUnit");
        testScope2.setMethodName("getLineOfSight");
        testScope2.setMethodValue("2");
        assertTrue(testScope2.equals(testScope2));

        assertTrue(testScope1.equals(testScope2));
        assertTrue(testScope2.equals(testScope1));

        testScope1.setType("model.unit.carpenter");

        assertFalse(testScope1.equals(testScope2));
        assertFalse(testScope2.equals(testScope1));

        testScope1.setType("model.unit.frigate");
        testScope1.setAbilityID("model.ability.foundColony");

        assertFalse(testScope1.equals(testScope2));
        assertFalse(testScope2.equals(testScope1));

        testScope1.setAbilityID("model.ability.navalUnit");
        testScope1.setAbilityValue(false);

        assertFalse(testScope1.equals(testScope2));
        assertFalse(testScope2.equals(testScope1));

        testScope1.setAbilityValue(true);
        testScope1.setMethodName("getOffence");

        assertFalse(testScope1.equals(testScope2));
        assertFalse(testScope2.equals(testScope1));

        testScope1.setMethodName("getLineOfSight");
        testScope1.setMethodValue("9");

        assertFalse(testScope1.equals(testScope2));
        assertFalse(testScope2.equals(testScope1));

        testScope1.setMethodValue("2");

        assertTrue(testScope1.equals(testScope2));
        assertTrue(testScope2.equals(testScope1));

    }
        

}
