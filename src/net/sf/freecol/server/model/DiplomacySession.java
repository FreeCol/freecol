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

import net.sf.freecol.common.model.DiplomaticTrade;
import net.sf.freecol.common.model.DiplomaticTrade.TradeStatus;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.networking.DiplomacyMessage;
import net.sf.freecol.common.networking.TrivialMessage;
import net.sf.freecol.server.control.ChangeSet;
import net.sf.freecol.server.control.ChangeSet.See;


/**
 * A type of session to handle diplomacy.
 */
public class DiplomacySession extends TimedSession {

    private static final Logger logger = Logger.getLogger(DiplomacySession.class.getName());

    /** The agreement under consideration. */
    private DiplomaticTrade agreement;

    /** The initiating unit. */
    private final Unit unit;

    /** The other player's settlement. */
    private final Settlement settlement;

    /** The other player's unit (only non-null in first contact cases). */
    private final Unit otherUnit;


    /**
     * Start a new diplomacy session with our unit and another player
     * settlement.
     *
     * @param unit The {@code Unit} that is initiating diplomacy.
     * @param settlement The {@code Settlement} that is contacted.
     * @param timeout The amount of time to wait for a response.
     */
    public DiplomacySession(Unit unit, Settlement settlement, long timeout) {
        super(makeSessionKey(DiplomacySession.class, unit, settlement),
              timeout);

        this.agreement = null;
        this.unit = unit;
        this.settlement = settlement;
        this.otherUnit = null;
    }

    /**
     * Start a new diplomacy session with our unit and another player unit.
     *
     * @param unit The {@code Unit} that is initiating diplomacy.
     * @param otherUnit The other {@code Unit} that is contacted.
     * @param timeout The amount of time to wait for a response.
     */
    public DiplomacySession(Unit unit, Unit otherUnit, long timeout) {
        super(makeSessionKey(DiplomacySession.class, unit, otherUnit),
              timeout);
        this.agreement = null;
        this.unit = unit;
        this.settlement = null;
        this.otherUnit = otherUnit;
    }


    public DiplomaticTrade getAgreement() {
        return this.agreement;
    }

    public void setAgreement(DiplomaticTrade agreement) {
        this.agreement = agreement;
    }
    
    public Unit getUnit() {
        return this.unit;
    }

    public Settlement getSettlement() {
        return this.settlement;
    }

    public Unit getOtherUnit() {
        return this.otherUnit;
    }

    private ServerPlayer getOwner() {
        return (ServerPlayer)this.unit.getOwner();
    }

    private ServerGame getGame() {
        return (ServerGame)this.unit.getGame();
    }

    private DiplomacyMessage getMessage() {
        return (this.otherUnit == null)
            ? new DiplomacyMessage(this.unit, this.settlement, this.agreement)
            : new DiplomacyMessage(this.unit, this.otherUnit, this.agreement);
    }
    
    /**
     * Primitive level to finishing the session with the given result.
     *
     * @param result The result of the session.
     * @param cs A {@code ChangeSet} to update.
     */
    private void completeInternal(boolean result, ChangeSet cs) {
        if (this.agreement != null) { // Agreement is null in first contact
            if (result) {
                result = getGame().csAcceptTrade(this.agreement, this.unit,
                                                 this.settlement, cs);
            }
            this.agreement.setStatus((result) ? TradeStatus.ACCEPT_TRADE
                                              : TradeStatus.REJECT_TRADE);
            cs.add(See.only((ServerPlayer)this.agreement.getSender()),
                   ChangeSet.ChangePriority.CHANGE_LATE, getMessage());
            cs.add(See.only((ServerPlayer)this.agreement.getRecipient()),
                   ChangeSet.ChangePriority.CHANGE_LATE, getMessage());
        }
        this.unit.setMovesLeft(0);
        cs.add(See.only(getOwner()), this.unit);
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


    // Implement TimedSession
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean complete(boolean result) {
        ChangeSet cs = new ChangeSet();
        boolean ret = complete(false, cs);
        if (!ret) { // Withdraw offer
            cs.add(See.only((ServerPlayer)this.agreement.getSender()),
                ChangeSet.ChangePriority.CHANGE_NORMAL,
                TrivialMessage.CLOSE_MENUS_MESSAGE);
            cs.add(See.only((ServerPlayer)this.agreement.getRecipient()),
                ChangeSet.ChangePriority.CHANGE_NORMAL,
                TrivialMessage.CLOSE_MENUS_MESSAGE);
        }
        getGame().sendToAll(cs);
        return ret;
    }


    // Override Session

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean complete(ChangeSet cs) {
        return complete(false, cs);
    }
}
