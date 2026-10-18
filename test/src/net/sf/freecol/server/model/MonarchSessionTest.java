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

package net.sf.freecol.server.model;

import java.util.Arrays;
import java.util.List;

import net.sf.freecol.common.model.AbstractUnit;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Monarch.MonarchAction;
import net.sf.freecol.common.networking.ChangeSet;
import net.sf.freecol.server.ServerTestHelper;
import net.sf.freecol.util.test.FreeColTestCase;

public class MonarchSessionTest extends FreeColTestCase {

    public void testRaiseTaxAccepted() {
        ServerGame game = ServerTestHelper.startServerGame(getTestMap());
        ServerPlayer dutch = getServerPlayer(game, "model.nation.dutch");

        GoodsType type = spec().getGoodsType("model.goods.cotton");
        Goods goods = new Goods(game, null, type, 10);

        MonarchSession session =
            new MonarchSession(
                dutch,
                MonarchAction.RAISE_TAX_ACT,
                7,
                goods);

        session.complete(true, new ChangeSet());

        assertEquals(7, dutch.getTax());
    }

    public void testRaiseTaxRejected() {
        ServerGame game = ServerTestHelper.startServerGame(getTestMap());
        ServerPlayer dutch = getServerPlayer(game, "model.nation.dutch");

        GoodsType type = spec().getGoodsType("model.goods.cigars");

        dutch.getMarket().getMarketData(type).setTraded(true);

        Tile tile = game.getMap().getTile(5, 5);
        Colony colony = new ServerColony(game, dutch,
            "New Amsterdam", tile);

        colony.addGoods(type, 200);

        Goods goods = new Goods(game, colony, type, 100);

        MonarchSession session =
            new MonarchSession(
                dutch,
                MonarchAction.RAISE_TAX_WAR,
                7,
                goods);

        session.complete(false, new ChangeSet());

        assertEquals(0, dutch.getTax());
        assertTrue(colony.getGoodsCount(type) < 200);
    }

    public void testRaiseTaxIgnored() {
        ServerGame game = ServerTestHelper.startServerGame(getTestMap());
        ServerPlayer dutch = getServerPlayer(game, "model.nation.dutch");

        GoodsType type = spec().getGoodsType("model.goods.tools");
        Goods goods = new Goods(game, null, type, 30);

        MonarchSession session =
            new MonarchSession(
                dutch,
                MonarchAction.RAISE_TAX_ACT,
                10,
                goods);

        assertFalse(session.complete(new ChangeSet()));
    }

    public void testMercenariesAccepted() {
        ServerGame game = ServerTestHelper.startServerGame(getTestMap());
        ServerPlayer dutch = getServerPlayer(game, "model.nation.dutch");

        List<AbstractUnit> mercs = Arrays.asList(
            new AbstractUnit("model.unit.soldier", null, 1));

        MonarchSession session =
            new MonarchSession(
                dutch,
                MonarchAction.MONARCH_MERCENARIES,
                mercs,
                200);

        ChangeSet cs = new ChangeSet();

        assertFalse(session.complete(true, cs));
        assertTrue(session.complete(true, cs));
    }

    public void testMercenariesRejected() {
        ServerGame game = ServerTestHelper.startServerGame(getTestMap());
        ServerPlayer dutch = getServerPlayer(game, "model.nation.dutch");

        int initialGold = dutch.getGold();

        List<AbstractUnit> mercs = Arrays.asList(
            new AbstractUnit("model.unit.soldier", null, 1));

        MonarchSession session =
            new MonarchSession(
                dutch,
                MonarchAction.HESSIAN_MERCENARIES,
                mercs,
                150);

        session.complete(false, new ChangeSet());

        assertEquals(initialGold, dutch.getGold());
    }

    public void testMercenariesIgnored() {
        ServerGame game = ServerTestHelper.startServerGame(getTestMap());
        ServerPlayer dutch = getServerPlayer(game, "model.nation.dutch");

        List<AbstractUnit> mercs = Arrays.asList(
            new AbstractUnit("model.unit.soldier", null, 1));

        MonarchSession session =
            new MonarchSession(
                dutch,
                MonarchAction.MONARCH_MERCENARIES,
                mercs,
                150);

        assertFalse(session.complete(new ChangeSet()));
    }

    public void testSessionCompletesOnlyOnce() {
        ServerGame game =
            ServerTestHelper.startServerGame(getTestMap());

        ServerPlayer dutch =
            getServerPlayer(game, "model.nation.dutch");

        GoodsType type =
            spec().getGoodsType("model.goods.tools");

        Goods goods =
            new Goods(game, null, type, 20);

        MonarchSession session =
            new MonarchSession(
                dutch,
                MonarchAction.RAISE_TAX_ACT,
                5,
                goods);

        ChangeSet cs = new ChangeSet();

        assertFalse(session.complete(true, cs));

        assertTrue(session.complete(true, cs));
    }

    public void testMercenarySessionState() {
        ServerGame game =
            ServerTestHelper.startServerGame(getTestMap());

        ServerPlayer dutch =
            getServerPlayer(game, "model.nation.dutch");

        AbstractUnit unit =
            new AbstractUnit(
                "model.unit.soldier",
                null,
                1);

        List<AbstractUnit> mercs =
            Arrays.asList(unit);

        MonarchSession session =
            new MonarchSession(
                dutch,
                MonarchAction.MONARCH_MERCENARIES,
                mercs,
                150);

        assertEquals(
            MonarchAction.MONARCH_MERCENARIES,
            session.getAction());

        assertEquals(
            150,
            session.getPrice());

        assertSame(
            mercs,
            session.getMercenaries());
    }

    public void testTaxSessionState() {
        ServerGame game =
            ServerTestHelper.startServerGame(getTestMap());

        ServerPlayer dutch =
            getServerPlayer(game, "model.nation.dutch");

        GoodsType type =
            spec().getGoodsType("model.goods.tools");

        Goods goods =
            new Goods(game, null, type, 20);

        MonarchSession session =
            new MonarchSession(
                dutch,
                MonarchAction.RAISE_TAX_ACT,
                5,
                goods);

        assertEquals(
            MonarchAction.RAISE_TAX_ACT,
            session.getAction());

        assertEquals(
            5,
            session.getTax());

        assertSame(
            goods,
            session.getGoods());

        assertNull(
            session.getMercenaries());

        assertEquals(
            0,
            session.getPrice());
    }

    public void testIgnoredSessionCompletesOnlyOnce() {
        ServerGame game =
            ServerTestHelper.startServerGame(getTestMap());

        ServerPlayer dutch =
            getServerPlayer(game, "model.nation.dutch");

        GoodsType type =
            spec().getGoodsType("model.goods.tools");

        Goods goods =
            new Goods(game, null, type, 10);

        MonarchSession session =
            new MonarchSession(
                dutch,
                MonarchAction.RAISE_TAX_ACT,
                5,
                goods);

        ChangeSet cs = new ChangeSet();

        assertFalse(session.complete(cs));

        assertTrue(session.complete(cs));
    }
}
