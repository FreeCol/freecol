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
        assertEquals("HAPPY", new Tension(50).toString());
        assertEquals("CONTENT", new Tension(500).toString());
        assertEquals("HATEFUL", new Tension(2000).toString());
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
}