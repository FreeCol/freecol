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
import java.util.List;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.DiplomaticTrade.TradeContext;
import net.sf.freecol.common.model.DiplomaticTrade.TradeStatus;
import net.sf.freecol.util.test.FreeColTestCase;

public class DiplomaticTradeTest extends FreeColTestCase {

    private static class TestColony extends Colony {
        public TestColony(Game game, Player owner, String name, Tile tile) {
            super(game, owner, name, tile);
            if (tile != null) {
                tile.setSettlement(this);
            }
        }
    }

    public void testDiplomaticTradeConstruction() {
        Game game = getGame();
        Player dutch = game.getPlayerByNationId("model.nation.dutch");
        Player french = game.getPlayerByNationId("model.nation.french");

        DiplomaticTrade dt = new DiplomaticTrade(game, TradeContext.DIPLOMATIC, 
            dutch, french, null, 1);

        assertEquals(TradeContext.DIPLOMATIC, dt.getContext());
        assertEquals(dutch, dt.getSender());
        assertEquals(french, dt.getRecipient());
        assertEquals(TradeStatus.PROPOSE_TRADE, dt.getStatus());
        assertEquals(1, dt.getVersion());
        assertTrue(dt.isEmpty());
    }

    public void testMakePeaceTreaty() {
        Game game = getGame();
        Player dutch = game.getPlayerByNationId("model.nation.dutch");
        Player french = game.getPlayerByNationId("model.nation.french");

        DiplomaticTrade dt = DiplomaticTrade.makePeaceTreaty(TradeContext.DIPLOMATIC, dutch, french);

        assertEquals(Stance.PEACE, dt.getStance());
        assertEquals(1, dt.getItems().size());
    }

    public void testGetOtherPlayer() {
        Game game = getGame();
        Player dutch = game.getPlayerByNationId("model.nation.dutch");
        Player french = game.getPlayerByNationId("model.nation.french");

        DiplomaticTrade dt = new DiplomaticTrade(game, TradeContext.DIPLOMATIC, dutch, french, null, 0);

        assertEquals(french, dt.getOtherPlayer(dutch));
        assertEquals(dutch, dt.getOtherPlayer(french));
    }

    public void testSerializationRoundTrip() throws Exception {
        Game game = getGame();
        Player dutch = game.getPlayerByNationId("model.nation.dutch");
        Player french = game.getPlayerByNationId("model.nation.french");

        DiplomaticTrade dt = new DiplomaticTrade(game, TradeContext.TRIBUTE, dutch, french, null, 5);
        dt.add(new GoldTradeItem(game, dutch, french, 500));
        dt.setStatus(TradeStatus.ACCEPT_TRADE);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        FreeColXMLWriter xw = new FreeColXMLWriter(out, FreeColXMLWriter.WriteScope.toSave(), false);
        dt.toXML(xw);
        xw.close();

        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        FreeColXMLReader xr = new FreeColXMLReader(in);
        xr.nextTag();

        DiplomaticTrade dt2 = new DiplomaticTrade(game, "");
        dt2.readFromXML(xr);
        xr.close();

        assertEquals(dt.getContext(), dt2.getContext());
        assertEquals(dt.getSender(), dt2.getSender());
        assertEquals(dt.getRecipient(), dt2.getRecipient());
        assertEquals(dt.getStatus(), dt2.getStatus());
        assertEquals(dt.getVersion(), dt2.getVersion());
        assertEquals(1, dt2.getItems().size());
        assertEquals(500, dt2.getGoldGivenBy(dutch));
    }

    public void testGetGoldGivenBy() {
        Game game = getGame();
        Player dutch = game.getPlayerByNationId("model.nation.dutch");
        Player french = game.getPlayerByNationId("model.nation.french");

        DiplomaticTrade dt = new DiplomaticTrade(game, TradeContext.TRADE, dutch, french, null, 0);

        assertEquals(-1, dt.getGoldGivenBy(dutch));

        dt.add(new GoldTradeItem(game, dutch, french, 150));
        assertEquals(150, dt.getGoldGivenBy(dutch));
        assertEquals(-1, dt.getGoldGivenBy(french));
    }

    public void testInciteTradeValidation() {
        Game game = getGame();
        Player dutch = game.getPlayerByNationId("model.nation.dutch");
        Player french = game.getPlayerByNationId("model.nation.french");

        InciteTradeItem selfAttack = new InciteTradeItem(game, dutch, french, french);
        assertFalse(selfAttack.isValid());

        InciteTradeItem attackMe = new InciteTradeItem(game, dutch, french, dutch);
        assertFalse(attackMe.isValid());
    }

    public void testGoldTradeValidation() {
        Game game = getGame();
        Player dutch = game.getPlayerByNationId("model.nation.dutch");
        Player french = game.getPlayerByNationId("model.nation.french");

        GoldTradeItem negativeGold = new GoldTradeItem(game, dutch, french, -100);
        assertFalse(negativeGold.isValid());

        GoldTradeItem zeroGold = new GoldTradeItem(game, dutch, french, 0);
        assertTrue(zeroGold.isValid());
        
        dutch.modifyGold(-dutch.getGold());
        GoldTradeItem tooExpensive = new GoldTradeItem(game, dutch, french, 500);
        assertFalse(tooExpensive.isValid());
    }

    public void testColonyTradeValidation() {
        Game game = getGame();
        Map map = getTestMap();
        Player dutch = game.getPlayerByNationId("model.nation.dutch");
        Player french = game.getPlayerByNationId("model.nation.french");
        
        Tile tile = map.getTile(5, 5);
        Colony colony = new TestColony(game, dutch, "Nieuw Amsterdam", tile);
        
        try {
            new ColonyTradeItem(game, french, dutch, colony);
            fail("Constructor should block invalid ownership");
        } catch (IllegalArgumentException e) {
            // Success
        }

        ColonyTradeItem validItem = new ColonyTradeItem(game, dutch, french, colony);
        assertTrue(validItem.isValid());
    }

    public void testComplexMultiItemTrade() {
        Game game = getGame();
        Map map = getTestMap();
        Player dutch = game.getPlayerByNationId("model.nation.dutch");
        Player french = game.getPlayerByNationId("model.nation.french");

        dutch.modifyGold(1000);
        Tile colonyTile = map.getTile(10, 10);
        Colony colony = new TestColony(game, dutch, "Nieuw Amsterdam", colonyTile);
        
        DiplomaticTrade dt = new DiplomaticTrade(game, TradeContext.DIPLOMATIC, dutch, french, null, 0);
        
        dt.add(new StanceTradeItem(game, dutch, french, Stance.PEACE));
        dt.add(new StanceTradeItem(game, french, dutch, Stance.PEACE));
        dt.add(new GoldTradeItem(game, dutch, french, 500));
        dt.add(new ColonyTradeItem(game, dutch, french, colony));

        assertEquals(3, dt.getItems().size());
        assertEquals(500, dt.getGoldGivenBy(dutch));
        
        List<Colony> colonies = dt.getColoniesGivenBy(dutch);
        assertEquals(1, colonies.size());
        
        for (TradeItem item : dt.getItems()) {
            assertTrue(item.isValid());
        }
    }

    public void testTradeItemsManagement() {
        Game game = getGame();
        Player dutch = game.getPlayerByNationId("model.nation.dutch");
        Player french = game.getPlayerByNationId("model.nation.french");

        DiplomaticTrade dt = new DiplomaticTrade(game, TradeContext.TRADE, dutch, french, null, 0);

        dt.add(new GoldTradeItem(game, dutch, french, 100));
        assertEquals(1, dt.getItems().size());

        dt.add(new GoldTradeItem(game, dutch, french, 200));
        assertEquals(1, dt.getItems().size());
        assertEquals(200, dt.getGoldGivenBy(dutch));

        dt.clear();
        assertTrue(dt.isEmpty());
    }
}
