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

package net.sf.freecol.common.networking;

import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.DiplomaticTrade;
import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.control.InGameController;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


/**
 * The message sent when executing a diplomatic trade.
 */
public class DiplomacyMessage extends DOMMessage {

    /**
     * The identifier of our entity that is conducting diplomacy
     * (either a unit or a settlement).
     */
    private final String ourId;

    /**
     * The identifier of the other entity to negotiate with (unit or
     * settlement).
     */
    private final String otherId;

    /** The agreement being negotiated. */
    private DiplomaticTrade agreement;

    /** An extra unit if needed (when a scout is on board a ship). */
    private Unit extraUnit;


    /**
     * Create a new <code>DiplomacyMessage</code>.
     *
     * @param our Our <code>FreeColGameObject</code> that is negotiating.
     * @param other The other <code>FreeColGameObject</code> to negotiate with.
     * @param agreement The <code>DiplomaticTrade</code> to make.
     */
    public DiplomacyMessage(FreeColGameObject our, FreeColGameObject other,
                            DiplomaticTrade agreement) {
        super(getXMLElementTagName());

        this.ourId = our.getId();
        this.otherId = other.getId();
        this.agreement = agreement;
        this.extraUnit = null;
    }

    /**
     * Create a new <code>DiplomacyMessage</code>.
     *
     * @param unit The <code>Unit</code> that is negotiating.
     * @param otherUnit The other <code>Unit</code> to negotiate with.
     * @param agreement The <code>DiplomaticTrade</code> to make.
     */
    public DiplomacyMessage(Unit unit, Unit otherUnit,
                            DiplomaticTrade agreement) {
        this((FreeColGameObject)unit, (FreeColGameObject)otherUnit, agreement);
    }

    /**
     * Create a new <code>DiplomacyMessage</code>.
     *
     * @param unit The <code>Unit</code> that is negotiating.
     * @param colony The <code>Colony</code> to negotiate with.
     * @param agreement The <code>DiplomaticTrade</code> to make.
     */
    public DiplomacyMessage(Unit unit, Colony colony,
                            DiplomaticTrade agreement) {
        this((FreeColGameObject)unit, (FreeColGameObject)colony, agreement);
    }

    /**
     * Create a new <code>DiplomacyMessage</code>.
     *
     * @param colony The <code>Colony</code> that is negotiating.
     * @param unit The <code>Unit</code> that to negotiate with.
     * @param agreement The <code>DiplomaticTrade</code> to make.
     */
    public DiplomacyMessage(Colony colony, Unit unit,
                            DiplomaticTrade agreement) {
        this((FreeColGameObject)colony, (FreeColGameObject)unit, agreement);
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

        this.ourId = element.getAttribute("ourId");
        this.otherId = element.getAttribute("otherId");
        
        NodeList nodes = element.getChildNodes();
        this.agreement = (nodes.getLength() < 1) ? null
            : new DiplomaticTrade(game, (Element)nodes.item(0));
        if (nodes.getLength() < 2) {
            this.extraUnit = null;
        } else {
            Element ue = (Element)nodes.item(1);
            String id = FreeColObject.readId(ue);
            this.extraUnit = game.getFreeColGameObject(id, Unit.class);
            if (this.extraUnit == null) this.extraUnit = new Unit(game, ue);
        }
    }


    // Public interface

    /**
     * Get the extra <code>Unit</code>.
     *
     * @return The extra <code>Unit</code>, or null if none.
     */
    public Unit getExtraUnit() {
        return this.extraUnit;
    }

    /**
     * Get our FCGO.
     *
     * @param game The <code>Game</code> to extract the FCGO from.
     * @return Our <code>FreeColGameObject</code>.
     */
    public FreeColGameObject getOurFCGO(Game game) {
        return game.getFreeColGameObject(this.ourId);
    }

    /**
     * Get the other FCGO.
     *
     * @param game The <code>Game</code> to extract the FCGO from.
     * @return The other <code>FreeColGameObject</code>.
     */
    public FreeColGameObject getOtherFCGO(Game game) {
        return game.getFreeColGameObject(this.otherId);
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
    public DiplomacyMessage setAgreement(DiplomaticTrade agreement) {
        this.agreement = agreement;
        return this;
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

        if (this.agreement == null) {
            return DOMMessage.clientError("Null diplomatic agreement.");
        }

        Unit ourUnit = null;
        Colony ourColony = null;
        FreeColGameObject our = getOurFCGO(game);
        if (our == null) {
            return DOMMessage.clientError("Missing our object: " + ourId);
        } if (our instanceof Unit) {
            ourUnit = (Unit)our;
            if (!serverPlayer.owns(ourUnit)) {
                return DOMMessage.clientError("Not our unit: " + ourId);
            } else if (!ourUnit.hasTile()) {
                return DOMMessage.clientError("Our unit is not on the map: "
                    + ourId);
            }
        } else if (our instanceof Colony) {
            ourColony = (Colony)our;
            if (!serverPlayer.owns(ourColony)) {
                return DOMMessage.clientError("Not our settlement: " + ourId);
            }
        } else {
            return DOMMessage.clientError("Our object is bogus: " + our);
        }

        Unit otherUnit = null;
        Colony otherColony = null;
        Player otherPlayer = null;
        FreeColGameObject other = getOtherFCGO(game);
        if (other == null) {
            return DOMMessage.clientError("Missing other object: " + otherId);
        } if (other instanceof Unit) {
            otherUnit = (Unit)other;
            if (serverPlayer.owns(otherUnit)) {
                return DOMMessage.clientError("Contacting our unit? " + otherId);
            } else if (!otherUnit.hasTile()) {
                return DOMMessage.clientError("Other unit is not on the map: "
                    + otherId);
            } else if (ourUnit != null
                && !ourUnit.getTile().isAdjacent(otherUnit.getTile())) {
                return DOMMessage.clientError("Our unit " + ourId
                    + " is not adjacent to other unit " + otherId);
            } else if (ourColony != null
                && !ourColony.getTile().isAdjacent(otherUnit.getTile())) {
                return DOMMessage.clientError("Our colony " + ourId
                    + " is not adjacent to other unit " + otherId);
            }
            otherPlayer = otherUnit.getOwner();
        } else if (other instanceof Colony) {
            otherColony = (Colony)other;
            if (serverPlayer.owns(otherColony)) {
                return DOMMessage.clientError("Contacting our colony? " + otherId);
            } else if (ourUnit != null
                && !ourUnit.getTile().isAdjacent(otherColony.getTile())) {
                return DOMMessage.clientError("Our unit " + ourId
                    + " is not adjacent to other colony " + otherId);
            } else if (ourColony != null
                && !ourColony.getTile().isAdjacent(otherColony.getTile())) {
                return DOMMessage.clientError("Our colony " + ourId
                    + " is not adjacent to other colony " + otherId);
            }
            otherPlayer = otherColony.getOwner();
        } else {
            return DOMMessage.clientError("Other object is bogus: " + other);
        }
        if (ourUnit == null && otherUnit == null) {
            return DOMMessage.clientError("Both units null");
        }

        Player senderPlayer = agreement.getSender();
        Player recipientPlayer = agreement.getRecipient();
        Player refPlayer = serverPlayer.getREFPlayer();
        if (senderPlayer == null) {
            return DOMMessage.clientError("Null sender in agreement.");
        } else if (recipientPlayer == null) {
            return DOMMessage.clientError("Null recipient in agreement.");
        } else if (senderPlayer != (Player)serverPlayer) {
            return DOMMessage.clientError("Sender is not our player: "
                + senderPlayer.getId());
        } else if (recipientPlayer != otherPlayer) {
            return DOMMessage.clientError("Recipient is not other player: "
                + recipientPlayer.getId());
        } else if (senderPlayer == refPlayer || recipientPlayer == refPlayer) {
            return DOMMessage.clientError("The REF does not negotiate: "
                + refPlayer.getId());
        }

        final InGameController igc = server.getInGameController();
        switch (agreement.getContext()) {
        case CONTACT:
            return (ourColony != null)
                ? igc.europeanFirstContact(serverPlayer, ourColony, otherUnit,
                                           agreement)
                : (otherUnit != null)
                ? igc.europeanFirstContact(serverPlayer, ourUnit, otherUnit,
                                           agreement)
                : igc.europeanFirstContact(serverPlayer, ourUnit, otherColony,
                                           agreement);
        case DIPLOMATIC:
            return (ourUnit != null) 
                ? ((!ourUnit.hasAbility(Ability.NEGOTIATE))
                    ? DOMMessage.clientError("Unit lacks ability"
                        + " to negotiate: " + ourUnit)
                    : (otherColony == null)
                    ? DOMMessage.clientError("Null other settlement")
                    : igc.diplomacy(serverPlayer, ourUnit, otherColony,
                                    agreement))
                : ((!otherUnit.hasAbility(Ability.NEGOTIATE))
                    ? DOMMessage.clientError("Unit lacks ability"
                        + " to negotiate: " + otherUnit)
                    : igc.diplomacy(serverPlayer, ourColony, otherUnit,
                                    agreement));
        case TRADE:
            return (ourUnit != null)
                ? ((!ourUnit.isCarrier())
                    ? DOMMessage.clientError("Unit is not a carrier: "
                        + ourUnit)
                    : (!serverPlayer.hasAbility(Ability.TRADE_WITH_FOREIGN_COLONIES))
                    ? DOMMessage.clientError("Player lacks ability"
                        + " to trade with other Europeans: " + serverPlayer)
                    : (otherColony == null)
                    ? DOMMessage.clientError("Null other settlement")
                    : igc.diplomacy(serverPlayer, ourUnit, otherColony,
                                    agreement))
                : ((!otherUnit.isCarrier())
                    ? DOMMessage.clientError("Unit is not a carrier: "
                        + otherUnit)
                    : (!otherPlayer.hasAbility(Ability.TRADE_WITH_FOREIGN_COLONIES))
                    ? DOMMessage.clientError("Player lacks ability"
                        + " to trade with other Europeans: " + otherPlayer)
                    : igc.diplomacy(serverPlayer, ourColony, otherUnit,
                                    agreement));
        case TRIBUTE:
            return (ourUnit != null)
                ? ((!ourUnit.isOffensiveUnit() || ourUnit.isNaval())
                    ? DOMMessage.clientError("Unit is not an offensive"
                        + " land unit: " + ourUnit)
                    : (otherColony == null)
                    ? DOMMessage.clientError("Null other settlement")
                    : igc.diplomacy(serverPlayer, ourUnit, otherColony,
                                    agreement))
                : ((!otherUnit.isOffensiveUnit() || otherUnit.isNaval())
                    ? DOMMessage.clientError("Unit is not an offensive"
                        + " land unit: " + otherUnit)
                    : igc.diplomacy(serverPlayer, ourColony, otherUnit,
                                    agreement));
        default:
            break;
        }
        return DOMMessage.clientError("Invalid diplomacy for "
            + agreement.getContext());
    }

    /**
     * Convert this DiplomacyMessage to XML.
     *
     * @return The XML representation of this message.
     */
    @Override
    public Element toXMLElement() {
        Element result = createMessage(getXMLElementTagName(),
            "ourId", ourId,
            "otherId", otherId);
        Document doc = result.getOwnerDocument();
        result.appendChild(agreement.toXMLElement(doc));
        if (extraUnit != null) result.appendChild(extraUnit.toXMLElement(doc));
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
