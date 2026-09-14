/**
 * Copyright (C) 2002-2024  The FreeCol Team
 *
 * This file is part of FreeCol.
 *
 * FreeCol is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * FreeCol is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with FreeCol.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.sf.freecol.common.model;

import net.sf.freecol.util.test.FreeColTestCase;

public class TensionTest extends FreeColTestCase {

    public void testDefaultConstructor() {
        Tension tension = new Tension();
        assertEquals(Tension.Level.HAPPY.getLimit(), tension.getValue());
        assertEquals(Tension.Level.HAPPY, tension.getLevel());
    }

    public void testValueMappingToLevels() {
        assertEquals(Tension.Level.HAPPY, new Tension(0).getLevel());
        assertEquals(Tension.Level.HAPPY, new Tension(100).getLevel());
        assertEquals(Tension.Level.CONTENT, new Tension(101).getLevel());
        assertEquals(Tension.Level.CONTENT, new Tension(600).getLevel());
        assertEquals(Tension.Level.HATEFUL, new Tension(1000).getLevel());
        assertEquals(Tension.Level.HATEFUL, new Tension(1500).getLevel());
    }

    public void testBoundaries() {
        Tension tension = new Tension();

        tension.setValue(-500);
        assertEquals(Tension.TENSION_MIN, tension.getValue());

        tension.setValue(Tension.TENSION_MAX + 500);
        assertEquals(Tension.TENSION_MAX, tension.getValue());
    }

    public void testModifyTension() {
        Tension tension = new Tension(Tension.Level.CONTENT.getLimit());
        
        tension.modify(Tension.PEACE_TREATY_MODIFIER);
        assertEquals(350, tension.getValue());
        assertEquals(Tension.Level.CONTENT, tension.getLevel());

        tension.modify(Tension.ALLIANCE_MODIFIER);
        assertEquals(0, tension.getValue());
        assertEquals(Tension.Level.HAPPY, tension.getLevel());

        tension.modify(Tension.WAR_MODIFIER);
        assertEquals(1000, tension.getValue());
        assertEquals(Tension.Level.HATEFUL, tension.getLevel());
    }

    public void testEqualsAndHashCode() {
        Tension t1 = new Tension(500);
        Tension t2 = new Tension(500);
        Tension t3 = new Tension(600);

        assertEquals(t1, t2);
        assertFalse(t1.equals(t3));
        assertEquals(t1.hashCode(), t2.hashCode());
    }

    public void testModifyClampsToMax() {
        Tension tension = new Tension(900);
        tension.modify(5000);
        assertEquals(Tension.TENSION_MAX, tension.getValue());
    }

    public void testGetLevelOrdering() {
        assertEquals(Tension.Level.HAPPY, new Tension(100).getLevel());
        assertEquals(Tension.Level.CONTENT, new Tension(101).getLevel());
        assertEquals(Tension.Level.DISPLEASED, new Tension(700).getLevel());
        assertEquals(Tension.Level.ANGRY, new Tension(800).getLevel());
        assertEquals(Tension.Level.HATEFUL, new Tension(1000).getLevel());
    }

    public void testToStringMatchesLevel() {
        assertEquals("HAPPY(50)", new Tension(50).toString());
        assertEquals("CONTENT(500)", new Tension(500).toString());
        assertEquals("HATEFUL(1100)", new Tension(2000).toString()); 
    }

    public void testKeyFormatting() {
        Tension t = new Tension(50);
        assertEquals("tension." + Tension.Level.HAPPY.getKey(), t.getKey());
    }

    public void testNameKeyFormatting() {
        Tension t = new Tension(50);
        String expected = "model.tension." + Tension.Level.HAPPY.getKey() + ".name";
        assertEquals(expected, t.getNameKey());
    }

    public void testLevelClassificationMethods() {
        assertTrue(Tension.Level.HAPPY.isFriendly());
        assertTrue(Tension.Level.HAPPY.isVeryFriendly());
        assertFalse(Tension.Level.HAPPY.isNeutral());
        assertFalse(Tension.Level.HAPPY.isHostile());
        assertTrue(Tension.Level.CONTENT.isFriendly());
        assertFalse(Tension.Level.CONTENT.isVeryFriendly());
        assertTrue(Tension.Level.CONTENT.isNeutral());
        assertTrue(Tension.Level.DISPLEASED.isNeutral());
        assertFalse(Tension.Level.DISPLEASED.isFriendly());
        assertTrue(Tension.Level.ANGRY.isHostile());
        assertFalse(Tension.Level.ANGRY.isVeryHostile());
        assertTrue(Tension.Level.HATEFUL.isHostile());
        assertTrue(Tension.Level.HATEFUL.isVeryHostile());
    }

    public void testLevelComparison() {
        assertTrue(Tension.Level.HATEFUL.isWorseThan(Tension.Level.HAPPY));
        assertTrue(Tension.Level.HAPPY.isBetterThan(Tension.Level.ANGRY));
        assertFalse(Tension.Level.CONTENT.isWorseThan(Tension.Level.CONTENT));
    }

    public void testPointsToNextLevel() {
        assertEquals(50, new Tension(50).pointsToNextLevel());
        assertEquals(50, new Tension(650).pointsToNextLevel());
        assertEquals(0, new Tension(Tension.Level.HATEFUL.getLimit()).pointsToNextLevel());
        assertEquals(0, new Tension(Tension.TENSION_MAX).pointsToNextLevel());
    }

    public void testLevelChangedAfterModify() {
        Tension tension = new Tension(95);

        assertFalse("Small change should not trigger level change", 
            tension.levelChangedAfterModify(2));

        assertTrue("Pushing over 100 should trigger level change", 
            tension.levelChangedAfterModify(10));
   
        assertEquals(Tension.Level.CONTENT, tension.getLevel());
    }

    public void testSurrenderValue() {
        Tension tension = new Tension(Tension.SURRENDERED);
        assertEquals(350, tension.getValue());
        assertEquals(Tension.Level.CONTENT, tension.getLevel());
    }
}
