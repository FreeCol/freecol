/**
 *  Copyright (C) 2002-2016   The FreeCol Team
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

import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.NativeTrade;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.control.ChangeSet;


/**
 * A type of session to handle trading with a native settlement.
 */
public class NativeTradeSession extends Session {

    private static final Logger logger = Logger.getLogger(NativeTradeSession.class.getName());

    /** The moves the trading unit has left at start of session. */
    private final int movesLeft;

    /** Whether any action has been taken in this session. */
    private boolean actionTaken;

    /** The native trade information. */
    private NativeTrade nt;


    /**
     * Creates a new <code>NativeTradeSession</code>.
     *
     * @param unit The <code>Unit</code> that is trading.
     * @param is The <code>IndianSettlement</code> to trade with.
     */
    public NativeTradeSession(Unit unit, IndianSettlement is) {
        super(makeSessionKey(NativeTradeSession.class, unit, is));

        this.movesLeft = unit.getMovesLeft();
        this.actionTaken = false;
        this.nt = new NativeTrade(unit, is);
    }

    @Override
    public void complete(ChangeSet cs) {
        super.complete(cs);
    }

    public int getMovesLeft() {
        return this.movesLeft;
    }

    public boolean getActionTaken() {
        return this.actionTaken;
    }

    public boolean getBuy() {
        return this.nt.getBuy();
    }

    public boolean getSell() {
        return this.nt.getSell();
    }

    public boolean getGift() {
        return this.nt.getGift();
    }

    public void setBuy() {
        this.actionTaken = true;
        this.nt.setBuy(false);
    }

    public void setSell() {
        this.actionTaken = true;
        this.nt.setSell(false);
    }

    public void setGift() {
        this.actionTaken = true;
        this.nt.setGift(false);
    }

    public boolean getDone() {
        return this.nt.getDone();
    }
}
