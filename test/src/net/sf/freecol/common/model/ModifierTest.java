package net.sf.freecol.common.model;

import net.sf.freecol.util.test.FreeColTestCase;

public class ModifierTest extends FreeColTestCase {

    public static final String COPYRIGHT = "Copyright (C) 2003-2007 The FreeCol Team";
    public static final String LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String REVISION = "$Revision$";

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

        Modifier modifier1 = new Modifier("test1", 3, Modifier.ADDITIVE);
        Modifier modifier2 = new Modifier("test2", 4, Modifier.ADDITIVE);
        modifier1.combine(modifier2);
        assertTrue(modifier1.getType() == Modifier.ADDITIVE);
        assertTrue(modifier1.applyTo(1) == 1 + 3 + 4);
        assertTrue(modifier1.getModifiers().size() == 2);
        assertTrue(modifier1.getModifiers().get(1) == modifier2);

    }

    public void testCombineMultiplicativeModifiers() {

        Modifier modifier1 = new Modifier("test1", 3, Modifier.MULTIPLICATIVE);
        Modifier modifier2 = new Modifier("test2", 4, Modifier.MULTIPLICATIVE);
        modifier1.combine(modifier2);
        assertTrue(modifier1.getType() == Modifier.MULTIPLICATIVE);
        assertTrue(modifier1.applyTo(2) == 2 * 3 * 4);
        assertTrue(modifier1.getModifiers().size() == 2);
        assertTrue(modifier1.getModifiers().get(1) == modifier2);

    }

    public void testCombinePercentageModifiers() {

        Modifier modifier1 = new Modifier("test1", 3, Modifier.PERCENTAGE);
        Modifier modifier2 = new Modifier("test2", 4, Modifier.PERCENTAGE);
        modifier1.combine(modifier2);
        assertTrue(modifier1.getType() == Modifier.PERCENTAGE);
        assertTrue(modifier1.applyTo(100) == 107f);
        assertTrue(modifier1.getModifiers().size() == 2);
        assertTrue(modifier1.getModifiers().get(1) == modifier2);

    }

    public void testCombinedModifier() {
        
        Modifier modifier1 = new Modifier("test1", 3, Modifier.ADDITIVE);
        Modifier modifier2 = new Modifier("test2", 1.5f, Modifier.MULTIPLICATIVE);
        Modifier modifier3 = new Modifier("test3", 50, Modifier.PERCENTAGE);

        modifier1.combine(modifier2);
        assertTrue(modifier1.getType() == Modifier.COMBINED);
        assertTrue(modifier1.applyTo(1) == (1 + 3) * 1.5f);
        assertTrue(modifier1.getModifiers().size() == 2);
        assertTrue(modifier1.getModifiers().get(1) == modifier2);

        modifier1.combine(modifier3);
        assertTrue(modifier1.getType() == Modifier.COMBINED);
        assertTrue(modifier1.applyTo(1) == ((1 + 3) * 1.5f) * 1.5f);
        assertTrue(modifier1.getModifiers().size() == 3);
        assertTrue(modifier1.getModifiers().get(1) == modifier2);
        assertTrue(modifier1.getModifiers().get(2) == modifier3);

        modifier2.combine(modifier3);
        assertTrue(modifier2.getType() == Modifier.COMBINED);
        assertTrue(modifier2.applyTo(10) == 10 * 1.5f * 1.5f);
        assertTrue(modifier2.getModifiers().size() == 2);
        assertTrue(modifier2.getModifiers().get(1) == modifier3);
        // but the modifiers of modifier1 have not changed
        assertTrue(modifier1.getModifiers().size() == 3);
        assertTrue(modifier1.getModifiers().get(1) == modifier2);
        assertTrue(modifier1.getModifiers().get(2) == modifier3);
    }

}