package net.sf.freecol.common.model;

import net.sf.freecol.FreeCol;
import net.sf.freecol.util.test.FreeColTestCase;

public class ScopeTest extends FreeColTestCase {

    public static final String COPYRIGHT = "Copyright (C) 2003-2007 The FreeCol Team";
    public static final String LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String REVISION = "$Revision$";

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
}
