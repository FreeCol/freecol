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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.util.test.FreeColTestCase;

public class StanceTradeItemTest extends FreeColTestCase {

    public void testValidity() {
        Game game = getGame();
        Player dutch = game.getPlayerByNationId("model.nation.dutch");
        Player french = game.getPlayerByNationId("model.nation.french");

        dutch.setStance(french, Stance.PEACE);

        StanceTradeItem sti = new StanceTradeItem(game, dutch, french, Stance.ALLIANCE);
        assertTrue(sti.isValid());

        dutch.setStance(french, Stance.UNCONTACTED);
        assertFalse(sti.isValid());
    }

    public void testEvaluation() {
        Game game = getGame();
        Player dutch = game.getPlayerByNationId("model.nation.dutch");
        Player french = game.getPlayerByNationId("model.nation.french");

        StanceTradeItem sti = new StanceTradeItem(game, dutch, french, Stance.WAR);
        
        int value = sti.evaluateFor(dutch);
        assertTrue(value != 0);
    }

    public void testTensionImpact() {
        Game game = getGame();
        Player dutch = game.getPlayerByNationId("model.nation.dutch");
        Player french = game.getPlayerByNationId("model.nation.french");

        dutch.setStance(french, Stance.PEACE);
        StanceTradeItem sti = new StanceTradeItem(game, dutch, french, Stance.WAR);

        int impact = sti.getTensionImpact();
        assertEquals(Tension.WAR_MODIFIER, impact);
    }

    public void testSerializationRoundTrip() throws Exception {
        Game game = getGame();
        Player dutch = game.getPlayerByNationId("model.nation.dutch");
        Player french = game.getPlayerByNationId("model.nation.french");

        StanceTradeItem sti = new StanceTradeItem(game, dutch, french, Stance.ALLIANCE);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        FreeColXMLWriter xw = new FreeColXMLWriter(out, FreeColXMLWriter.WriteScope.toSave(), false);

        sti.toXML(xw);
        xw.close();

        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        FreeColXMLReader xr = new FreeColXMLReader(in);
        xr.nextTag();

        StanceTradeItem sti2 = new StanceTradeItem(game, xr);
        xr.close();

        assertEquals(Stance.ALLIANCE, sti2.getStance());
        assertEquals(dutch, sti2.getSource());
        assertEquals(french, sti2.getDestination());
    }

    public void testEqualsAndHashCode() {
        Game game = getGame();
        Player dutch = game.getPlayerByNationId("model.nation.dutch");
        Player french = game.getPlayerByNationId("model.nation.french");

        StanceTradeItem sti1 = new StanceTradeItem(game, dutch, french, Stance.PEACE);
        StanceTradeItem sti2 = new StanceTradeItem(game, dutch, french, Stance.PEACE);
        StanceTradeItem sti3 = new StanceTradeItem(game, dutch, french, Stance.WAR);

        assertEquals(sti1, sti2);
        assertFalse(sti1.equals(sti3));
        assertEquals(sti1.hashCode(), sti2.hashCode());
    }

    public void testNullStanceIsInvalid() {
        Game game = getGame();
        Player dutch = game.getPlayerByNationId("model.nation.dutch");
        Player french = game.getPlayerByNationId("model.nation.french");

        StanceTradeItem sti = new StanceTradeItem(game, dutch, french, null);
        assertFalse(sti.isValid());
    }

    public void testValidityRespectsDirectionalStance() {
        Game game = getGame();
        Player dutch = game.getPlayerByNationId("model.nation.dutch");
        Player french = game.getPlayerByNationId("model.nation.french");

        dutch.setStance(french, Stance.PEACE);
        french.setStance(dutch, Stance.WAR);

        StanceTradeItem sti = new StanceTradeItem(game, dutch, french, Stance.ALLIANCE);
        assertTrue(sti.isValid());

        StanceTradeItem reverse = new StanceTradeItem(game, french, dutch, Stance.ALLIANCE);
        assertTrue(reverse.isValid());
    }

    public void testAllTensionImpacts() {
        Game game = getGame();
        Player dutch = game.getPlayerByNationId("model.nation.dutch");
        Player french = game.getPlayerByNationId("model.nation.french");

        dutch.setStance(french, Stance.WAR);

        assertEquals(Tension.CEASE_FIRE_MODIFIER,
            new StanceTradeItem(game, dutch, french, Stance.CEASE_FIRE).getTensionImpact());

        assertEquals(Tension.CEASE_FIRE_MODIFIER + Tension.PEACE_TREATY_MODIFIER,
            new StanceTradeItem(game, dutch, french, Stance.PEACE).getTensionImpact());

        int expectedAlliance = Tension.ALLIANCE_MODIFIER
            + Tension.CEASE_FIRE_MODIFIER
            + Tension.PEACE_TREATY_MODIFIER;

        assertEquals(expectedAlliance,
            new StanceTradeItem(game, dutch, french, Stance.ALLIANCE).getTensionImpact());
    }

    public void testTensionImpactThrowsOnInvalidTransition() {
        Game game = getGame();
        Player dutch = game.getPlayerByNationId("model.nation.dutch");
        Player french = game.getPlayerByNationId("model.nation.french");

        dutch.setStance(french, Stance.UNCONTACTED);
        StanceTradeItem sti = new StanceTradeItem(game, dutch, french, Stance.ALLIANCE);

        try {
            sti.getTensionImpact();
            fail("Should have thrown RuntimeException");
        } catch (RuntimeException e) {
            assertTrue(true);
        }
    }

    public void testSerializationPreservesValidity() throws Exception {
        Game game = getGame();
        Player dutch = game.getPlayerByNationId("model.nation.dutch");
        Player french = game.getPlayerByNationId("model.nation.french");

        dutch.setStance(french, Stance.PEACE);

        StanceTradeItem sti = new StanceTradeItem(game, dutch, french, Stance.ALLIANCE);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        FreeColXMLWriter xw = new FreeColXMLWriter(out, FreeColXMLWriter.WriteScope.toSave(), false);
        sti.toXML(xw);
        xw.close();

        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        FreeColXMLReader xr = new FreeColXMLReader(in);
        xr.nextTag();

        StanceTradeItem sti2 = new StanceTradeItem(game, xr);
        xr.close();

        assertTrue(sti2.isValid());
    }
}