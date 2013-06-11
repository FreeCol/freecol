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

package net.sf.freecol.common.networking;

import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.DiplomaticTrade;
import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


/**
 * The message sent when executing a diplomatic trade.
 */
public class DiplomacyMessage extends DOMMessage {

    /**
     * The unit doing the trading.  Can not use just an identifier as
     * the unit might be invisible to the settlement due to being
     * aboard a carrier.
     */
    private Unit unit;

    /**
     * The settlement to negotiate with.
     */
    private Settlement settlement;

    /**
     * The trade to make.
     */
    private DiplomaticTrade agreement;


    /**
     * Create a new <code>DiplomacyMessage</code>.
     *
     * @param unit The <code>Unit</code> that is negotiating.
     * @param settlement The <code>Settlement</code> to negotiate with.
     * @param agreement The <code>DiplomaticTrade</code> to make.
     */
    public DiplomacyMessage(Unit unit, Settlement settlement,
                            DiplomaticTrade agreement) {
        this.unit = unit;
        this.settlement = settlement;
        this.agreement = agreement;
    }

    /**
     * Create a new <code>DiplomacyMessage</code> from a
     * supplied element.  The unit is supplied in case it was hidden in
     * some way, such as aboard a ship.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     */
    public DiplomacyMessage(Game game, Element element) {
        String settlementId = element.getAttribute("settlement");
        settlement = game.getFreeColGameObject(settlementId,
                                               Settlement.class);

        NodeList nodes = element.getChildNodes();
        this.agreement = (nodes.getLength() < 1) ? null
            : new DiplomaticTrade(game, (Element)nodes.item(0));
        if (nodes.getLength() < 2) {
            this.unit = null;
        } else {
            Element ue = (Element)nodes.item(1);
            String unitId = FreeColObject.readId(ue);
            this.unit = game.getFreeColGameObject(unitId, Unit.class);
            if (this.unit == null) this.unit = new Unit(game, ue);
        }
    }

    /**
     * Get the <code>Unit</code> which began this diplomatic exchange.
     *
     * @return The unit, or null if none.
     */
    public Unit getUnit() {
        return unit;
    }

    /**
     * Get the <code>Settlement</code> at which a diplomatic exchange
     * happens.
     *
     * @return The settlement, or null if none.
     */
    public Settlement getSettlement() {
        return settlement;
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

        Unit unit = getUnit();
        if (unit == null) {
            return DOMMessage.clientError("Missing unit in diplomacy.");
        } else if (unit.getTile() == null) {
            return DOMMessage.clientError("Unit is not on the map: "
                + unit.getId());
        } else if (unit.getOwner() != serverPlayer) {
            return DOMMessage.clientError("Player does not own unit: "
                + unit.getId());
        }

        Settlement settlement = getSettlement();
        if (settlement == null) {
            return DOMMessage.clientError("Missing settlement in diplomacy.");
        } else if (!(settlement instanceof Colony)) {
            return DOMMessage.clientError("Settlement is not a colony: "
                + settlement.getId());
        } else if (!unit.getTile().isAdjacent(settlement.getTile())) {
            return DOMMessage.clientError("Unit " + unit.getId()
                + " is not adjacent to settlement " + settlement.getId());
        }
        Player otherPlayer = settlement.getOwner();

        if (agreement == null) {
            return DOMMessage.clientError("Null diplomatic agreement.");
        }
        Player senderPlayer = agreement.getSender();
        Player recipientPlayer = agreement.getRecipient();
        Player refPlayer = serverPlayer.getREFPlayer();
        if (senderPlayer == null) {
            return DOMMessage.clientError("Null sender in agreement.");
        } else if (recipientPlayer == null) {
            return DOMMessage.clientError("Null recipient in agreement.");
        } else if (senderPlayer != (Player) serverPlayer) {
            return DOMMessage.clientError("Sender is not the unit owner: "
                + senderPlayer.getId());
        } else if (recipientPlayer != otherPlayer) {
            return DOMMessage.clientError("Recipient is not the settlement owner: "
                + recipientPlayer.getId());
        } else if (senderPlayer == refPlayer || recipientPlayer == refPlayer) {
            return DOMMessage.clientError("The REF does not negotiate: "
                + refPlayer.getId());
        }

        // Valid, try to trade.
        return server.getInGameController()
            .diplomaticTrade(serverPlayer, unit, settlement, agreement);
    }

    /**
     * Convert this DiplomacyMessage to XML.
     *
     * @return The XML representation of this message.
     */
    public Element toXMLElement() {
        Element result = createMessage(getXMLElementTagName(),
            "settlement", settlement.getId());
        Document doc = result.getOwnerDocument();
        result.appendChild(agreement.toXMLElement(null, doc));
        result.appendChild(unit.toXMLElement(null, doc));
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
