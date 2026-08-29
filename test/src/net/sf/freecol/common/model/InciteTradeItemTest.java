/**
 * Copyright (C) 2002-2024  The FreeCol Team
 *
 * This file is part of FreeCol.
 *
 * FreeCol is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 */

package net.sf.freecol.common.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.util.test.FreeColTestCase;

public class InciteTradeItemTest extends FreeColTestCase {

    public void testInciteTradeItemValidity() {
        Game game = getGame();
        Player french = game.getPlayerByNationId("model.nation.french");
        Player english = game.getPlayerByNationId("model.nation.english");
        Player dutch = game.getPlayerByNationId("model.nation.dutch");

        InciteTradeItem item = new InciteTradeItem(game, french, english, dutch);
        assertTrue(item.isValid());

        InciteTradeItem invalidSource = new InciteTradeItem(game, french, english, french);
        assertFalse(invalidSource.isValid());

        InciteTradeItem invalidDest = new InciteTradeItem(game, french, english, english);
        assertFalse(invalidDest.isValid());
    }

    public void testEvaluateFor() {
        Game game = getGame();
        game.setMap(getTestMap());

        Player french = game.getPlayerByNationId("model.nation.french");
        Player english = game.getPlayerByNationId("model.nation.english");
        Player dutch = game.getPlayerByNationId("model.nation.dutch");

        InciteTradeItem item = new InciteTradeItem(game, french, english, dutch);

        english.setStance(dutch, Stance.ALLIANCE);
        assertEquals(TradeItem.INVALID_TRADE_ITEM, item.evaluateFor(english));

        english.setStance(dutch, Stance.WAR);
        assertEquals(0, item.evaluateFor(english));

        english.setStance(dutch, Stance.PEACE);
        int value = item.evaluateFor(english);
        assertTrue(value < 0);
    }

    public void testEvaluateForEdgeCases() {
        Game game = getGame();
        Player french = game.getPlayerByNationId("model.nation.french");
        Player dutch = game.getPlayerByNationId("model.nation.dutch");

        Player errorPlayer = new Player(game, "dummy.english.1") {
            @Override
            public double getStrengthRatio(Player other, boolean naval) {
                return -1.0;
            }
        };
        InciteTradeItem item1 = new InciteTradeItem(game, french, errorPlayer, dutch);
        assertEquals(-100, item1.evaluateFor(errorPlayer));

        Player weakPlayer = new Player(game, "dummy.english.2") {
            @Override
            public double getStrengthRatio(Player other, boolean naval) {
                return 0.0;
            }
        };
        InciteTradeItem item2 = new InciteTradeItem(game, french, weakPlayer, dutch);
        assertEquals(-500, item2.evaluateFor(weakPlayer));
    }

    public void testEvaluateForFallbackRatio() {
        Game game = getGame();
        game.setMap(getTestMap());

        Player french = game.getPlayerByNationId("model.nation.french");
        Player english = game.getPlayerByNationId("model.nation.english");
        Player dutch = game.getPlayerByNationId("model.nation.dutch");

        InciteTradeItem item = new InciteTradeItem(game, french, english, dutch);

        english.clearNationSummary(dutch);
        english.setStance(dutch, Stance.PEACE);

        int value = item.evaluateFor(english);
        assertTrue(value < 0);
    }

    public void testIsUnique() {
        Game game = getGame();
        Player french = game.getPlayerByNationId("model.nation.french");
        Player english = game.getPlayerByNationId("model.nation.english");
        Player dutch = game.getPlayerByNationId("model.nation.dutch");

        InciteTradeItem item = new InciteTradeItem(game, french, english, dutch);
        assertTrue(item.isUnique());
    }

    public void testSerializationRoundTrip() throws Exception {
        Game game = getGame();
        Player french = game.getPlayerByNationId("model.nation.french");
        Player english = game.getPlayerByNationId("model.nation.english");
        Player dutch = game.getPlayerByNationId("model.nation.dutch");

        InciteTradeItem item = new InciteTradeItem(game, french, english, dutch);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (FreeColXMLWriter xw =
                 new FreeColXMLWriter(bos, FreeColXMLWriter.WriteScope.toSave(), false)) {
            item.toXML(xw);
        }

        ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
        try (FreeColXMLReader xr = new FreeColXMLReader(bis)) {
            xr.nextTag();
            InciteTradeItem copy = new InciteTradeItem(game, xr);

            assertEquals(item.getVictim(), copy.getVictim());
            assertEquals(item.getSource(), copy.getSource());
        }
    }

    public void testEqualsAndHashCode() {
        Game game = getGame();
        Player french = game.getPlayerByNationId("model.nation.french");
        Player english = game.getPlayerByNationId("model.nation.english");
        Player dutch = game.getPlayerByNationId("model.nation.dutch");

        InciteTradeItem a = new InciteTradeItem(game, french, english, dutch);
        InciteTradeItem b = new InciteTradeItem(game, french, english, dutch);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }
}
