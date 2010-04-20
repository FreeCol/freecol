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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import java.io.IOException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.DiplomaticTrade;
import net.sf.freecol.common.model.DiplomaticTrade.TradeStatus;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TradeItem;
import net.sf.freecol.common.model.Turn;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.MoveType;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;


/**
 * The message sent when executing a diplomatic trade.
 */
public class DiplomacyMessage extends Message {
    /**
     * A place to remember what agreements are in progress but not complete.
     */
    static List<DiplomacyMessage> savedAgreements = null;
    static Turn agreementTurn = null;

    /**
     * The id of the object doing the trading.
     */
    private String unitId;

    /**
     * The id of the settlement to negotiate with.
     */
    private String settlementId;

    /**
     * The trade to make.
     */
    private DiplomaticTrade agreement;

    /**
     * The agreement status.
     */
    private TradeStatus status;


    /**
     * Create a new <code>DiplomacyMessage</code>.
     *
     * @param unit The <code>Unit</code> that is negotiating.
     * @param settlement The <code>Settlement</code> to negotiate with.
     * @param agreement The <code>DiplomaticTrade</code> to make.
     */
    public DiplomacyMessage(Unit unit, Settlement settlement,
                            DiplomaticTrade agreement) {
        this.unitId = unit.getId();
        this.settlementId = settlement.getId();
        this.agreement = agreement;
        this.status = (agreement.isAccept()) ? TradeStatus.ACCEPT_TRADE
            : TradeStatus.PROPOSE_TRADE;
    }

    /**
     * Create a new <code>DiplomacyMessage</code> from a
     * supplied element.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     */
    public DiplomacyMessage(Game game, Element element) {
        this.unitId = element.getAttribute("unit");
        this.settlementId = element.getAttribute("settlement");
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
     * This is a helper routine to be called in-client as it blindly
     * trusts its field.
     *
     * @param element An <code>Element</code> in which to search for
     *                the unit if it is not known.
     * @return The unit, or null if none.
     */
    public Unit getUnit(Element element) {
        Game game = agreement.getGame();
        Unit unit = (Unit) game.getFreeColGameObject(unitId);
        if (unit == null && element != null) {
            NodeList nodes = element.getChildNodes();
            if (nodes.getLength() >= 2) {
                unit = new Unit(game, (Element) nodes.item(1));
            }
        }
        return unit;
    }

    /**
     * Get the <code>Settlement</code> at which a diplomatic exchange
     * happens.  This is a helper routine to be called in-client as it
     * blindly trusts all fields.
     *
     * @return The settlement, or null if none.
     */
    public Settlement getSettlement() {
        Game game = agreement.getGame();
        return (game.getFreeColGameObject(settlementId) instanceof Settlement)
            ? (Settlement) game.getFreeColGameObject(settlementId)
            : null;
    }

    /**
     * Get the name of this message's other nation as a string.
     *
     * @param player This nation's player.
     * @return The name of the other nation.
     */
    public String getOtherNationName(Player player) {
        if (agreement != null) {
            if (agreement.getRecipient() != null
                && agreement.getRecipient() != player) {
                return Messages.message(agreement.getRecipient().getNationName());
            }
            if (agreement.getSender() != null
                && agreement.getSender() != player) {
                return Messages.message(agreement.getSender().getNationName());
            }
        }
        return null;
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
     *
     * @param agreement The <code>DiplomaticTrade</code> to set.
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
     * Drop outdated agreements.
     *
     * @param turn The <code>Turn</code> to drop before.
     */
    private void flushAgreements(Turn turn) {
        if (savedAgreements == null || agreementTurn == null
            || !agreementTurn.equals(turn)) {
            savedAgreements = new ArrayList<DiplomacyMessage>();
        }
        agreementTurn = turn;
    }

    /**
     * Save an agreement for a specified turn.
     *
     * @param message The <code>DiplomacyMessage</code> to save.
     * @param turn The <code>Turn</code> of the agreement to save.
     */
    private void saveAgreement(DiplomacyMessage message, Turn turn) {
        flushAgreements(turn);
        savedAgreements.add(message);
    }

    /**
     * Try to find an agreement in the saved agreements.
     *
     * @param message The <code>DiplomacyMessage</code> to find the
     *                original for.
     * @param turn The <code>Turn</code> of the agreement to find.
     * @return A suitable message or null on failure.
     */
    private DiplomacyMessage loadAgreement(DiplomacyMessage message, Turn turn) {
        DiplomacyMessage result = null;
        flushAgreements(turn);
        for (DiplomacyMessage dm : savedAgreements) {
            if (dm.isSameTransaction(message)) {
                result = dm;
                break;
            }
        }
        if (result != null) savedAgreements.remove(result);
        return result;
    }

    /**
     * Do <code>DiplomacyMessage</code>s match enough to have come from
     * the same transaction?
     *
     * The checking of all fields is probably overkill, but provides some
     * insulation from misbehaving clients.  Note that a simple equality test
     * of the agreement items will fail as while they may well be equivalent
     * the IDs will have changed in the course of the round trip to the client
     * which will trick List.equals unless we improve mutual comparison of
     * TradeItems.
     *
     * @param message A <code>DiplomacyMessage</code> to check.
     * @return True if the message is from the same transaction.
     **/
    private boolean isSameTransaction(DiplomacyMessage message) {
        return message != null
            && message.unitId.equals(unitId)
            && message.settlementId.equals(settlementId)
            && message.agreement != null
            && message.agreement.getGame() == agreement.getGame()
            && message.agreement.getSender() == agreement.getSender()
            && message.agreement.getRecipient() == agreement.getRecipient();
    }

    /**
     * Is an incoming <code>DiplomacyMessage</code> a valid acceptance
     * of this message?
     * To be valid, the response should be equivalent to the proposal, but with
     * "accept" set.
     *
     * @param message A <code>DiplomacyMessage</code> to examine.
     * @return True if the message is a valid acceptance.
     */
    private boolean isValidAcceptance(DiplomacyMessage message) {
        return isSameTransaction(message)
            && this.status == TradeStatus.PROPOSE_TRADE
            && message.status == TradeStatus.ACCEPT_TRADE;
    }

    /**
     * Is an incoming <code>DiplomacyMessage</code> a valid counter-proposal?
     * To be valid, the response should be equivalent to the proposal,
     * but with different items to trade (alas, omitted), and be a trade
     * proposal.
     *
     * @param message A <code>DiplomacyMessage</code> to examine.
     * @return True if the message is a valid counter-proposal.
     */
    private boolean isValidCounterProposal(DiplomacyMessage message) {
        return isSameTransaction(message)
            && this.status == TradeStatus.PROPOSE_TRADE
            && message.status == TradeStatus.PROPOSE_TRADE;
    }

    /**
     * Build an "update" message for a given player, from the objects
     * that have been transferred in a successful diplomatic agreement.
     *
     * @param player The <code>Player</code> which will receive the update.
     * @param objects The List of <code>FreeColGameObject</code>s transferred.
     * @return An <code>Element</code> containing a suitable update message,
     *         or null if none is required.
     */
    private Element buildUpdate(final Player player,
                                final List<FreeColGameObject> objects,
                                Element update) {
        if (update == null) {
            update = createNewRootElement("update");
        }
        Document doc = update.getOwnerDocument();

        for (FreeColGameObject object : objects) {
            if (object == player) { // update gold and score attributes
                update.appendChild(player.toXMLElementPartial(doc, "gold", "score"));
            } else if (object instanceof Player) {
                continue; // this is not the player you are looking for
            } else if (object instanceof Colony) {
                // Just updating a Colony is not enough, we need to
                // update all its tiles.
                Colony colony = (Colony) object;
                Tile colonyTile = colony.getTile();
                Map map = colony.getGame().getMap();
                int radius = colony.getRadius();
                update.appendChild(colonyTile.toXMLElement(player, doc));
                for (Tile tile : map.getSurroundingTiles(colonyTile, radius)) {
                    update.appendChild(tile.toXMLElement(player, doc));
                }
            } else {
                update.appendChild(object.toXMLElement(player, doc));
            }
        }
        // Make sure there is something useful in the update,
        // as a pure stance change will not add anything and gold changes are
        // private.
        return (update.getChildNodes().getLength() > 0) ? update : null;
    }

    /**
     * Perform an agreed trade, create an update a player and send it.
     *
     * @param player The original <code>Player</code> that started this trade.
     * @param objects A list of objects that need updating.
     */
    private void sendUpdate(final ServerPlayer player,
                            final List<FreeColGameObject> objects) {
        Element update;
        if ((update = buildUpdate(player, objects, null)) != null) {
            try {
                player.getConnection().send(update);
            } catch (IOException e) {
                logger.warning(e.getMessage());
            }
        }
    }

    /**
     * Handle a "diplomacy"-message.
     *
     * @param server The <code>FreeColServer</code> that handles the message.
     * @param connection The <code>Connection</code> the message is from.
     * @return An <code>Element</code> describing the trade with either
     *         "accept" or "reject" status, null on trade failure,
     *         or an error <code>Element</code> on outright error.
     */
    public Element handle(FreeColServer server, Connection connection) {
        ServerPlayer serverPlayer = server.getPlayer(connection);

        Unit unit;
        try {
            unit = server.getUnitSafely(unitId, serverPlayer);
        } catch (Exception e) {
            return Message.clientError(e.getMessage());
        }
        if (unit.getTile() == null) {
            return Message.clientError("Unit is not on the map: " + unitId);
        }
        Settlement settlement;
        try {
            settlement = server.getAdjacentSettlementSafely(settlementId, unit);
        } catch (Exception e) {
            return Message.clientError(e.getMessage());
        }
        if (!(settlement instanceof Colony)) {
            return Message.clientError("Settlement is not a colony: "
                                       + settlementId);
        }
        MoveType type = unit.getSimpleMoveType(settlement.getTile());
        if (type != MoveType.ENTER_FOREIGN_COLONY_WITH_SCOUT) {
            return Message.clientError("Unable to enter "
                                       + settlement.getName()
                                       + ": " + type.whyIllegal());
        }
        if (agreement == null) {
            return Message.clientError("DiplomaticTrade with null agreement.");
        }
        if (agreement.getSender() != serverPlayer) {
            return Message.clientError("DiplomaticTrade received from player who is not the sender: " + serverPlayer.getId());
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
        if (enemyPlayer == serverPlayer.getREFPlayer()) {
            return Message.clientError("Player can not negotiate with the REF: "
                                       + serverPlayer.getId());
        }
        Connection enemyConnection = enemyPlayer.getConnection();
        if (enemyConnection == null) {
            return Message.createError("server.communicate",
                                       "Unable to communicate with the enemy.");
        }

        // Clean out continuations of existing trades
        Game game = serverPlayer.getGame();
        DiplomacyMessage response;
        switch (status) {
        case ACCEPT_TRADE:
            response = loadAgreement(this, game.getTurn());
            if (response != null && response.isValidAcceptance(this)) {
                try {
                    enemyConnection.sendAndWait(this.toXMLElement());
                } catch (IOException e) {
                    logger.warning(e.getMessage());
                }
                // Act on what was proposed, not the accept message
                // to frustrate tricksy client changing the conditions.
                List<FreeColGameObject> tradeObjects
                    = response.getAgreement().makeTrade();
                sendUpdate(enemyPlayer, tradeObjects);
                Element update = createNewRootElement("update");
                Document doc = update.getOwnerDocument();
                update.appendChild(unit.toXMLElementPartial(doc, "movesLeft"));
                return buildUpdate(serverPlayer, tradeObjects, update);
            }
            logger.warning("Accept of bogus trade.");
            this.setReject();
            // Fall through
        case REJECT_TRADE:
            response = loadAgreement(this, game.getTurn());
            try {
                enemyConnection.sendAndWait(this.toXMLElement());
            } catch (IOException e) {
                logger.warning(e.getMessage());
            }
            return null;
        case PROPOSE_TRADE: // this is/can be treated as a new proposal
            break;
        default:
            return Message.clientError("Invalid diplomacy status.");
        }

        // New trade proposal.
        TradeStatus state = TradeStatus.PROPOSE_TRADE;
        Element proposal = this.toXMLElement();
        if (unit.isOnCarrier()) {
            // If the unit is on a carrier we need to update the
            // client with it first as the diplomacy message refers to it.
            Document doc = proposal.getOwnerDocument();
            proposal.appendChild(unit.toXMLElement(null, doc));
        }
        try {
            Element reply = enemyConnection.ask(proposal);
            if (reply == null) {
                response = this;
                state = TradeStatus.REJECT_TRADE;
            } else {
                response = new DiplomacyMessage(game, reply);
                state = response.status;
            }
        } catch (IOException e) {
            logger.warning(e.getMessage());
            response = this;
            state = TradeStatus.REJECT_TRADE;
        }

        // Return an update containing the unit move change and
        // changes from the agreement if it is enacted, and
        // the current state of the agreement.
        Element result = createNewRootElement("multiple");
        Document doc = result.getOwnerDocument();
        Element update = doc.createElement("update");
        result.appendChild(update);
        unit.setMovesLeft(0);
        update.appendChild(unit.toXMLElementPartial(doc, "movesLeft"));

        switch (state) {
        case PROPOSE_TRADE:
            if (this.isValidCounterProposal(response)) {
                saveAgreement(response, game.getTurn());
            } else {
                logger.warning("Confused diplomatic counterproposal.");
                response.setReject();
            }
            break;
        case ACCEPT_TRADE:
            if (this.isValidAcceptance(response)) {
                // Again, act on the proposal
                List<FreeColGameObject> tradeObjects
                    = this.agreement.makeTrade();
                sendUpdate(enemyPlayer, tradeObjects);
                update = buildUpdate(serverPlayer, tradeObjects, update);
            } else {
                logger.warning("Confused diplomatic acceptance.");
                response.setReject();
            }
            break;
        case REJECT_TRADE:
            response.setReject();
            break;
        default:
            logger.warning("Confused diplomatic status");
            response.setReject();
            break;
        }
        result.appendChild(doc.importNode(response.toXMLElement(), true));
        return result;
    }

    /**
     * Convert this DiplomacyMessage to XML.
     *
     * @return The XML representation of this message.
     */
    public Element toXMLElement() {
        Element result = createNewRootElement(getXMLElementTagName());
        result.setAttribute("unit", unitId);
        result.setAttribute("settlement", settlementId);
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
     * @return "diplomacy".
     */
    public static String getXMLElementTagName() {
        return "diplomacy";
    }
}
