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

    private static class SpyMonarchSession extends MonarchSession {
        Boolean capturedResult = null;
        MonarchAction capturedAction = null;

        SpyMonarchSession(ServerPlayer sp, MonarchAction action, int tax, Goods goods) {
            super(sp, action, tax, goods);
        }

        SpyMonarchSession(ServerPlayer sp, MonarchAction action, List<AbstractUnit> mercs, int price) {
            super(sp, action, mercs, price);
        }

        @Override
        public boolean complete(boolean result, ChangeSet cs) {
            this.capturedResult = result;
            this.capturedAction = getAction();
            return super.complete(result, cs);
        }

        @Override
        public boolean complete(ChangeSet cs) {
            this.capturedResult = null;
            this.capturedAction = getAction();
            return super.complete(cs);
        }
    }

    public void testRaiseTaxAccepted() {
        ServerGame game = ServerTestHelper.startServerGame(getTestMap());
        ServerPlayer dutch = getServerPlayer(game, "model.nation.dutch");
        int taxIncrease = 7;
        GoodsType type = spec().getGoodsType("model.goods.cotton");
        Goods goods = new Goods(game, null, type, 10);

        SpyMonarchSession session = new SpyMonarchSession(dutch, MonarchAction.RAISE_TAX_ACT, taxIncrease, goods);
        ChangeSet cs = new ChangeSet();
        session.complete(true, cs);

        assertEquals(Boolean.TRUE, session.capturedResult);
        assertEquals(7, dutch.getTax());
    }

    public void testRaiseTaxRejected() {
        ServerGame game = ServerTestHelper.startServerGame(getTestMap());
        ServerPlayer dutch = getServerPlayer(game, "model.nation.dutch");
        GoodsType type = spec().getGoodsType("model.goods.cigars");

        dutch.getMarket().getMarketData(type).setTraded(true);

        Tile tile = game.getMap().getTile(5, 5);
        Colony colony = new ServerColony(game, dutch, "New Amsterdam", tile);
        colony.addGoods(type, 200);

        Goods goods = new Goods(game, colony, type, 100);

        SpyMonarchSession session = new SpyMonarchSession(dutch, MonarchAction.RAISE_TAX_WAR, 7, goods);
        ChangeSet cs = new ChangeSet();
        session.complete(false, cs);

        assertEquals(Boolean.FALSE, session.capturedResult);
        assertEquals(0, dutch.getTax());
        assertTrue(colony.getGoodsCount(type) < 200);
    }

    public void testRaiseTaxIgnored() {
        ServerGame game = ServerTestHelper.startServerGame(getTestMap());
        ServerPlayer dutch = getServerPlayer(game, "model.nation.dutch");
        GoodsType type = spec().getGoodsType("model.goods.tools");
        Goods goods = new Goods(game, null, type, 30);

        SpyMonarchSession session = new SpyMonarchSession(dutch, MonarchAction.RAISE_TAX_ACT, 10, goods);
        ChangeSet cs = new ChangeSet();
        session.complete(cs);

        assertNull(session.capturedResult);
        assertEquals(MonarchAction.RAISE_TAX_ACT, session.capturedAction);
    }

    public void testMercenariesAccepted() {
        ServerGame game = ServerTestHelper.startServerGame(getTestMap());
        ServerPlayer dutch = getServerPlayer(game, "model.nation.dutch");
        AbstractUnit unit = new AbstractUnit("model.unit.soldier", null, 1);
        List<AbstractUnit> mercs = Arrays.asList(unit);

        SpyMonarchSession session = new SpyMonarchSession(dutch, MonarchAction.MONARCH_MERCENARIES, mercs, 200);
        ChangeSet cs = new ChangeSet();
        session.complete(true, cs);

        assertEquals(Boolean.TRUE, session.capturedResult);
        assertEquals(MonarchAction.MONARCH_MERCENARIES, session.capturedAction);
    }

    public void testMercenariesRejected() {
        ServerGame game = ServerTestHelper.startServerGame(getTestMap());
        ServerPlayer dutch = getServerPlayer(game, "model.nation.dutch");
        AbstractUnit unit = new AbstractUnit("model.unit.soldier", null, 1);
        List<AbstractUnit> mercs = Arrays.asList(unit);

        SpyMonarchSession session = new SpyMonarchSession(dutch, MonarchAction.HESSIAN_MERCENARIES, mercs, 150);
        ChangeSet cs = new ChangeSet();
        session.complete(false, cs);

        assertEquals(Boolean.FALSE, session.capturedResult);
        assertEquals(MonarchAction.HESSIAN_MERCENARIES, session.capturedAction);
    }

    public void testMercenariesIgnored() {
        ServerGame game = ServerTestHelper.startServerGame(getTestMap());
        ServerPlayer dutch = getServerPlayer(game, "model.nation.dutch");
        AbstractUnit unit = new AbstractUnit("model.unit.soldier", null, 1);
        List<AbstractUnit> mercs = Arrays.asList(unit);

        SpyMonarchSession session = new SpyMonarchSession(dutch, MonarchAction.MONARCH_MERCENARIES, mercs, 150);
        ChangeSet cs = new ChangeSet();
        session.complete(cs);

        assertNull(session.capturedResult);
        assertEquals(MonarchAction.MONARCH_MERCENARIES, session.capturedAction);
    }
}
