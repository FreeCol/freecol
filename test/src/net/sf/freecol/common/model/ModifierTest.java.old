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

public class ModifierTest extends FreeColTestCase {


    public void testAdditiveModifier() {

        Modifier modifier = new Modifier("test", 3, Modifier.Type.ADDITIVE);

        assertEquals(4f, modifier.applyTo(1));
    }

    public void testMultiplicativeModifier() {

        Modifier modifier = new Modifier("test", 1.5f, Modifier.Type.MULTIPLICATIVE);

        assertEquals(1 * 1.5f, modifier.applyTo(1));
        assertEquals(3 * 1.5f, modifier.applyTo(3));
    }

    public void testPercentageModifier() {

        Modifier modifier = new Modifier("test", 50, Modifier.Type.PERCENTAGE);

        assertEquals(150f, modifier.applyTo(100));
        assertEquals(4.5f, modifier.applyTo(3));

    }

    public void testCombineAdditiveModifiers() {

        Modifier modifier1 = new Modifier("test", 3, Modifier.Type.ADDITIVE);
        Modifier modifier2 = new Modifier("test", 4, Modifier.Type.ADDITIVE);
        Modifier modifier = Modifier.combine(null, modifier1, modifier2);
        assertEquals(Modifier.Type.ADDITIVE, modifier.getType());
        assertEquals(1 + 3 + 4f, modifier.applyTo(1));
        assertEquals(2, modifier.getModifiers().size());
        assertEquals(modifier1, modifier.getModifiers().get(0));
        assertEquals(modifier2, modifier.getModifiers().get(1));

    }

    public void testCombineMultiplicativeModifiers() {

        Modifier modifier1 = new Modifier("test", 3, Modifier.Type.MULTIPLICATIVE);
        Modifier modifier2 = new Modifier("test", 4, Modifier.Type.MULTIPLICATIVE);
        Modifier modifier = Modifier.combine(modifier1, null, modifier2);
        assertEquals(Modifier.Type.MULTIPLICATIVE, modifier.getType());
        assertEquals(2 * 3 * 4f, modifier.applyTo(2));
        assertEquals(2, modifier.getModifiers().size());
        assertEquals(modifier2, modifier.getModifiers().get(1));

    }

    public void testCombinePercentageModifiers() {

        Modifier modifier1 = new Modifier("test", 3, Modifier.Type.PERCENTAGE);
        Modifier modifier2 = new Modifier("test", 4, Modifier.Type.PERCENTAGE);
        Modifier modifier = Modifier.combine(modifier1, modifier2, null);
        assertEquals(Modifier.Type.PERCENTAGE, modifier.getType());
        assertEquals(107f, modifier.applyTo(100));
        assertEquals(2, modifier.getModifiers().size());
        assertEquals(modifier1, modifier.getModifiers().get(0));
        assertEquals(modifier2, modifier.getModifiers().get(1));

    }

    public void testCombinedModifier() {
        
        Modifier modifier1 = new Modifier("test", 3, Modifier.Type.ADDITIVE);
        Modifier modifier2 = new Modifier("test", 1.5f, Modifier.Type.MULTIPLICATIVE);
        Modifier modifier3 = new Modifier("test", 50, Modifier.Type.PERCENTAGE);

        Modifier modifier = Modifier.combine(null, modifier1, null, modifier2, null);
        assertEquals(Modifier.Type.COMBINED, modifier.getType());
        assertEquals((1 + 3) * 1.5f, modifier.applyTo(1));
        assertEquals(2, modifier.getModifiers().size());
        assertEquals(modifier.getModifiers().get(0), modifier1);
        assertEquals(modifier.getModifiers().get(1), modifier2);

        modifier = Modifier.combine(modifier, modifier3);
        assertEquals(Modifier.Type.COMBINED, modifier.getType());
        assertEquals(((1 + 3) * 1.5f) * 1.5f, modifier.applyTo(1));
        assertEquals(3, modifier.getModifiers().size());
        assertEquals(modifier1, modifier.getModifiers().get(0));
        assertEquals(modifier2, modifier.getModifiers().get(1));
        assertEquals(modifier3, modifier.getModifiers().get(2));

        modifier = Modifier.combine(modifier2, modifier3);
        assertEquals(Modifier.Type.COMBINED, modifier.getType());
        assertEquals(10 * 1.5f * 1.5f, modifier.applyTo(10));
        assertEquals(2, modifier.getModifiers().size());
        assertEquals(modifier2, modifier.getModifiers().get(0));
        assertEquals(modifier3, modifier.getModifiers().get(1));

    }

    public void testCombineFails() {

        Modifier modifier1 = new Modifier("test1", 3, Modifier.Type.ADDITIVE);
        Modifier modifier2 = new Modifier("test2", 1.5f, Modifier.Type.MULTIPLICATIVE);

        try {
            Modifier modifier = Modifier.combine(modifier1, modifier2);
            fail("Should not be able to combine Modifiers with different IDs.");
        } catch(Exception e) {
            // this is expected
        }

    }

    public void testRemove() {

        Modifier modifier1 = new Modifier("test", 3, Modifier.Type.ADDITIVE);
        Modifier modifier2 = new Modifier("test", 1.5f, Modifier.Type.MULTIPLICATIVE);
        Modifier modifier3 = new Modifier("test", 50, Modifier.Type.PERCENTAGE);

        Modifier modifier = Modifier.combine(modifier1, modifier2, modifier3);
        assertEquals(Modifier.Type.COMBINED, modifier.getType());
        assertEquals(((1 + 3) * 1.5f) * 1.5f, modifier.applyTo(1));
        assertEquals(3, modifier.getModifiers().size());
        assertEquals(modifier1, modifier.getModifiers().get(0));
        assertEquals(modifier2, modifier.getModifiers().get(1));
        assertEquals(modifier3, modifier.getModifiers().get(2));

        Modifier modifier4 = modifier.remove(modifier2);
        assertEquals(modifier, modifier4);
        assertEquals(2, modifier.getModifiers().size());
        assertEquals(modifier1, modifier.getModifiers().get(0));
        assertEquals(modifier3, modifier.getModifiers().get(1));
        assertEquals(((1 + 3) * 1.5f), modifier.applyTo(1));

        Modifier modifier5 = modifier4.remove(modifier3);
        assertEquals(modifier1, modifier5);

    }

    public void testScope() {

        UnitType carpenter = FreeCol.getSpecification().getUnitType("model.unit.masterCarpenter");
        UnitType frigate = FreeCol.getSpecification().getUnitType("model.unit.frigate");

        Modifier modifier1 = new Modifier("test", 3, Modifier.Type.ADDITIVE);
        Modifier modifier2 = new Modifier("test", 1.5f, Modifier.Type.MULTIPLICATIVE);
        Modifier modifier3 = new Modifier("test", 30, Modifier.Type.PERCENTAGE);
        Modifier modifier = Modifier.combine(modifier1, modifier2, modifier3);

        // applies to frigate
        Scope scope1 = new Scope();
        scope1.setType("model.unit.frigate");
        // applies to carpenter
        Scope scope2 = new Scope();
        scope2.setAbilityID("model.ability.navalUnit");
        scope2.setAbilityValue(false);
        // applies to frigate
        Scope scope3 = new Scope();
        scope3.setMethodName("getLineOfSight");
        scope3.setMethodValue("2");

        modifier1.getScopes().add(scope1);
        assertTrue(modifier1.appliesTo(frigate));
        assertFalse(modifier1.appliesTo(carpenter));

        Modifier result = modifier.getApplicableModifier(frigate);
        assertEquals(3, result.getModifiers().size());
        assertEquals(modifier1, result.getModifiers().get(0));
        assertEquals(modifier2, result.getModifiers().get(1));
        assertEquals(modifier3, result.getModifiers().get(2));
        assertEquals(((1 + 3) * 1.5f) + ((1 + 3) * 1.5f) * 30 / 100, result.applyTo(1));

        modifier.setScope(true);
        result = modifier.getApplicableModifier(carpenter);
        assertEquals(2, result.getModifiers().size());
        assertEquals(modifier2, result.getModifiers().get(0));
        assertEquals(modifier3, result.getModifiers().get(1));
        assertEquals(1.5f + (1.5f * 30) / 100, result.applyTo(1));

        modifier2.getScopes().add(scope2);
        modifier2.getScopes().add(scope3);
        assertTrue(modifier2.appliesTo(frigate));
        assertTrue(modifier2.appliesTo(carpenter));

        result = modifier.getApplicableModifier(frigate);
        assertEquals(3, result.getModifiers().size());
        assertEquals(modifier1, result.getModifiers().get(0));
        assertEquals(modifier2, result.getModifiers().get(1));
        assertEquals(modifier3, result.getModifiers().get(2));
        assertEquals(((1 + 3) * 1.5f) + ((1 + 3) * 1.5f) * 30 / 100, result.applyTo(1));

        result = modifier.getApplicableModifier(carpenter);
        assertEquals(2, result.getModifiers().size());
        assertEquals(modifier2, result.getModifiers().get(0));
        assertEquals(modifier3, result.getModifiers().get(1));
        assertEquals(1.5f + (1.5f * 30) / 100, result.applyTo(1));

    }

    public void testTimeLimits() {

        UnitType frigate = FreeCol.getSpecification().getUnitType("model.unit.frigate");

        Modifier modifier1 = new Modifier("test", 1, Modifier.Type.ADDITIVE);
        Modifier modifier2 = new Modifier("test", 2, Modifier.Type.ADDITIVE);

        modifier1.setFirstTurn(new Turn(10));
        modifier1.setLastTurn(new Turn(30));
        modifier2.setFirstTurn(new Turn(20));
        modifier2.setLastTurn(new Turn(40));

        assertTrue(modifier1.hasTimeLimit());
        assertTrue(modifier2.hasTimeLimit());

        assertFalse(modifier1.appliesTo(frigate, new Turn(5)));
        assertFalse(modifier1.appliesTo(frigate, new Turn(35)));
        assertFalse(modifier1.isOutOfDate(new Turn(25)));
        assertTrue(modifier1.appliesTo(frigate, new Turn(25)));

        assertFalse(modifier2.appliesTo(frigate, new Turn(5)));
        assertFalse(modifier2.appliesTo(frigate, new Turn(5)));
        assertFalse(modifier2.isOutOfDate(new Turn(25)));
        assertTrue(modifier2.appliesTo(frigate, new Turn(25)));

        Modifier modifier = Modifier.combine(modifier1, modifier2);

        assertTrue(modifier.hasTimeLimit());

        assertFalse(modifier.appliesTo(frigate, new Turn(5)));
        assertFalse(modifier.appliesTo(frigate, new Turn(45)));
        assertTrue(modifier.appliesTo(frigate, new Turn(25)));
        assertTrue(modifier.appliesTo(frigate, new Turn(35)));

        assertEquals(modifier1, modifier.getApplicableModifier(frigate, new Turn(15)));
        assertEquals(modifier2, modifier.getApplicableModifier(frigate, new Turn(35)));
        // modifier1 has been removed as out of date
        assertEquals(1, modifier.getModifiers().size());
        assertEquals(modifier2, modifier.getModifiers().get(0));

        modifier = Modifier.combine(modifier1, modifier2);
        Modifier applicableModifier = modifier.getApplicableModifier(frigate, new Turn(25));
        assertEquals(2, applicableModifier.getModifiers().size());
        assertEquals(modifier1, applicableModifier.getModifiers().get(0));
        assertEquals(modifier2, applicableModifier.getModifiers().get(1));

        assertEquals(1 + 1 + 2f, modifier.getApplicableModifier(frigate, new Turn(25)).applyTo(1));

    }

    public void testIncrements() {

        UnitType frigate = FreeCol.getSpecification().getUnitType("model.unit.frigate");

        Modifier modifier1 = new Modifier("test", 1, Modifier.Type.ADDITIVE);
        Modifier modifier2 = new Modifier("test", 2, Modifier.Type.ADDITIVE);

        modifier1.setIncrement(1, Modifier.Type.ADDITIVE, new Turn(10), new Turn(15));
        assertTrue(modifier1.hasIncrement());

        assertFalse(modifier1.appliesTo(frigate, new Turn(9)));
        assertEquals(null, modifier1.getApplicableModifier(frigate, new Turn(9)));
        assertTrue(modifier1.appliesTo(frigate, new Turn(10)));
        assertEquals(2f, modifier1.getApplicableModifier(frigate, new Turn(10)).applyTo(1));
        assertEquals(3f, modifier1.getApplicableModifier(frigate, new Turn(11)).applyTo(1));
        assertEquals(7f, modifier1.getApplicableModifier(frigate, new Turn(15)).applyTo(1));
        assertEquals(null, modifier1.getApplicableModifier(frigate, new Turn(16)));

    }

}
