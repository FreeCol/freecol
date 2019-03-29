/**
 *  Copyright (C) 2002-2019   The FreeCol Team
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
import net.sf.freecol.common.model.Monarch.MonarchAction;
import net.sf.freecol.common.networking.ChangeSet;


/**
 * A type of session to handle monarch actions that require response.
 */
public class MonarchSession extends Session {

    private static final Logger logger = Logger.getLogger(MonarchSession.class.getName());

    /** The player whose monarch is active. */
    private final ServerPlayer serverPlayer;

    /** The action to be considered. */
    private final MonarchAction action;

    /** The amount of tax to raise. */
    private final int tax;

    /** The goods for the goods party. */
    private Goods goods = null;

    /** Mercenaries on offer. */
    private List<AbstractUnit> mercenaries = null;

    /** Mercenary price. */
    private final int price;


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


    /**
     * Primitive level to finishing the session with the given result.
     *
     * @param result The result of the session, null means ignored.
     * @param cs A {@code ChangeSet} to update.
     */
    private void completeInternal(Boolean result, ChangeSet cs) {
        switch (action) {
        case RAISE_TAX_ACT: case RAISE_TAX_WAR:
            if (result == null) {
                serverPlayer.ignoreTax(tax, goods, cs);
            } else {
                serverPlayer.csRaiseTax(tax, goods, result, cs);
            }
            break;
        case MONARCH_MERCENARIES: case HESSIAN_MERCENARIES:
            if (result == null) {
                serverPlayer.ignoreMercenaries(cs);
            } else if (result) {
                serverPlayer.csAddMercenaries(mercenaries, price, null, cs);
            }
            break;
        default:
            break;
        }
    }

    /**
     * Explicit completion of the session with a given result.
     *
     * Called from the controller when the player returns a definite response.
     *
     * @param result Whether to accept or reject the demand.
     * @param cs A {@code ChangeSet} to update.
     * @return Whether the session was already complete.
     */
    public boolean complete(boolean result, ChangeSet cs) {
        boolean ret = super.complete(cs);
        if (!ret) {
            completeInternal(result, cs);
        }
        return ret;
    }

    @Override
    public boolean complete(ChangeSet cs) {
        boolean ret = super.complete(cs);
        if (!ret) {
            completeInternal(null, cs);
        }
        return ret;
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
