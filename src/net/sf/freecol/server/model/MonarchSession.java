/**
 *  Copyright (C) 2002-2013   The FreeCol Team
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

import java.util.List;
import java.util.logging.Logger;

import net.sf.freecol.common.model.AbstractUnit;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.ModelMessage;
import net.sf.freecol.common.model.Monarch;
import net.sf.freecol.common.model.Monarch.MonarchAction;
import net.sf.freecol.server.control.ChangeSet;
import net.sf.freecol.server.control.ChangeSet.See;


/**
 * A type of session to handle monarch actions that require response.
 */
public class MonarchSession extends TransactionSession {

    private static final Logger logger = Logger.getLogger(MonarchSession.class.getName());

    /** The player whose monarch is active. */
    private ServerPlayer serverPlayer;

    /** The action to be considered. */
    private MonarchAction action;

    /** The amount of tax to raise. */
    private int tax;

    /** The goods for the goods party. */
    private Goods goods = null;

    /** Mercenaries on offer. */
    private List<AbstractUnit> mercenaries = null;

    /** Mercenary price. */
    private int price;


    public MonarchSession(ServerPlayer serverPlayer, MonarchAction action,
                          int tax, Goods goods) {
        super(makeSessionKey(MonarchSession.class, serverPlayer.getId(), ""));

        this.serverPlayer = serverPlayer;
        this.action = action;
        this.tax = tax;
        this.goods = goods;
        this.mercenaries = null;
        this.price = 0;
    }

    public MonarchSession(ServerPlayer serverPlayer, MonarchAction action,
                          List<AbstractUnit> mercenaries, int price) {
        super(makeSessionKey(MonarchSession.class, serverPlayer.getId(), ""));

        this.serverPlayer = serverPlayer;
        this.action = action;
        this.tax = 0;
        this.goods = null;
        this.mercenaries = mercenaries;
        this.price = price;
    }

    public void complete(boolean result, ChangeSet cs) {
        switch (action) {
        case RAISE_TAX_ACT: case RAISE_TAX_WAR:
            serverPlayer.csRaiseTax(tax, goods, result, cs);
            break;
        case OFFER_MERCENARIES:
            if (result) serverPlayer.csAddMercenaries(mercenaries, price, cs);
            break;
        }
        super.complete(cs);
    }

    public void complete(ChangeSet cs) {
        switch (action) {
        case RAISE_TAX_ACT: case RAISE_TAX_WAR:
            serverPlayer.csRaiseTax(tax, goods, true, cs);
            cs.addMessage(See.only(serverPlayer),
                new ModelMessage("model.monarch.ignoredTax", serverPlayer)
                    .addAmount("%amount%", tax));
            break;
        case OFFER_MERCENARIES:
            cs.addMessage(See.only(serverPlayer),
                new ModelMessage("model.monarch.ignoredMercenaries",
                                 serverPlayer));
            break;
        }
        super.complete(cs);
    }

    public MonarchAction getAction() {
        return this.action;
    }

    public int getTax() {
        return this.tax;
    }

    public Goods getGoods() {
        return this.goods;
    }

    public List<AbstractUnit> getMercenaries() {
        return this.mercenaries;
    }

    public int getPrice() {
        return this.price;
    }
}
