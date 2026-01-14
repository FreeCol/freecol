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
 */

package net.sf.freecol.common.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.util.test.FreeColTestCase;

/**
 * Unit tests for the {@link GoodsTradeItem} class.
 */
public class GoodsTradeItemTest extends FreeColTestCase {

    public void testGoodsTradeItemValidity() {
        Game game = getGame();
        game.setMap(getTestMap());

        Player french = game.getPlayerByNationId("model.nation.french");
        Player english = game.getPlayerByNationId("model.nation.english");
        
        // Setup colony owned by the seller (French)
        Colony colony = createStandardColony(1, 5, 5); 
        colony.setOwner(french); 

        GoodsType cigars = spec().getGoodsType("model.goods.cigars");
        Goods goods = new Goods(game, colony, cigars, 50);
        GoodsTradeItem item = new GoodsTradeItem(game, french, english, goods);

        assertTrue("Trade item should be valid when owner matches colony", item.isValid());

        goods.setAmount(0);
        assertFalse("Trade item should be invalid if amount is 0", item.isValid());
        
        goods.setAmount(50);
        GoodsTradeItem invalidOwnerItem = new GoodsTradeItem(game, english, french, goods);
        assertFalse("English player should not be able to trade goods in a French colony", 
                   invalidOwnerItem.isValid());
    }

    public void testEvaluateFor() {
        Game game = getGame();
        game.setMap(getTestMap());

        Player french = game.getPlayerByNationId("model.nation.french");
        Player english = game.getPlayerByNationId("model.nation.english");
        
        Colony colony = createStandardColony(1, 5, 5);
        colony.setOwner(french);

        GoodsType silver = spec().getGoodsType("model.goods.silver");
        Goods goods = new Goods(game, colony, silver, 20);
        GoodsTradeItem item = new GoodsTradeItem(game, french, english, goods);

        int frenchValue = item.evaluateFor(french);
        assertTrue("Source value should be a valid market bid price", 
                   frenchValue != TradeItem.INVALID_TRADE_ITEM);
        
        int englishValue = item.evaluateFor(english);
        assertTrue("Destination value should be a valid market sale price", 
                   englishValue != TradeItem.INVALID_TRADE_ITEM);
    }

    public void testEvaluateWithTax() {
        Game game = getGame();
        game.setMap(getTestMap());

        Player french = game.getPlayerByNationId("model.nation.french");
        Player english = game.getPlayerByNationId("model.nation.english");
        
        Colony colony = createStandardColony(1, 5, 5);
        colony.setOwner(french);

        GoodsType silver = spec().getGoodsType("model.goods.silver");
        Goods goods = new Goods(game, colony, silver, 100);
        GoodsTradeItem item = new GoodsTradeItem(game, french, english, goods);

        english.setTax(0);
        int valueNoTax = item.evaluateFor(english);

        english.setTax(50);
        int valueWithTax = item.evaluateFor(english);

        assertEquals("Value with 50% tax should be exactly half of value with 0% tax", 
                     valueNoTax / 2, valueWithTax, 1.0);
    }

    public void testGoodsTradeItemSerialization() throws Exception {
        Game game = getGame();
        game.setMap(getTestMap());

        Player french = game.getPlayerByNationId("model.nation.french");
        Player english = game.getPlayerByNationId("model.nation.english");
        Colony colony = createStandardColony(1, 5, 5);
        colony.setOwner(french);

        GoodsType coats = spec().getGoodsType("model.goods.coats");
        Goods goods = new Goods(game, colony, coats, 40);
        GoodsTradeItem item = new GoodsTradeItem(game, french, english, goods);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (FreeColXMLWriter xw = new FreeColXMLWriter(out, FreeColXMLWriter.WriteScope.toSave(), false)) {
            xw.writeStartElement(item.getXMLTagName());
            item.writeAttributes(xw);
            item.writeChildren(xw);
            xw.writeEndElement();
        }

        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        try (FreeColXMLReader xr = new FreeColXMLReader(in)) {
            xr.nextTag();
            GoodsTradeItem readItem = new GoodsTradeItem(game, xr);
            assertNotNull("Reconstructed item should not be null", readItem);
            assertNotNull("Goods should be restored inside the trade item", readItem.getGoods());
            assertEquals("GoodsType should match after round-trip", coats, readItem.getGoods().getType());
            assertEquals("Goods amount should match after round-trip", 40, readItem.getGoods().getAmount());
            assertEquals("Source player ID should be correctly resolved", french, readItem.getSource());
        }
    }
}
