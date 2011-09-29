/**
 *  Copyright (C) 2002-2011  The FreeCol Team
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

import java.util.logging.Logger;

import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.Monarch;
import net.sf.freecol.server.control.ChangeSet;


/**
 * A type of session to handle trading.
 */
public class TaxSession extends TransactionSession {

    private static final Logger logger = Logger.getLogger(TaxSession.class.getName());

    /** The player whose tax is being raised. */
    private ServerPlayer serverPlayer;

    /** The tax raise. */
    private int tax;

    /** The goods to use in a tea party. */
    private Goods goods;

    /** Was the tax raise accepted. */
    private boolean accepted;


    public TaxSession(Monarch monarch, ServerPlayer serverPlayer) {
        super(makeSessionKey(TaxSession.class, monarch, serverPlayer));
        this.serverPlayer = serverPlayer;
        this.tax = 0;
        this.goods = null;
        this.accepted = false;
    }

    public void complete(ChangeSet cs) {
        serverPlayer.csRaiseTax(tax, goods, accepted, cs);
        super.complete(cs);
    }

    public int getTax() {
        return tax;
    }

    public void setTax(int tax) {
        this.tax = tax;
    }

    public Goods getGoods() {
        return goods;
    }

    public void setGoods(Goods goods) {
        this.goods = goods;
    }

    public boolean getAccepted() {
        return accepted;
    }

    public void setAccepted(boolean accepted) {
        this.accepted = accepted;
    }
}
