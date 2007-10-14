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
        Modifier modifier = Modifier.combine(modifier1, modifier2);
        assertTrue(modifier.getType() == Modifier.ADDITIVE);
        assertTrue(modifier.applyTo(1) == 1 + 3 + 4);
        assertTrue(modifier.getModifiers().size() == 2);
        assertTrue(modifier.getModifiers().get(0) == modifier1);
        assertTrue(modifier.getModifiers().get(1) == modifier2);

    }

    public void testCombineMultiplicativeModifiers() {

        Modifier modifier1 = new Modifier("test", 3, Modifier.MULTIPLICATIVE);
        Modifier modifier2 = new Modifier("test", 4, Modifier.MULTIPLICATIVE);
        Modifier modifier = Modifier.combine(modifier1, modifier2);
        assertTrue(modifier.getType() == Modifier.MULTIPLICATIVE);
        assertTrue(modifier.applyTo(2) == 2 * 3 * 4);
        assertTrue(modifier.getModifiers().size() == 2);
        assertTrue(modifier.getModifiers().get(1) == modifier2);

    }

    public void testCombinePercentageModifiers() {

        Modifier modifier1 = new Modifier("test", 3, Modifier.PERCENTAGE);
        Modifier modifier2 = new Modifier("test", 4, Modifier.PERCENTAGE);
        Modifier modifier = Modifier.combine(modifier1, modifier2);
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

        Modifier modifier = Modifier.combine(modifier1, modifier2);
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

        Modifier modifier = Modifier.combine(modifier1, modifier2);
        modifier = Modifier.combine(modifier, modifier3);
        assertTrue(modifier.getType() == Modifier.COMBINED);
        assertTrue(modifier.applyTo(1) == ((1 + 3) * 1.5f) * 1.5f);
        assertTrue(modifier.getModifiers().size() == 3);
        assertTrue(modifier.getModifiers().get(0) == modifier1);
        assertTrue(modifier.getModifiers().get(1) == modifier2);
        assertTrue(modifier.getModifiers().get(2) == modifier3);

        modifier.getModifiers().remove(modifier2);
        assertTrue(modifier.getModifiers().size() == 2);
        assertTrue(modifier.getModifiers().get(0) == modifier1);
        assertTrue(modifier.getModifiers().get(1) == modifier3);
        // values have not been removed
        assertTrue(modifier.applyTo(1) == ((1 + 3) * 1.5f) * 1.5f);
        modifier.removeValues(modifier2);
        assertTrue(modifier.applyTo(1) == ((1 + 3) * 1.5f));
    }

}
