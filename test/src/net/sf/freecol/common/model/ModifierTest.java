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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.sf.freecol.FreeCol;
import net.sf.freecol.util.test.FreeColTestCase;

public class ModifierTest extends FreeColTestCase {


    public void testAdditiveModifier() {

        Modifier modifier = new Modifier("test", 3, Modifier.Type.ADDITIVE);
        spec().addModifier(modifier);

        assertEquals(4f, modifier.applyTo(1));
    }

    public void testMultiplicativeModifier() {

        Modifier modifier = new Modifier("test", 1.5f, Modifier.Type.MULTIPLICATIVE);
        spec().addModifier(modifier);

        assertEquals(1 * 1.5f, modifier.applyTo(1));
        assertEquals(3 * 1.5f, modifier.applyTo(3));
    }

    public void testPercentageModifier() {

        Modifier modifier = new Modifier("test", 50, Modifier.Type.PERCENTAGE);
        spec().addModifier(modifier);

        assertEquals(150f, modifier.applyTo(100));
        assertEquals(4.5f, modifier.applyTo(3));

    }

    public void testCombineAdditiveModifiers() {

        Modifier modifier1 = new Modifier("test", 3, Modifier.Type.ADDITIVE);
        Modifier modifier2 = new Modifier("test", 4, Modifier.Type.ADDITIVE);
        Set<Modifier> modifierSet = new HashSet<Modifier>();
        modifierSet.add(modifier1);
        modifierSet.add(modifier2);
        assertEquals(1 + 3 + 4f, FeatureContainer.applyModifierSet(1, null, modifierSet));
    }

    public void testCombineMultiplicativeModifiers() {

        Modifier modifier1 = new Modifier("test", 3, Modifier.Type.MULTIPLICATIVE);
        Modifier modifier2 = new Modifier("test", 4, Modifier.Type.MULTIPLICATIVE);
        Set<Modifier> modifierSet = new HashSet<Modifier>();
        modifierSet.add(modifier1);
        modifierSet.add(modifier2);
        assertEquals(2 * 3 * 4f, FeatureContainer.applyModifierSet(2, null, modifierSet));
    }

    public void testCombinePercentageModifiers() {

        Modifier modifier1 = new Modifier("test", 3, Modifier.Type.PERCENTAGE);
        Modifier modifier2 = new Modifier("test", 4, Modifier.Type.PERCENTAGE);
        Set<Modifier> modifierSet = new HashSet<Modifier>();
        modifierSet.add(modifier1);
        modifierSet.add(modifier2);
        assertEquals(107f, FeatureContainer.applyModifierSet(100, null, modifierSet));
    }

    public void testCombinedModifier() {
        
        Modifier modifier1 = new Modifier("test", 3, Modifier.Type.ADDITIVE);
        Modifier modifier2 = new Modifier("test", 1.5f, Modifier.Type.MULTIPLICATIVE);
        Modifier modifier3 = new Modifier("test", 50, Modifier.Type.PERCENTAGE);

        Set<Modifier> modifierSet = new HashSet<Modifier>();
        modifierSet.add(modifier1);
        modifierSet.add(modifier2);
        assertEquals((1 + 3) * 1.5f, FeatureContainer.applyModifierSet(1, null, modifierSet));

        modifierSet.add(modifier3);
        assertEquals(((1 + 3) * 1.5f) * 1.5f, FeatureContainer.applyModifierSet(1, null, modifierSet));

        modifierSet.remove(modifier1);
        assertEquals(10 * 1.5f * 1.5f, FeatureContainer.applyModifierSet(10, null, modifierSet));
    }

    public void testScope() {

        UnitType carpenter = spec().getUnitType("model.unit.masterCarpenter");
        UnitType frigate = spec().getUnitType("model.unit.frigate");

        Modifier modifier1 = new Modifier("test", 3, Modifier.Type.ADDITIVE);
        Modifier modifier2 = new Modifier("test", 1.5f, Modifier.Type.MULTIPLICATIVE);
        Modifier modifier3 = new Modifier("test", 30, Modifier.Type.PERCENTAGE);

        FeatureContainer featureContainer = new FeatureContainer(spec());
        featureContainer.addModifier(modifier1);
        featureContainer.addModifier(modifier2);
        featureContainer.addModifier(modifier3);

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

        List<Scope> scopes = new ArrayList<Scope>();
        scopes.add(scope1);
        modifier1.setScopes(scopes);
        assertTrue(modifier1.appliesTo(frigate));
        assertFalse(modifier1.appliesTo(carpenter));

        Set<Modifier> result = featureContainer.getModifierSet("test", frigate);
        assertEquals(3, result.size());
        assertEquals(((1 + 3) * 1.5f) + ((1 + 3) * 1.5f) * 30 / 100, 
                FeatureContainer.applyModifierSet(1, null, result));

        result = featureContainer.getModifierSet("test", carpenter);
        assertEquals(2, result.size());
        assertEquals(1.5f + (1.5f * 30) / 100, 
                FeatureContainer.applyModifierSet(1, null, result));

        List<Scope> scopes2 = new ArrayList<Scope>();
        scopes2.add(scope2);
        scopes2.add(scope3);
        modifier2.setScopes(scopes2);
        assertTrue(modifier2.appliesTo(frigate));
        assertTrue(modifier2.appliesTo(carpenter));

        result = featureContainer.getModifierSet("test", frigate);
        assertEquals(3, result.size());
        assertEquals(((1 + 3) * 1.5f) + ((1 + 3) * 1.5f) * 30 / 100, 
                FeatureContainer.applyModifierSet(1, null, result));

        result = featureContainer.getModifierSet("test", carpenter);
        assertEquals(2, result.size());

        assertEquals(1.5f + (1.5f * 30) / 100, 
                FeatureContainer.applyModifierSet(1, null, result));
    }

    public void testTimeLimits() {

        UnitType frigate = spec().getUnitType("model.unit.frigate");

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

        FeatureContainer featureContainer = new FeatureContainer(spec());
        featureContainer.addModifier(modifier1);
        featureContainer.addModifier(modifier2);
        Set<Modifier> modifierSet = featureContainer.getModifierSet("test", frigate, new Turn(15));
        assertEquals(1, modifierSet.size());
        assertEquals(modifier1, modifierSet.iterator().next());
        modifierSet = featureContainer.getModifierSet("test", frigate, new Turn(35));
        assertEquals(1, modifierSet.size());
        assertEquals(modifier2, modifierSet.iterator().next());

    }

    public void testIncrements() {

        UnitType frigate = spec().getUnitType("model.unit.frigate");

        Modifier modifier1 = new Modifier("test", 1, Modifier.Type.ADDITIVE);
        Modifier modifier2 = new Modifier("test", 2, Modifier.Type.ADDITIVE);

        modifier1.setIncrement(1, Modifier.Type.ADDITIVE, new Turn(10), new Turn(15));
        assertFalse(modifier1.appliesTo(frigate, new Turn(9)));
        assertTrue(modifier1.appliesTo(frigate, new Turn(10)));
        assertTrue(modifier1.hasIncrement());

        FeatureContainer featureContainer = new FeatureContainer(spec());
        featureContainer.addModifier(modifier1);
        featureContainer.addModifier(modifier2);

        // only modifier2
        assertEquals(3f, featureContainer.applyModifier(1, "test", frigate, new Turn(9)));
        // both modifiers
        assertEquals(4f, featureContainer.applyModifier(1, "test", frigate, new Turn(10)));
        assertEquals(5f, featureContainer.applyModifier(1, "test", frigate, new Turn(11)));
        assertEquals(9f, featureContainer.applyModifier(1, "test", frigate, new Turn(15)));
        // only modifier2
        assertEquals(3f, featureContainer.applyModifier(1, "test", frigate, new Turn(16)));

    }

    public void testHashEquals() {

        Scope scope1 = new Scope();
        scope1.setType("model.unit.frigate");
        scope1.setAbilityID("whatever");

        Scope scope2 = new Scope();
        scope2.setType("model.unit.frigate");
        scope2.setAbilityID("whatever");

        Scope scope3 = new Scope();
        scope3.setType("model.unit.frigate");
        scope3.setAbilityID("whatever");
        scope3.setAbilityValue(false);

        assertEquals(scope1, scope1);
        assertEquals(scope1.hashCode(), scope1.hashCode());
        assertEquals(scope1, scope2);
        assertEquals(scope1.hashCode(), scope2.hashCode());
        assertFalse(scope1.equals(scope3));
        assertFalse(scope1.hashCode() == scope3.hashCode());

        Modifier modifier1 = new Modifier("test", 3, Modifier.Type.ADDITIVE);
        Modifier modifier2 = new Modifier("test", 3, Modifier.Type.ADDITIVE);
        Modifier modifier3 = new Modifier("test", 2, Modifier.Type.ADDITIVE);

        assertEquals(modifier1, modifier1);
        assertEquals(modifier1.hashCode(), modifier1.hashCode());
        assertEquals(modifier1, modifier2);
        assertEquals(modifier1.hashCode(), modifier2.hashCode());
        assertFalse(modifier1.equals(modifier3));
        assertFalse(modifier1.hashCode() == modifier3.hashCode());

        List<Scope> scopeList1 = new ArrayList<Scope>();
        scopeList1.add(scope1);
        scopeList1.add(scope3);
        List<Scope> scopeList2 = new ArrayList<Scope>();
        scopeList2.add(scope3);
        scopeList2.add(scope1);
        List<Scope> scopeList3 = new ArrayList<Scope>();
        scopeList3.add(scope1);
        scopeList3.add(scope2);

        modifier1.setScopes(scopeList1);
        modifier2.setScopes(scopeList1);

        assertEquals(modifier1, modifier1);
        assertEquals(modifier1.hashCode(), modifier1.hashCode());
        assertEquals(modifier1, modifier2);
        assertEquals(modifier1.hashCode(), modifier2.hashCode());
        assertFalse(modifier1.equals(modifier3));
        assertFalse(modifier1.hashCode() == modifier3.hashCode());

        modifier2.setScopes(scopeList2);

        assertEquals(modifier2, modifier2);
        assertEquals(modifier2.hashCode(), modifier2.hashCode());
        assertEquals(modifier1, modifier2);
        assertEquals(modifier1.hashCode(), modifier2.hashCode());
        assertFalse(modifier2.equals(modifier3));
        assertFalse(modifier2.hashCode() == modifier3.hashCode());

        modifier2.setScopes(scopeList3);

        assertEquals(modifier2, modifier2);
        assertEquals(modifier2.hashCode(), modifier2.hashCode());
        assertFalse(modifier1.equals(modifier2));
        assertFalse(modifier1.hashCode() == modifier2.hashCode());
        assertFalse(modifier2.equals(modifier3));
        assertFalse(modifier2.hashCode() == modifier3.hashCode());

    }

}
