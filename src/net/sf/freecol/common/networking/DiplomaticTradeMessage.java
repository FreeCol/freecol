/**
 *  Copyright (C) 2002-2007  The FreeCol Team
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

package net.sf.freecol.common.networking;

import java.io.IOException;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.DiplomaticTrade;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TradeItem;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;


/**
 * The message sent when executing a diplomatic trade.
 */
public class DiplomaticTradeMessage extends Message {
    /**
     * The id of the object doing the trading.
     */
    private String unitId;

    /**
     * The direction the trader is looking.
     */
    private String directionString;

    /**
     * The trade to make.
     */
    private DiplomaticTrade agreement;

    /**
     * A type for the agreement status.
     */
    public static enum TradeStatus {
        PROPOSE_TRADE,
        ACCEPT_TRADE,
        REJECT_TRADE
    }
    /**
     * The agreement status.
     */
    private TradeStatus status;


    /**
     * Create a new <code>DiplomaticTradeMessage</code> with the
     * supplied unit, direction and agreement.
     *
     * @param unit The <code>Unit</code> that is spying.
     * @param direction The <code>Direction</code> the unit is looking.
     * @param agreement The <code>DiplomaticTrade</code> to make.
     */
    public DiplomaticTradeMessage(Unit unit, Direction direction,
                                  DiplomaticTrade agreement) {
        this.unitId = unit.getId();
        this.directionString = String.valueOf(direction);
        this.agreement = agreement;
        this.status = TradeStatus.PROPOSE_TRADE;
    }

    /**
     * Create a new <code>DiplomaticTradeMessage</code> from a
     * supplied element.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     */
    public DiplomaticTradeMessage(Game game, Element element) {
        this.unitId = element.getAttribute("unit");
        this.directionString = element.getAttribute("direction");
        NodeList nodes = element.getChildNodes();
        this.agreement = (nodes.getLength() < 1) ? null
            : new DiplomaticTrade(game, (Element) nodes.item(0));
        this.status = TradeStatus.PROPOSE_TRADE;
        String statusString = element.getAttribute("status");
        if (statusString != null) {
            if (statusString.equals("accept")) {
                this.status = TradeStatus.ACCEPT_TRADE;
            } else if (statusString.equals("reject")) {
                this.status = TradeStatus.REJECT_TRADE;
            }
        }
    }

    /**
     * Get the <code>Unit</code> which began this diplomatic exchange.
     * This is a helper routine to be called in-client as it blindly trusts its field.
     *
     * @return The unit, or null if none.
     */
    public Unit getUnit() {
        try {
            return (Unit) agreement.getGame().getFreeColGameObject(unitId);
        } catch (Exception e) {
        }
        return null;
    }

    /**
     * Get the <code>Settlement</code> at which a diplomatic exchange happens.
     * This is a helper routine to be called in-client as it blindly trusts all fields.
     *
     * @return The settlement, or null if none.
     */
    public Settlement getSettlement() {
        try {
            Game game = agreement.getGame();
            Unit unit = (Unit) game.getFreeColGameObject(unitId);
            Direction direction = Enum.valueOf(Direction.class, directionString);
            Tile tile = game.getMap().getNeighbourOrNull(direction, unit.getTile());
            return tile.getSettlement();
        } catch (Exception e) {
        }
        return null;
    }

    /**
     * Get the name of this message's sender nation as a string.
     *
     * @return The name of the sender nation.
     */
    public String getOtherNationName() {
        return (agreement == null || agreement.getRecipient() == null) ? null
            : agreement.getRecipient().getNationAsString();
    }

    /**
     * Get the agreement (a <code>DiplomaticTrade</code>) in this message.
     *
     * @return The agreement in this message.
     */
    public DiplomaticTrade getAgreement() {
        return agreement;
    }

    /**
     * Set the agreement (a <code>DiplomaticTrade</code>) in this message.
     */
    public void setAgreement(DiplomaticTrade agreement) {
        this.agreement = agreement;
    }

    /**
     * Does this message indicate agreement to the trade?
     *
     * @return True if the agreement has been agreed to.
     */
    public boolean isAccept() {
        return status == TradeStatus.ACCEPT_TRADE;
    }

    /**
     * Mark a trade as accepted.
     */
    public void setAccept() {
        status = TradeStatus.ACCEPT_TRADE;
    }

    /**
     * Does this message indicate rejection of the trade?
     *
     * @return True if the agreement has been rejected.
     */
    public boolean isReject() {
        return status == TradeStatus.REJECT_TRADE;
    }

    /**
     * Mark a trade as rejected.
     */
    public void setReject() {
        status = TradeStatus.REJECT_TRADE;
    }

    /**
     * Is an incoming <code>DiplomaticTradeMessage</code> a valid acceptance
     * of this message?
     * To be valid, the response should be equivalent to the proposal, but with
     * "accept" set.
     * The checking of all fields is probably overkill, but provides some
     * insulation from misbehaving clients.  Note that a simple equality test
     * of the agreement items will fail as while they may well be equivalent
     * the IDs will have changed in the course of the round trip to the client
     * which will trick List.equals unless we improve mutual comparison of
     * TradeItems.
     *
     * @param response A <code>DiplomaticTradeMessage</code> to examine.
     *
     * @return True if the response is a valid acceptance.
     */
    private boolean isValidAcceptance(DiplomaticTradeMessage response) {
        return response != null
            && response.unitId.equals(unitId)
            && response.directionString.equals(directionString)
            && response.agreement != null
            && response.agreement.getGame() == agreement.getGame()
            && response.agreement.getSender() == agreement.getSender()
            && response.agreement.getRecipient() == agreement.getRecipient()
            && response.status == TradeStatus.ACCEPT_TRADE;
    }

    /**
     * Is an incoming <code>DiplomaticTradeMessage</code> a valid
     * counter-proposal to this message?
     * To be valid, the response should be equivalent to the proposal but
     * with sender and recipient exchanged and different items to trade.
     * The checking of all fields is probably overkill, but provides some
     * insulation from misbehaving clients.
     *
     * @param response A <code>DiplomaticTradeMessage</code> to examine.
     *
     * @return True if the response is a valid counter-proposal.
     */
    private boolean isValidCounterProposal(DiplomaticTradeMessage response) {
        return response != null
            && response.unitId.equals(unitId)
            && response.directionString.equals(directionString)
            && response.agreement != null
            && response.agreement.getGame() == agreement.getGame()
            && response.agreement.getSender() == agreement.getSender()
            && response.agreement.getRecipient() == agreement.getRecipient()
            && response.status == TradeStatus.PROPOSE_TRADE;
    }

    /**
     * Handle a "diplomaticTrade"-message.
     *
     * @param server The <code>FreeColServer</code> that is handling the message.
     * @param connection The <code>Connection</code> the message was received on.
     *
     * @return An <code>Element</code> describing the trade with either
     *         "accept" or "reject" status, null on trade failure,
     *         or an error <code>Element</code> on outright error.
     */
    public Element handle(FreeColServer server, Connection connection) {
        ServerPlayer serverPlayer = server.getPlayer(connection);
        Game game = serverPlayer.getGame();
        Unit unit;

        try {
            unit = server.getUnitSafely(unitId, serverPlayer);
        } catch (Exception e) {
            return Message.clientError(e.getMessage());
        }
        if (unit.getTile() == null) {
            return Message.clientError("Unit is not on the map: " + unitId);
        }
        Direction direction = Enum.valueOf(Direction.class, directionString);
        Tile newTile = game.getMap().getNeighbourOrNull(direction, unit.getTile());
        if (newTile == null) {
            return Message.clientError("Could not find tile"
                                       + " in direction: " + direction
                                       + " from unit: " + unitId);
        }
        Settlement settlement = newTile.getSettlement();
        if (settlement == null || !(settlement instanceof Colony)) {
            return Message.clientError("There is no colony at: "
                                       + newTile.getId());
        }
        if (agreement == null) {
            return Message.clientError("DiplomaticTrade with null agreement.");
        }
        if (agreement.getSender() != serverPlayer) {
            return Message.clientError("DiplomaticTrade received from player who is not the sender: " + serverPlayer.getId());
        }
        if (status != TradeStatus.PROPOSE_TRADE || agreement.isAccept()) {
            return Message.clientError("DiplomaticTrade must start with a proposed trade!");
        }
        ServerPlayer enemyPlayer = (ServerPlayer) agreement.getRecipient();
        if (enemyPlayer == null) {
            return Message.clientError("DiplomaticTrade recipient is null");
        }
        if (enemyPlayer == serverPlayer) {
            return Message.clientError("DiplomaticTrade recipient matches sender: "
                                       + serverPlayer.getId());
        }
        Player settlementPlayer = settlement.getOwner();
        if (settlementPlayer != (Player) enemyPlayer) {
            return Message.clientError("DiplomaticTrade recipient: " + enemyPlayer.getId()
                                       + " does not match Settlement owner: " + settlementPlayer);
        }

        if (!serverPlayer.hasContacted(settlementPlayer)) {
            serverPlayer.setContacted(settlementPlayer, true);
            settlementPlayer.setContacted(serverPlayer, true);
        }
        unit.setMovesLeft(0);
        // Loop sending proposal and counter proposal until one side rejects
        // (null reply), or accepts (agreement is marked as accepted),
        // or failure.
        ServerPlayer sender = serverPlayer;
        ServerPlayer recipient = enemyPlayer;
        DiplomaticTradeMessage request = this;
        Element reply;
        for (;;) {
            try {
                reply = recipient.getConnection().ask(request.toXMLElement());
            } catch (IOException e) {
                return Message.createError("server.communicate", e.getMessage());
            }
            DiplomaticTradeMessage response = new DiplomaticTradeMessage(game, reply);
            switch (response.status) {
            case PROPOSE_TRADE:
                if (request.isValidCounterProposal(response)) {
                    // Recipient replied with a valid counter-proposal,
                    // swap and loop
                    request = response;
                    ServerPlayer tmp = sender;
                    sender = recipient;
                    recipient = tmp;
                    continue;
                }
                logger.warning("Confused diplomatic counterproposal.");
                break;
            case ACCEPT_TRADE:
                if (request.isValidAcceptance(response)) {
                    try {
                        // Use the successful *request*, as we do not check
                        // that the client did not mess with the items
                        // agreed to!
                        request.setAccept();
                        sender.getConnection().send(request.toXMLElement());
                    } catch (IOException e) {
                        return Message.createError("server.communicate", e.getMessage());
                    }
                    request.getAgreement().makeTrade();
                    return null;
                }
                logger.warning("Confused diplomatic acceptance.");
                break;
            case REJECT_TRADE:
                break;
            }
            break; // only loop on successful counter proposal
        }
        // Tell the last player who asked that the offer was rejected
        request.setReject();
        try {
            sender.getConnection().send(request.toXMLElement());
        } catch (IOException e) {
            // ignore failure, it is safe for the client to do nothing
        }
        return null;
    }

    /**
     * Convert this DiplomaticTradeMessage to XML.
     *
     * @return The XML representation of this message.
     */
    public Element toXMLElement() {
        Element result = createNewRootElement(getXMLElementTagName());
        result.setAttribute("unit", unitId);
        result.setAttribute("direction", directionString);
        switch (status) {
        case PROPOSE_TRADE:result.setAttribute("status", ""); break;
        case ACCEPT_TRADE: result.setAttribute("status", "accept"); break;
        case REJECT_TRADE: result.setAttribute("status", "reject"); break;
        }
        result.appendChild(agreement.toXMLElement(null, result.getOwnerDocument()));
        return result;
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "diplomaticTrade".
     */
    public static String getXMLElementTagName() {
        return "diplomaticTrade";
    }
}
