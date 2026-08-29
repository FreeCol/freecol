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

public class StanceTest extends FreeColTestCase {

    public void testIsValidTransition() {
        assertTrue(Stance.PEACE.isValidTransition(Stance.PEACE));

        assertTrue(Stance.UNCONTACTED.isValidTransition(Stance.PEACE));
        assertFalse(Stance.UNCONTACTED.isValidTransition(Stance.ALLIANCE));

        assertTrue(Stance.WAR.isValidTransition(Stance.CEASE_FIRE));
        assertTrue(Stance.CEASE_FIRE.isValidTransition(Stance.PEACE));

        assertFalse(Stance.PEACE.isValidTransition(Stance.UNCONTACTED));
    }

    public void testGetTradeValue() {
        double highRatio = 1.0;
        assertEquals(100, Stance.WAR.getTradeValue(highRatio));
        assertEquals(-1000, Stance.PEACE.getTradeValue(highRatio));

        double lowRatio = 0.1;
        assertEquals(-1000, Stance.WAR.getTradeValue(lowRatio));
        assertEquals(1000, Stance.PEACE.getTradeValue(lowRatio));
    }

    public void testGetTensionModifier() {
        assertEquals(Tension.WAR_MODIFIER,
            Stance.PEACE.getTensionModifier(Stance.WAR));

        assertEquals(Tension.CONTACT_MODIFIER,
            Stance.UNCONTACTED.getTensionModifier(Stance.PEACE));

        int expectedAllianceFromWar = Tension.ALLIANCE_MODIFIER
            + Tension.CEASE_FIRE_MODIFIER
            + Tension.PEACE_TREATY_MODIFIER;
        assertEquals(expectedAllianceFromWar, Stance.WAR.getTensionModifier(Stance.ALLIANCE));
    }

    public void testGetStanceFromTension() {
        Tension hateful = new Tension(Tension.Level.HATEFUL.getLimit() + 500);
        Tension happy = new Tension(Tension.Level.HAPPY.getLimit() - 500);

        assertEquals(Stance.WAR, Stance.PEACE.getStanceFromTension(hateful));
        assertEquals(Stance.CEASE_FIRE, Stance.WAR.getStanceFromTension(happy));
    }

    public void testAllTransitionsAgainstTable() {
        assertTrue(Stance.UNCONTACTED.isValidTransition(Stance.PEACE));
        assertTrue(Stance.UNCONTACTED.isValidTransition(Stance.WAR));
        assertFalse(Stance.UNCONTACTED.isValidTransition(Stance.ALLIANCE));
        assertFalse(Stance.UNCONTACTED.isValidTransition(Stance.CEASE_FIRE));

        assertTrue(Stance.ALLIANCE.isValidTransition(Stance.PEACE));
        assertTrue(Stance.ALLIANCE.isValidTransition(Stance.WAR));
        assertFalse(Stance.ALLIANCE.isValidTransition(Stance.CEASE_FIRE));
        assertFalse(Stance.ALLIANCE.isValidTransition(Stance.UNCONTACTED));

        assertTrue(Stance.PEACE.isValidTransition(Stance.ALLIANCE));
        assertTrue(Stance.PEACE.isValidTransition(Stance.WAR));
        assertFalse(Stance.PEACE.isValidTransition(Stance.CEASE_FIRE));
        assertFalse(Stance.PEACE.isValidTransition(Stance.UNCONTACTED));

        assertTrue(Stance.CEASE_FIRE.isValidTransition(Stance.ALLIANCE));
        assertTrue(Stance.CEASE_FIRE.isValidTransition(Stance.PEACE));
        assertTrue(Stance.CEASE_FIRE.isValidTransition(Stance.WAR));
        assertFalse(Stance.CEASE_FIRE.isValidTransition(Stance.UNCONTACTED));

        assertTrue(Stance.WAR.isValidTransition(Stance.ALLIANCE));
        assertTrue(Stance.WAR.isValidTransition(Stance.PEACE));
        assertTrue(Stance.WAR.isValidTransition(Stance.CEASE_FIRE));
        assertFalse(Stance.WAR.isValidTransition(Stance.UNCONTACTED));
    }

    public void testIllegalTensionModifierThrows() {
        try {
            Stance.UNCONTACTED.getTensionModifier(Stance.ALLIANCE);
            fail("Should have thrown RuntimeException for UNCONTACTED -> ALLIANCE");
        } catch (RuntimeException e) {
            assertTrue(true);
        }

        try {
            Stance.PEACE.getTensionModifier(Stance.CEASE_FIRE);
            fail("Should have thrown RuntimeException for PEACE -> CEASE_FIRE");
        } catch (RuntimeException e) {
            assertTrue(true);
        }

        try {
            Stance.ALLIANCE.getTensionModifier(Stance.CEASE_FIRE);
            fail("Should have thrown RuntimeException for ALLIANCE -> CEASE_FIRE");
        } catch (RuntimeException e) {
            assertTrue(true);
        }
    }

    public void testIdentityTensionModifierIsZero() {
        for (Stance s : Stance.values()) {
            assertEquals(0, s.getTensionModifier(s));
        }
    }

    public void testTensionDrivenTransitionsAreAlwaysLegal() {
        Tension low = new Tension(Tension.Level.HAPPY.getLimit() - 100);
        Tension mid = new Tension(Tension.Level.CONTENT.getLimit());
        Tension high = new Tension(Tension.Level.HATEFUL.getLimit() + 100);

        Tension[] tensions = { low, mid, high };

        for (Stance s : Stance.values()) {
            for (Tension t : tensions) {
                Stance result = s.getStanceFromTension(t);
                assertTrue(s.isValidTransition(result));
            }
        }
    }

    public void testIsIncitable() {
        assertTrue(Stance.PEACE.isIncitable());
        assertTrue(Stance.CEASE_FIRE.isIncitable());

        assertFalse(Stance.ALLIANCE.isIncitable());
        assertFalse(Stance.WAR.isIncitable());
        assertFalse(Stance.UNCONTACTED.isIncitable());
    }

    public void testHysteresisEffect() {
        Stance war = Stance.WAR;
        Tension onLimit = new Tension(Tension.Level.CONTENT.getLimit());
        assertEquals(Stance.WAR, war.getStanceFromTension(onLimit));

        Tension withinDelta = new Tension(Tension.Level.CONTENT.getLimit() - (Tension.DELTA / 2));
        assertEquals(Stance.WAR, war.getStanceFromTension(withinDelta));

        Tension beyondDelta = new Tension(Tension.Level.CONTENT.getLimit() - Tension.DELTA);
        assertEquals(Stance.CEASE_FIRE, war.getStanceFromTension(beyondDelta));

        Stance peace = Stance.PEACE;
        Tension hatefulLimit = new Tension(Tension.Level.HATEFUL.getLimit());
        assertEquals(Stance.PEACE, peace.getStanceFromTension(hatefulLimit));

        Tension slightlyHateful = new Tension(Tension.Level.HATEFUL.getLimit() + (Tension.DELTA / 2));
        assertEquals(Stance.PEACE, peace.getStanceFromTension(slightlyHateful));

        Tension trulyHateful = new Tension(Tension.Level.HATEFUL.getLimit() + Tension.DELTA + 1);
        assertEquals(Stance.WAR, peace.getStanceFromTension(trulyHateful));
    }
}