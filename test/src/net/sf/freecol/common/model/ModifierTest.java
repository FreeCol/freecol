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

        Modifier modifier = new Modifier("test", 3, Modifier.ADDITIVE);

        assertTrue(modifier.applyTo(1) == 3 + 1);
    }

    public void testMultiplicativeModifier() {

        Modifier modifier = new Modifier("test", 1.5f, Modifier.MULTIPLICATIVE);

        assertTrue(modifier.applyTo(1) == 1 * 1.5);
        assertTrue(modifier.applyTo(3) == 3 * 1.5);
    }

    public void testPercentageModifier() {

        Modifier modifier = new Modifier("test", 50, Modifier.PERCENTAGE);

        assertTrue(modifier.applyTo(100) == 150f);
        assertTrue(modifier.applyTo(3) == 4.5f);

    }

    public void testCombineAdditiveModifiers() {

        Modifier modifier1 = new Modifier("test", 3, Modifier.ADDITIVE);
        Modifier modifier2 = new Modifier("test", 4, Modifier.ADDITIVE);
        Modifier modifier = Modifier.combine(null, modifier1, modifier2);
        assertTrue(modifier.getType() == Modifier.ADDITIVE);
        assertTrue(modifier.applyTo(1) == 1 + 3 + 4);
        assertTrue(modifier.getModifiers().size() == 2);
        assertTrue(modifier.getModifiers().get(0) == modifier1);
        assertTrue(modifier.getModifiers().get(1) == modifier2);

    }

    public void testCombineMultiplicativeModifiers() {

        Modifier modifier1 = new Modifier("test", 3, Modifier.MULTIPLICATIVE);
        Modifier modifier2 = new Modifier("test", 4, Modifier.MULTIPLICATIVE);
        Modifier modifier = Modifier.combine(modifier1, null, modifier2);
        assertTrue(modifier.getType() == Modifier.MULTIPLICATIVE);
        assertTrue(modifier.applyTo(2) == 2 * 3 * 4);
        assertTrue(modifier.getModifiers().size() == 2);
        assertTrue(modifier.getModifiers().get(1) == modifier2);

    }

    public void testCombinePercentageModifiers() {

        Modifier modifier1 = new Modifier("test", 3, Modifier.PERCENTAGE);
        Modifier modifier2 = new Modifier("test", 4, Modifier.PERCENTAGE);
        Modifier modifier = Modifier.combine(modifier1, modifier2, null);
        assertTrue(modifier.getType() == Modifier.PERCENTAGE);
        assertTrue(modifier.applyTo(100) == 107f);
        assertTrue(modifier.getModifiers().size() == 2);
        assertTrue(modifier.getModifiers().get(0) == modifier1);
        assertTrue(modifier.getModifiers().get(1) == modifier2);

    }

    public void testCombinedModifier() {
        
        Modifier modifier1 = new Modifier("test", 3, Modifier.ADDITIVE);
        Modifier modifier2 = new Modifier("test", 1.5f, Modifier.MULTIPLICATIVE);
        Modifier modifier3 = new Modifier("test", 50, Modifier.PERCENTAGE);

        Modifier modifier = Modifier.combine(null, modifier1, null, modifier2, null);
        assertTrue(modifier.getType() == Modifier.COMBINED);
        assertTrue(modifier.applyTo(1) == (1 + 3) * 1.5f);
        assertTrue(modifier.getModifiers().size() == 2);
        assertTrue(modifier.getModifiers().get(0) == modifier1);
        assertTrue(modifier.getModifiers().get(1) == modifier2);

        modifier = Modifier.combine(modifier, modifier3);
        assertTrue(modifier.getType() == Modifier.COMBINED);
        assertTrue(modifier.applyTo(1) == ((1 + 3) * 1.5f) * 1.5f);
        assertTrue(modifier.getModifiers().size() == 3);
        assertTrue(modifier.getModifiers().get(0) == modifier1);
        assertTrue(modifier.getModifiers().get(1) == modifier2);
        assertTrue(modifier.getModifiers().get(2) == modifier3);

        modifier = Modifier.combine(modifier2, modifier3);
        assertTrue(modifier.getType() == Modifier.COMBINED);
        assertTrue(modifier.applyTo(10) == 10 * 1.5f * 1.5f);
        assertTrue(modifier.getModifiers().size() == 2);
        assertTrue(modifier.getModifiers().get(0) == modifier2);
        assertTrue(modifier.getModifiers().get(1) == modifier3);

    }

    public void testCombineFails() {

        Modifier modifier1 = new Modifier("test1", 3, Modifier.ADDITIVE);
        Modifier modifier2 = new Modifier("test2", 1.5f, Modifier.MULTIPLICATIVE);

        Modifier modifier = Modifier.combine(modifier1, modifier2);
        // returns null because ids differ
        assertTrue(modifier == null);
    }

    public void testRemove() {

        Modifier modifier1 = new Modifier("test", 3, Modifier.ADDITIVE);
        Modifier modifier2 = new Modifier("test", 1.5f, Modifier.MULTIPLICATIVE);
        Modifier modifier3 = new Modifier("test", 50, Modifier.PERCENTAGE);

        Modifier modifier = Modifier.combine(modifier1, modifier2, modifier3);
        assertTrue(modifier.getType() == Modifier.COMBINED);
        assertTrue(modifier.applyTo(1) == ((1 + 3) * 1.5f) * 1.5f);
        assertTrue(modifier.getModifiers().size() == 3);
        assertTrue(modifier.getModifiers().get(0) == modifier1);
        assertTrue(modifier.getModifiers().get(1) == modifier2);
        assertTrue(modifier.getModifiers().get(2) == modifier3);

        Modifier modifier4 = modifier.remove(modifier2);
        assertTrue(modifier4 == modifier);
        assertTrue(modifier.getModifiers().size() == 2);
        assertTrue(modifier.getModifiers().get(0) == modifier1);
        assertTrue(modifier.getModifiers().get(1) == modifier3);
        assertTrue(modifier.applyTo(1) == ((1 + 3) * 1.5f));

        Modifier modifier5 = modifier4.remove(modifier3);
        assertTrue(modifier5 == modifier1);

    }

    public void testScope() {

        UnitType carpenter = FreeCol.getSpecification().getUnitType("model.unit.masterCarpenter");
        UnitType frigate = FreeCol.getSpecification().getUnitType("model.unit.frigate");

        Modifier modifier1 = new Modifier("test", 3, Modifier.ADDITIVE);
        Modifier modifier2 = new Modifier("test", 1.5f, Modifier.MULTIPLICATIVE);
        Modifier modifier3 = new Modifier("test", 30, Modifier.PERCENTAGE);
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
        assertTrue(result.getModifiers().size() == 3);
        assertTrue(result.getModifiers().get(0) == modifier1);
        assertTrue(result.getModifiers().get(1) == modifier2);
        assertTrue(result.getModifiers().get(2) == modifier3);
        assertTrue(result.applyTo(1) == ((1 + 3) * 1.5f) + ((1 + 3) * 1.5f) * 30 / 100);

        result = modifier.getApplicableModifier(carpenter);
        assertTrue(result.getModifiers().size() == 2);
        assertTrue(result.getModifiers().get(0) == modifier2);
        assertTrue(result.getModifiers().get(1) == modifier3);
        assertTrue(result.applyTo(1) == 1.5f + (1.5f * 30) / 100);

        modifier2.getScopes().add(scope2);
        modifier2.getScopes().add(scope3);
        assertTrue(modifier2.appliesTo(frigate));
        assertTrue(modifier2.appliesTo(carpenter));

        result = modifier.getApplicableModifier(frigate);
        assertTrue(result.getModifiers().size() == 3);
        assertTrue(result.getModifiers().get(0) == modifier1);
        assertTrue(result.getModifiers().get(1) == modifier2);
        assertTrue(result.getModifiers().get(2) == modifier3);
        assertTrue(result.applyTo(1) == ((1 + 3) * 1.5f) + ((1 + 3) * 1.5f) * 30 / 100);

        result = modifier.getApplicableModifier(carpenter);
        assertTrue(result.getModifiers().size() == 2);
        assertTrue(result.getModifiers().get(0) == modifier2);
        assertTrue(result.getModifiers().get(1) == modifier3);
        assertTrue(result.applyTo(1) == 1.5f + (1.5f * 30) / 100);

    }

}
