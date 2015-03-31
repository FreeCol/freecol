/**
 *  Copyright (C) 2002-2015   The FreeCol Team
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

import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.control.ChangeSet;


/**
 * A type of session to handle trading.
 */
public class TradeSession extends TransactionSession {

    private static final Logger logger = Logger.getLogger(TradeSession.class.getName());

    /** The moves the trading unit has left at start of session. */
    private final int movesLeft;

    /** Whether any action has been taken in this session. */
    private boolean actionTaken;

    /** Whether buying is still valid in this session. */
    private boolean canBuy;

    /** Whether selling is still valid in this session. */
    private boolean canSell;

    /** Whether giving a gift is still valid in this session. */
    private boolean canGift;


    /**
     * Creates a new <code>TradeSession</code>.
     *
     * @param unit The <code>Unit</code> that is trading.
     * @param settlement The <code>Settlement</code> to trade with.
     */
    public TradeSession(Unit unit, Settlement settlement) {
        super(makeSessionKey(TradeSession.class, unit, settlement));
        movesLeft = unit.getMovesLeft();
        actionTaken = false;
        boolean atWar = settlement.getOwner().atWarWith(unit.getOwner());
        canBuy = !atWar;
        canSell = !atWar && unit.hasGoodsCargo();
        canGift = unit.hasGoodsCargo();
    }

    @Override
    public void complete(ChangeSet cs) {
        super.complete(cs);
    }

    public int getMovesLeft() {
        return movesLeft;
    }

    public boolean getActionTaken() {
        return actionTaken;
    }

    public boolean getBuy() {
        return canBuy;
    }

    public boolean getSell() {
        return canSell;
    }

    public boolean getGift() {
        return canGift;
    }

    public void setBuy() {
        actionTaken = true;
        canBuy = false;
    }

    public void setSell() {
        actionTaken = true;
        canSell = false;
    }

    public void setGift() {
        actionTaken = true;
        canGift = false;
    }
}
