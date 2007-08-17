package net.sf.freecol.common;

import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;

import junit.framework.TestCase;

public final class SpecificationTest extends TestCase {

    /**
     * Make sure that a specification object can be created without an exception
     * being thrown.
     * 
     */
    public void testLoad() {
        new Specification();
    }

    public void testAbilities() {
        Specification spec = new Specification();

        UnitType colonist = spec.unitType(Unit.FREE_COLONIST);
        assertTrue(colonist.hasAbility("found-colony"));
        assertTrue(colonist.hasAbility("recruitable"));

        UnitType caravel = spec.unitType(Unit.CARAVEL);
        assertFalse(caravel.hasAbility("found-colony"));
        assertFalse(caravel.hasAbility("recruitable"));
        assertTrue(caravel.hasAbility("naval"));
                   
        UnitType privateer = spec.unitType(Unit.PRIVATEER);
        assertFalse(privateer.hasAbility("found-colony"));
        assertFalse(privateer.hasAbility("recruitable"));
        assertTrue(privateer.hasAbility("naval"));
        assertTrue(privateer.hasAbility("capture-goods"));

    }
}
