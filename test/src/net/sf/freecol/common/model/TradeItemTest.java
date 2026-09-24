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
import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.util.test.FreeColTestCase;

/**
 * Unit tests for the base {@link TradeItem} class logic.
 */
public class TradeItemTest extends FreeColTestCase {

    private static class MockTradeItem extends TradeItem {
        public MockTradeItem(Game game, Player source, Player destination) {
            super(game, "mock.trade.item", source, destination);
        }

        public MockTradeItem(Game game, FreeColXMLReader xr) throws XMLStreamException {
            super(game, xr);
        }

        @Override public boolean isValid() { return true; }
        @Override public boolean isUnique() { return false; }
        @Override public StringTemplate getLabel() { return StringTemplate.label("Mock"); }
        @Override public int evaluateFor(Player player) { return 0; }
        @Override public String getXMLTagName() { return "mockTradeItem"; }
    }

    public void testPlayerAssignment() {
        Game game = getGame();
        Player french = game.getPlayerByNationId("model.nation.french");
        Player english = game.getPlayerByNationId("model.nation.english");
        Player dutch = game.getPlayerByNationId("model.nation.dutch");

        TradeItem item = new MockTradeItem(game, french, english);

        assertEquals("Source should be French", french, item.getSource());
        assertEquals("Destination should be English", english, item.getDestination());

        // Test getOther logic
        assertEquals("Other than French should be English", english, item.getOther(french));
        assertEquals("Other than English should be French", french, item.getOther(english));
        assertEquals("Other than a 3rd party should return source by default", french, item.getOther(dutch));
    }

    public void testCopyIn() {
        Game game = getGame();
        Player french = game.getPlayerByNationId("model.nation.french");
        Player english = game.getPlayerByNationId("model.nation.english");
        Player dutch = game.getPlayerByNationId("model.nation.dutch");
        Player spanish = game.getPlayerByNationId("model.nation.spanish");

        MockTradeItem item1 = new MockTradeItem(game, french, english);
        MockTradeItem item2 = new MockTradeItem(game, dutch, spanish);

        assertTrue("copyIn should return true on successful copy", item1.copyIn(item2));
        
        assertEquals("Source should have been updated to Dutch", dutch, item1.getSource());
        assertEquals("Destination should have been updated to Spanish", spanish, item1.getDestination());
    }

    public void testEquality() {
        Game game = getGame();
        Player french = game.getPlayerByNationId("model.nation.french");
        Player english = game.getPlayerByNationId("model.nation.english");

        TradeItem item1 = new MockTradeItem(game, french, english);
        TradeItem item2 = new MockTradeItem(game, french, english);
        
        assertEquals("Items with same ID and players should be equal", item1, item2);
        assertEquals("HashCodes should match for equal items", item1.hashCode(), item2.hashCode());
    }

    public void testBaseSerialization() throws Exception {
        Game game = getGame();
        Player french = game.getPlayerByNationId("model.nation.french");
        Player english = game.getPlayerByNationId("model.nation.english");

        TradeItem item = new MockTradeItem(game, french, english);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(out, FreeColXMLWriter.WriteScope.toSave(), false)) {
            item.toXML(xw);
        }

        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        try (FreeColXMLReader xr = new FreeColXMLReader(in)) {
            xr.nextTag();
            MockTradeItem readItem = new MockTradeItem(game, xr);

            assertEquals("Source player should survive serialization", french, readItem.getSource());
            assertEquals("Destination player should survive serialization", english, readItem.getDestination());
        }
    }
}