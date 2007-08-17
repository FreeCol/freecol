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

    /**
     * Test for some typical abilities.
     */
    public void testAbilities() {
        Specification spec = new Specification();

        UnitType colonist = spec.unitType(Unit.FREE_COLONIST);
        assertTrue(colonist.hasAbility("model.ability.foundColony"));
        assertTrue(colonist.hasAbility("model.ability.recruitable"));
        assertFalse(colonist.hasAbility("model.ability.navalUnit"));
        assertFalse(colonist.hasAbility("model.ability.carryGoods"));
        assertFalse(colonist.hasAbility("model.ability.carryUnits"));
        assertFalse(colonist.hasAbility("model.ability.captureGoods"));

        UnitType wagon = spec.unitType(Unit.WAGON_TRAIN);
        assertFalse(wagon.hasAbility("model.ability.foundColony"));
        assertFalse(wagon.hasAbility("model.ability.recruitable"));
        assertFalse(wagon.hasAbility("model.ability.navalUnit"));
        assertTrue(wagon.hasAbility("model.ability.carryGoods"));
        assertFalse(wagon.hasAbility("model.ability.carryUnits"));
        assertFalse(wagon.hasAbility("model.ability.captureGoods"));

        UnitType brave = spec.unitType(Unit.BRAVE);
        assertFalse(brave.hasAbility("model.ability.foundColony"));
        assertFalse(brave.hasAbility("model.ability.recruitable"));
        assertFalse(brave.hasAbility("model.ability.navalUnit"));
        assertTrue(brave.hasAbility("model.ability.carryGoods"));
        assertFalse(brave.hasAbility("model.ability.carryUnits"));
        assertFalse(brave.hasAbility("model.ability.captureGoods"));

        UnitType caravel = spec.unitType(Unit.CARAVEL);
        assertFalse(caravel.hasAbility("model.ability.foundColony"));
        assertFalse(caravel.hasAbility("model.ability.recruitable"));
        assertTrue(caravel.hasAbility("model.ability.navalUnit"));
        assertTrue(caravel.hasAbility("model.ability.carryGoods"));
        assertTrue(caravel.hasAbility("model.ability.carryUnits"));
        assertFalse(caravel.hasAbility("model.ability.captureGoods"));
                   
        UnitType privateer = spec.unitType(Unit.PRIVATEER);
        assertFalse(privateer.hasAbility("model.ability.foundColony"));
        assertFalse(privateer.hasAbility("model.ability.recruitable"));
        assertTrue(privateer.hasAbility("model.ability.navalUnit"));
        assertTrue(privateer.hasAbility("model.ability.carryGoods"));
        assertTrue(privateer.hasAbility("model.ability.carryUnits"));
        assertTrue(privateer.hasAbility("model.ability.captureGoods"));

    }
}
