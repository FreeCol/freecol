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

import java.util.logging.Logger;

import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Constants.IndianDemandAction;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.networking.ChangeSet;
import net.sf.freecol.common.networking.ChangeSet.See;
import net.sf.freecol.common.networking.CloseMessage;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.server.model.ServerPlayer;


/**
 * A type of session to handle trading with a native settlement.
 */
public class NativeDemandSession extends TimedSession {

    private static final Logger logger = Logger.getLogger(NativeDemandSession.class.getName());

    private Unit unit;
    private Colony colony;
    private GoodsType type;
    private int amount;

    
    /**
     * Creates a new {@code NativeDemandSession}.
     *
     * @param unit The {@code Unit} making the demand.
     * @param colony The {@code Colony} where the demand is made.
     * @param goodsType The {@code GoodsType} to demand.
     * @param amount The amount of goods.
     * @param timeout The amount of time to wait for a response.
     */
    public NativeDemandSession(Unit unit, Colony colony,
                               GoodsType goodsType, int amount, long timeout) {
        super(makeSessionKey(NativeDemandSession.class, unit, colony),
              timeout);

        this.unit = unit;
        this.colony = colony;
        this.type = goodsType;
        this.amount = amount;
    }


    private ServerPlayer getColonyOwner() {
        return (ServerPlayer)this.colony.getOwner();
    }

    private ServerPlayer getUnitOwner() {
        return (ServerPlayer)this.unit.getOwner();
    }
    
    /**
     * Get the game.
     *
     * @return The {@code ServerGame}.
     */
    private ServerGame getGame() {
        return (ServerGame)this.unit.getGame();
    }

    /**
     * Primitive level to finishing the session with the given result.
     *
     * @param result The result of the demand.
     * @param cs A {@code ChangeSet} to update.
     */
    private void completeInternal(boolean result, ChangeSet cs) {
        final ServerPlayer demandPlayer = getUnitOwner();
        final ServerPlayer colonyPlayer = getColonyOwner();
        
        colonyPlayer.csCompleteNativeDemand(demandPlayer,
            this.unit, this.colony, this.type, this.amount,
            ((result) ? IndianDemandAction.INDIAN_DEMAND_ACCEPT
                : IndianDemandAction.INDIAN_DEMAND_REJECT), cs);
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
        if (!ret) completeInternal(result, cs);
        return ret;
    }


    // Implement TimedSession
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean complete(boolean result) {
        ChangeSet cs = new ChangeSet();
        boolean ret = super.complete(cs);
        if (!ret) {
            completeInternal(result, cs);
            cs.add(See.only(getColonyOwner()),
                   new CloseMessage("net.sf.freecol.client.gui.dialog.NativeDemandDialog"));
            getGame().sendToAll(cs);
        }
        return ret;
    }


    // Override Session

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean complete(ChangeSet cs) {
        boolean ret = super.complete(cs);
        if (!ret) completeInternal(false, cs);
        return ret;
    }


    // Override Object

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(64);
        sb.append('[').append(getClass())
            .append(" unit=").append(this.unit)
            .append(" colony=").append(this.colony)
            .append(" goods=").append(this.amount)
            .append(' ').append(this.type.getSuffix()).append(']');
        return sb.toString();
    }
}
