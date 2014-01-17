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

import net.sf.freecol.common.model.Ability;
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

    /** The settlement to negotiate with. */
    private String settlementId;

    /** The other unit to negotiate with. */
    private String otherUnitId;

    /** The trade to make. */
    private DiplomaticTrade agreement;


    /**
     * Create a new <code>DiplomacyMessage</code>.
     *
     * One of settlement and otherUnit must be non-null.
     *
     * @param unit The <code>Unit</code> that is negotiating.
     * @param settlement The <code>Settlement</code> to negotiate with.
     * @param otherUnit The other <code>Unit</code> to negotiate with.
     * @param agreement The <code>DiplomaticTrade</code> to make.
     */
    public DiplomacyMessage(Unit unit, Settlement settlement,
                            Unit otherUnit, DiplomaticTrade agreement) {
        super(getXMLElementTagName());

        this.unit = unit;
        this.settlementId = (settlement == null) ? null
            : settlement.getId();
        this.otherUnitId = (otherUnit == null) ? null : otherUnit.getId();
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
        super(getXMLElementTagName());

        this.settlementId = (!element.hasAttribute("settlement")) ? null
            : element.getAttribute("settlement");
        this.otherUnitId = (!element.hasAttribute("otherUnit")) ? null
            : element.getAttribute("otherUnit");
        
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


    // Public interface

    /**
     * Get the <code>Unit</code> which began this diplomatic exchange.
     *
     * @return The unit, or null if none.
     */
    public Unit getUnit() {
        return unit;
    }

    public Settlement getSettlement(Game game) {
        return game.getFreeColGameObject(settlementId, Settlement.class);
    }

    public Unit getOtherUnit(Game game) {
        return game.getFreeColGameObject(otherUnitId, Unit.class);
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
        final ServerPlayer serverPlayer = server.getPlayer(connection);
        final Game game = serverPlayer.getGame();

        if (agreement == null) {
            return DOMMessage.clientError("Null diplomatic agreement.");
        }

        Unit unit = getUnit();
        if (unit == null) {
            return DOMMessage.clientError("Missing unit in diplomacy.");
        } else if (!unit.hasTile()) {
            return DOMMessage.clientError("Unit is not on the map: "
                + unit.getId());
        } else if (!serverPlayer.owns(unit)) {
            return DOMMessage.clientError("Player does not own unit: "
                + unit.getId());
        } else {
            switch (agreement.getContext()) {
            case CONTACT:
                if (this.settlementId == null && this.otherUnitId == null) {
                    return DOMMessage.clientError("Unit lacks contact.");
                }
                break;
            case DIPLOMATIC:
                if (!unit.hasAbility(Ability.NEGOTIATE)) {
                    return DOMMessage.clientError("Unit lacks ability"
                        + " to negotiate: " + unit);
                }
                break;
            case TRADE:
                if (!unit.isCarrier()) {
                    return DOMMessage.clientError("Unit is not a carrier: "
                        + unit);
                } else if (!serverPlayer.hasAbility(Ability.TRADE_WITH_FOREIGN_COLONIES)) {
                    return DOMMessage.clientError("Unit owner lacks ability"
                        + " to trade with other Europeans: " + unit);
                }
                break;
            case TRIBUTE:
                if (!unit.isOffensiveUnit() || unit.isNaval()) {
                    return DOMMessage.clientError("Unit is not an offensive land unit: " + unit);
                }
                break;
            default:
                return DOMMessage.clientError("Bogus agreement context: "
                    + agreement.getContext());
            }
        }

        Settlement settlement = getSettlement(game);
        Unit otherUnit = getOtherUnit(game);
        Player otherPlayer = null;
        if (settlement == null) {
            if (otherUnit == null) {
                return DOMMessage.clientError("Missing other player entity in diplomacy.");
            } else {
                if (serverPlayer.owns(otherUnit)) {
                    return DOMMessage.clientError("Contacting own unit!?: "
                        + otherUnitId);
                } else if (!unit.getTile().isAdjacent(otherUnit.getTile())) {
                    return DOMMessage.clientError("Unit " + unit.getId()
                        + " is not adjacent to other unit " + otherUnitId);
                }
            }
            otherPlayer = otherUnit.getOwner();
        } else {
            if (!(settlement instanceof Colony)) {
                return DOMMessage.clientError("Settlement is not a colony: "
                    + settlement.getId());
            } else if (serverPlayer.owns(settlement)) {
                    return DOMMessage.clientError("Contacting own settlement!?: " + settlementId);
            } else if (!unit.getTile().isAdjacent(settlement.getTile())) {
                return DOMMessage.clientError("Unit " + unit.getId()
                    + " is not adjacent to settlement " + settlement.getId());
            }
            otherPlayer = settlement.getOwner();
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

        // Valid.
        return server.getInGameController()
            .diplomaticTrade(serverPlayer, unit, settlement, otherUnit,
                             agreement);
    }

    /**
     * Convert this DiplomacyMessage to XML.
     *
     * @return The XML representation of this message.
     */
    public Element toXMLElement() {
        Element result = createMessage(getXMLElementTagName());
        if (settlementId != null) {
            result.setAttribute("settlement", settlementId);
        }
        if (otherUnitId != null) {
            result.setAttribute("otherUnit", otherUnitId);
        }
        Document doc = result.getOwnerDocument();
        result.appendChild(agreement.toXMLElement(doc));
        result.appendChild(unit.toXMLElement(doc));
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
