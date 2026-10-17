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

/**
 * Unit tests for the {@link ColonyTradeItem} class.
 */
public class ColonyTradeItemTest extends FreeColTestCase {

    private static class TestColony extends Colony {
        public TestColony(Game game, Player owner, String name, Tile tile) {
            super(game, owner, name, tile);
            if (tile != null) {
                tile.setSettlement(this);
            }
        }
    }

    public void testValidity() {
        Game game = getGame();
        Map map = getTestMap();
        game.setMap(map);

        Player dutch = game.getPlayerByNationId("model.nation.dutch");
        Player french = game.getPlayerByNationId("model.nation.french");

        Colony colony =
            new TestColony(game, dutch, "Nieuw Amsterdam",
                           map.getTile(5, 5));

        ColonyTradeItem item =
            new ColonyTradeItem(game, dutch, french, colony);

        assertTrue(item.isValid());
    }

    public void testConstructorValidation() {
        Game game = getGame();
        Map map = getTestMap();
        game.setMap(map);

        Player dutch = game.getPlayerByNationId("model.nation.dutch");
        Player french = game.getPlayerByNationId("model.nation.french");

        Colony colony =
            new TestColony(game, dutch, "Nieuw Amsterdam",
                           map.getTile(5, 5));

        try {
            new ColonyTradeItem(game, french, dutch, colony);
            fail("Constructor should reject colonies not owned by source");
        } catch (IllegalArgumentException e) {
            // Expected.
        }
    }

    public void testCopyIn() {
        Game game = getGame();
        Map map = getTestMap();
        game.setMap(map);

        Player dutch = game.getPlayerByNationId("model.nation.dutch");
        Player french = game.getPlayerByNationId("model.nation.french");

        Colony colony =
            new TestColony(game, dutch, "Nieuw Amsterdam",
                           map.getTile(5, 5));

        ColonyTradeItem source =
            new ColonyTradeItem(game, dutch, french, colony);

        ColonyTradeItem target =
            new ColonyTradeItem(game, dutch, french, colony);

        assertTrue(target.copyIn(source));

        Colony copied = target.getColony(game);

        assertNotNull(copied);
        assertEquals(colony.getId(), copied.getId());
        assertEquals(dutch, target.getSource());
        assertEquals(french, target.getDestination());
    }

    public void testEvaluateFor() {
        Game game = getGame();
        Map map = getTestMap();
        game.setMap(map);

        Player dutch = game.getPlayerByNationId("model.nation.dutch");
        Player french = game.getPlayerByNationId("model.nation.french");

        Colony colony =
            new TestColony(game, dutch, "Nieuw Amsterdam",
                           map.getTile(5, 5));

        ColonyTradeItem item =
            new ColonyTradeItem(game, dutch, french, colony);

        assertTrue(item.evaluateFor(dutch)
            != TradeItem.INVALID_TRADE_ITEM);

        assertTrue(item.evaluateFor(french)
            != TradeItem.INVALID_TRADE_ITEM);
    }

    public void testEqualsAndHashCode() {
        Game game = getGame();
        Map map = getTestMap();
        game.setMap(map);

        Player dutch = game.getPlayerByNationId("model.nation.dutch");
        Player french = game.getPlayerByNationId("model.nation.french");

        Colony colony =
            new TestColony(game, dutch, "Nieuw Amsterdam",
                           map.getTile(5, 5));

        ColonyTradeItem a =
            new ColonyTradeItem(game, dutch, french, colony);

        ColonyTradeItem b =
            new ColonyTradeItem(game, dutch, french, colony);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    public void testSerializationRoundTrip() throws Exception {
        Game game = getGame();
        Map map = getTestMap();
        game.setMap(map);

        Player dutch = game.getPlayerByNationId("model.nation.dutch");
        Player french = game.getPlayerByNationId("model.nation.french");

        Colony colony =
            new TestColony(game, dutch, "Nieuw Amsterdam",
                           map.getTile(5, 5));

        ColonyTradeItem item =
            new ColonyTradeItem(game, dutch, french, colony);

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

            ColonyTradeItem copy =
                new ColonyTradeItem(game, xr);

            assertNotNull(copy.getColony(game));

            assertEquals(colony.getId(),
                         copy.getColony(game).getId());

            assertEquals(dutch, copy.getSource());
            assertEquals(french, copy.getDestination());

            assertEquals(item, copy);
            assertEquals(item.hashCode(), copy.hashCode());
        }
    }
}
