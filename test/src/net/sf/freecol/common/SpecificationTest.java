package net.sf.freecol.common;

import net.sf.freecol.common.model.FoundingFather;
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
    public void testUnitAbilities() {
        Specification spec = new Specification();

        UnitType colonist = spec.getUnitType(Unit.FREE_COLONIST);
        assertTrue(colonist.hasAbility("model.ability.foundColony"));
        assertTrue(colonist.isRecruitable());
        assertFalse(colonist.hasAbility("model.ability.navalUnit"));
        assertFalse(colonist.hasAbility("model.ability.carryGoods"));
        assertFalse(colonist.hasAbility("model.ability.carryUnits"));
        assertFalse(colonist.hasAbility("model.ability.captureGoods"));

        UnitType wagon = spec.getUnitType(Unit.WAGON_TRAIN);
        assertFalse(wagon.hasAbility("model.ability.foundColony"));
        assertFalse(wagon.isRecruitable());
        assertFalse(wagon.hasAbility("model.ability.navalUnit"));
        assertTrue(wagon.hasAbility("model.ability.carryGoods"));
        assertFalse(wagon.hasAbility("model.ability.carryUnits"));
        assertFalse(wagon.hasAbility("model.ability.captureGoods"));

        UnitType brave = spec.getUnitType(Unit.BRAVE);
        assertFalse(brave.hasAbility("model.ability.foundColony"));
        assertFalse(brave.isRecruitable());
        assertFalse(brave.hasAbility("model.ability.navalUnit"));
        assertTrue(brave.hasAbility("model.ability.carryGoods"));
        assertFalse(brave.hasAbility("model.ability.carryUnits"));
        assertFalse(brave.hasAbility("model.ability.captureGoods"));

        UnitType caravel = spec.getUnitType(Unit.CARAVEL);
        assertFalse(caravel.hasAbility("model.ability.foundColony"));
        assertFalse(caravel.isRecruitable());
        assertTrue(caravel.hasAbility("model.ability.navalUnit"));
        assertTrue(caravel.hasAbility("model.ability.carryGoods"));
        assertTrue(caravel.hasAbility("model.ability.carryUnits"));
        assertFalse(caravel.hasAbility("model.ability.captureGoods"));
                   
        UnitType privateer = spec.getUnitType(Unit.PRIVATEER);
        assertFalse(privateer.hasAbility("model.ability.foundColony"));
        assertFalse(privateer.isRecruitable());
        assertTrue(privateer.hasAbility("model.ability.navalUnit"));
        assertTrue(privateer.hasAbility("model.ability.carryGoods"));
        assertTrue(privateer.hasAbility("model.ability.carryUnits"));
        assertTrue(privateer.hasAbility("model.ability.captureGoods"));

    }

    public void testFoundingFathers() {

        Specification spec = new Specification();

        FoundingFather smith = spec.getFoundingFather("model.foundingFather.adamSmith");
        assertFalse(smith == null);
        assertTrue(smith.getType() == FoundingFather.TRADE);
        // weight is some value in [0, 10]
        assertTrue(smith.getWeight(1) >= 0);
        assertTrue(smith.getWeight(2) >= 0);
        assertTrue(smith.getWeight(3) >= 0);
        assertTrue(smith.getWeight(1) <= 10);
        assertTrue(smith.getWeight(2) <= 10);
        assertTrue(smith.getWeight(3) <= 10);
        // weight 3 is the default
        assertTrue(smith.getWeight(3) == smith.getWeight(0));
        assertTrue(smith.getWeight(3) == smith.getWeight(34));
        // check for ability
        assertTrue(smith.hasAbility("model.ability.buildFactory"));
    }


}
