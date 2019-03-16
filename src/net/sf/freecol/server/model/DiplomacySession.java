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

import java.util.function.Predicate;
import java.util.logging.Logger;

import net.sf.freecol.common.model.DiplomaticTrade;
import net.sf.freecol.common.model.DiplomaticTrade.TradeStatus;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Ownable;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.networking.ChangeSet;
import net.sf.freecol.common.networking.ChangeSet.See;
import net.sf.freecol.common.networking.CloseMessage;
import net.sf.freecol.common.networking.DiplomacyMessage;
import net.sf.freecol.common.networking.Message;
import static net.sf.freecol.common.util.CollectionUtils.*;


/**
 * A type of session to handle diplomacy.
 *
 * Diplomacy may involve human players, so it *must* be a TimedSession.
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
        super(makeDiplomacySessionKey(unit, settlement), timeout);

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
        super(makeDiplomacySessionKey(unit, otherUnit), timeout);

        this.agreement = null;
        this.unit = unit;
        this.settlement = null;
        this.otherUnit = otherUnit;
    }

    /**
     * Make a diplomacy session key for the given ownables.
     *
     * @param o1 The first {@code Ownable}.
     * @param o2 The second {@code Ownable}.
     * @return A diplomacy session key.
     */
    public static String makeDiplomacySessionKey(Ownable o1, Ownable o2) {
        return makeSessionKey(DiplomacySession.class,
                              o1.getOwner(), o2.getOwner());
    }


    /**
     * Get the game this session is in.
     *
     * @return The enclosing {@code ServerGame}.
     */
    private ServerGame getGame() {
        return (ServerGame)this.unit.getGame();
    }

    /**
     * Get the session agreement.
     *
     * @return The {@code DiplomaticTrade} under negotiation.
     */
    public DiplomaticTrade getAgreement() {
        return this.agreement;
    }

    /**
     * Set the session agreement.
     *
     * @param agreement The new {@code DiplomaticTrade} to negotiate.
     */
    public void setAgreement(DiplomaticTrade agreement) {
        this.agreement = agreement;
    }
    
    /**
     * Get the initiating unit.
     *
     * @return The {@code Unit} that started the session.
     */
    public Unit getUnit() {
        return this.unit;
    }

    /**
     * Get the settlement if any.
     *
     * @return The {@code Settlement} that is negotiating.
     */
    public Settlement getSettlement() {
        return this.settlement;
    }

    /**
     * Get the other unit if any.
     *
     * @return The other {@code Unit} that is negotiating.
     */
    public Unit getOtherUnit() {
        return this.otherUnit;
    }

    /**
     * Get the owner of the initiating unit.
     *
     * @return The {@code ServerPlayer} that owns the initiating unit.
     */
    private ServerPlayer getOwner() {
        return (ServerPlayer)this.unit.getOwner();
    }

    /**
     * Get the owner of the other settlement or unit.
     *
     * @return The {@code ServerPlayer} that owns the other settlement or unit.
     */
    private ServerPlayer getOtherPlayer() {
        return (ServerPlayer)((this.settlement != null)
            ? this.settlement.getOwner()
            : this.otherUnit.getOwner());
    }

    /**
     * Utility to find the other player in this session from a given one.
     *
     * @param serverPlayer The {@code ServerPlayer} *not* to find.
     * @return The other {@code ServerPlayer}.
     */
    public ServerPlayer getOtherPlayer(ServerPlayer serverPlayer) {
        ServerPlayer other = getOwner();
        return (other != serverPlayer) ? other : getOtherPlayer();
    }
        
    /**
     * Utility to create a message using the current session parameters and
     * trade agreement, given a desired destination player.
     *
     * @param destination The {@code Player} to send the message to.
     * @return A new {@code DiplomacyMessage} for the destination player.
     */
    public DiplomacyMessage getMessage(Player destination) {
        return (destination.owns(this.unit))
            ? ((this.otherUnit == null)
                ? new DiplomacyMessage(this.unit, this.settlement, this.agreement)
                : new DiplomacyMessage(this.unit, this.otherUnit, this.agreement))
            : ((this.otherUnit == null)
                ? new DiplomacyMessage(this.settlement, this.unit, this.agreement)
                : new DiplomacyMessage(this.otherUnit, this.unit, this.agreement));
    }

    /**
     * Was this session started by the given objects?
     *
     * @param fcgo1 The first {@code FreeColGameObject}.
     * @param fcgo2 The second {@code FreeColGameObject}.
     * @return True if the objects started this session.
     */
    public boolean isCompatible(FreeColGameObject fcgo1,
                                FreeColGameObject fcgo2) {
        return (fcgo1 == (FreeColGameObject)this.unit
            && (fcgo2 == (FreeColGameObject)this.settlement
                || fcgo2 == (FreeColGameObject)this.otherUnit))
            || (fcgo2 == (FreeColGameObject)this.unit
                && (fcgo1 == (FreeColGameObject)this.settlement
                    || fcgo1 == (FreeColGameObject)this.otherUnit));
    }
    
    /**
     * Find any contact session already underway between the owners of
     * the given units.
     *
     * @param unit The first {@code Unit}.
     * @param other The second {@code Unit}.
     * @return Any {@code DiplomacySession} found.
     */
    public static DiplomacySession findContactSession(Unit unit, Unit other) {
        return findContactSession(unit.getOwner(), other.getOwner());
    }

    /**
     * Find any contact session already underway between the owners of a 
     * given unit and settlement.
     *
     * @param unit The {@code Unit}.
     * @param settlement The {@code Settlement}.
     * @return Any {@code DiplomacySession} found.
     */
    public static DiplomacySession findContactSession(Unit unit,
                                                      Settlement settlement) {
        return findContactSession(unit.getOwner(), settlement.getOwner());
    }

    /**
     * Find any contact session already underway between the given players.
     *
     * @param p1 The first {@code Player}.
     * @param p2 The second {@code Player}.
     * @return Any {@code DiplomacySession} found.
     */
    private static DiplomacySession findContactSession(Player p1, Player p2) {
        final Predicate<Session> pred = s -> (s instanceof DiplomacySession)
            && ((DiplomacySession)s).getAgreement() != null
            && (((DiplomacySession)s).getAgreement().getContext()
                == DiplomaticTrade.TradeContext.CONTACT)
            && ((((DiplomacySession)s).getOwner() == p1
                    && ((DiplomacySession)s).getOtherPlayer() == p2)
                || (((DiplomacySession)s).getOwner() == p2
                    && ((DiplomacySession)s).getOtherPlayer() == p1));
        return (DiplomacySession)findSession(pred);
    }

    /**
     * Primitive level to finishing the session with the given result.
     *
     * @param result The result of the session.
     * @param cs A {@code ChangeSet} to update.
     */
    private void completeInternal(boolean result, ChangeSet cs) {
        logger.info("Completing diplomacy session: " + this);
        if (this.agreement != null) { // Agreement is null in first contact
            if (result) {
                result = getGame().csAcceptTrade(this.agreement, this.unit,
                                                 this.settlement, cs);
            }
            this.agreement.setStatus((result) ? TradeStatus.ACCEPT_TRADE
                                              : TradeStatus.REJECT_TRADE);
            Player sp = this.agreement.getSender();
            cs.add(See.only(sp), getMessage(sp));
            Player rp = this.agreement.getRecipient();
            cs.add(See.only(rp), getMessage(rp));
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
        final String dialogName = "net.sf.freecol.client.gui.dialog.NegotiationDialog";
        ChangeSet cs = new ChangeSet();
        boolean ret = complete(false, cs);
        if (!ret) { // Withdraw offer
            cs.add(See.only(this.agreement.getSender()),
                   new CloseMessage(dialogName));
            cs.add(See.only(this.agreement.getRecipient()),
                   new CloseMessage(dialogName));
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
