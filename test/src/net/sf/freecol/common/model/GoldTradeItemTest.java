/**
 *  Copyright (C) 2002-2024  The FreeCol Team
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.util.test.FreeColTestCase;

/**
 * Unit tests for the {@link GoldTradeItem} class.
 */
public class GoldTradeItemTest extends FreeColTestCase {

    public void testValidity() {
        Game game = getGame();

        Player french = game.getPlayerByNationId("model.nation.french");
        Player english = game.getPlayerByNationId("model.nation.english");

        GoldTradeItem valid =
            new GoldTradeItem(game, french, english, 0);

        assertTrue(valid.isValid());

        GoldTradeItem negative =
            new GoldTradeItem(game, french, english, -1);

        assertFalse(negative.isValid());
    }

    public void testValidityInsufficientGold() {
        Game game = getGame();

        Player french = game.getPlayerByNationId("model.nation.french");
        Player english = game.getPlayerByNationId("model.nation.english");

        french.modifyGold(-french.getGold());

        GoldTradeItem item =
            new GoldTradeItem(game, french, english, 100);

        assertFalse(item.isValid());
    }

    public void testEvaluateFor() {
        Game game = getGame();

        Player french = game.getPlayerByNationId("model.nation.french");
        Player english = game.getPlayerByNationId("model.nation.english");

        french.modifyGold(1000);

        GoldTradeItem item =
            new GoldTradeItem(game, french, english, 250);

        assertTrue(item.isValid());

        assertEquals(-250, item.evaluateFor(french));
        assertEquals(250, item.evaluateFor(english));
    }

    public void testEvaluateForInvalidTrade() {
        Game game = getGame();

        Player french = game.getPlayerByNationId("model.nation.french");
        Player english = game.getPlayerByNationId("model.nation.english");

        GoldTradeItem item =
            new GoldTradeItem(game, french, english, -50);

        assertEquals(TradeItem.INVALID_TRADE_ITEM,
                     item.evaluateFor(french));
    }

    public void testCopyIn() {
        Game game = getGame();

        Player french = game.getPlayerByNationId("model.nation.french");
        Player english = game.getPlayerByNationId("model.nation.english");

        french.modifyGold(1000);

        GoldTradeItem source =
            new GoldTradeItem(game, french, english, 500);

        GoldTradeItem target =
            new GoldTradeItem(game, english, french, 0);

        assertTrue(target.copyIn(source));

        assertEquals(500, target.getGold());
        assertEquals(french, target.getSource());
        assertEquals(english, target.getDestination());
    }

    public void testIsUnique() {
        Game game = getGame();

        Player french = game.getPlayerByNationId("model.nation.french");
        Player english = game.getPlayerByNationId("model.nation.english");

        GoldTradeItem item =
            new GoldTradeItem(game, french, english, 100);

        assertTrue(item.isUnique());
    }

    public void testEqualsAndHashCode() {
        Game game = getGame();

        Player french = game.getPlayerByNationId("model.nation.french");
        Player english = game.getPlayerByNationId("model.nation.english");

        GoldTradeItem a =
            new GoldTradeItem(game, french, english, 100);

        GoldTradeItem b =
            new GoldTradeItem(game, french, english, 100);

        GoldTradeItem c =
            new GoldTradeItem(game, french, english, 200);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());

        assertFalse(a.equals(c));
    }

    public void testSerializationRoundTrip() throws Exception {
        Game game = getGame();

        Player french = game.getPlayerByNationId("model.nation.french");
        Player english = game.getPlayerByNationId("model.nation.english");

        GoldTradeItem item =
            new GoldTradeItem(game, french, english, 750);

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try (FreeColXMLWriter xw =
                 new FreeColXMLWriter(out,
                                      FreeColXMLWriter.WriteScope.toSave(),
                                      false)) {
            item.toXML(xw);
        }

        ByteArrayInputStream in =
            new ByteArrayInputStream(out.toByteArray());

        try (FreeColXMLReader xr = new FreeColXMLReader(in)) {
            xr.nextTag();

            GoldTradeItem copy = new GoldTradeItem(game, xr);

            assertEquals(750, copy.getGold());
            assertEquals(french, copy.getSource());
            assertEquals(english, copy.getDestination());
        }
    }
}
