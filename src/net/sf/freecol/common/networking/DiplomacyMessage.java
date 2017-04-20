/**
 *  Copyright (C) 2002-2017   The FreeCol Team
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

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.DiplomaticTrade;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.ai.AIPlayer;
import net.sf.freecol.server.control.InGameController;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 * The message sent when executing a diplomatic trade.
 */
public class DiplomacyMessage extends ObjectMessage {

    public static final String TAG = "diplomacy";
    private static final String OTHER_ID_TAG = "otherId";
    private static final String OUR_ID_TAG = "ourId";

    /** The agreement being negotiated. */
    private DiplomaticTrade agreement = null;

    /** An extra unit if needed (when a scout is on board a ship). */
    private Unit extraUnit = null;


    /**
     * Create a new {@code DiplomacyMessage}.
     *
     * @param our Our {@code FreeColGameObject} that is negotiating.
     * @param other The other {@code FreeColGameObject} to negotiate with.
     * @param agreement The {@code DiplomaticTrade} to make.
     */
    public DiplomacyMessage(FreeColGameObject our, FreeColGameObject other,
                            DiplomaticTrade agreement) {
        super(TAG, OUR_ID_TAG, our.getId(), OTHER_ID_TAG, other.getId());

        this.agreement = agreement;
        this.extraUnit = null;
    }

    /**
     * Create a new {@code DiplomacyMessage}.
     *
     * @param unit The {@code Unit} that is negotiating.
     * @param otherUnit The other {@code Unit} to negotiate with.
     * @param agreement The {@code DiplomaticTrade} to make.
     */
    public DiplomacyMessage(Unit unit, Unit otherUnit,
                            DiplomaticTrade agreement) {
        this((FreeColGameObject)unit, (FreeColGameObject)otherUnit, agreement);
    }

    /**
     * Create a new {@code DiplomacyMessage}.
     *
     * @param unit The {@code Unit} that is negotiating.
     * @param colony The {@code Colony} to negotiate with.
     * @param agreement The {@code DiplomaticTrade} to make.
     */
    public DiplomacyMessage(Unit unit, Colony colony,
                            DiplomaticTrade agreement) {
        this((FreeColGameObject)unit, (FreeColGameObject)colony, agreement);
    }

    /**
     * Create a new {@code DiplomacyMessage}.
     *
     * @param colony The {@code Colony} that is negotiating.
     * @param unit The {@code Unit} that to negotiate with.
     * @param agreement The {@code DiplomaticTrade} to make.
     */
    public DiplomacyMessage(Colony colony, Unit unit,
                            DiplomaticTrade agreement) {
        this((FreeColGameObject)colony, (FreeColGameObject)unit, agreement);
    }

    /**
     * Create a new {@code DiplomacyMessage} from a
     * supplied element.  The unit is supplied in case it was hidden in
     * some way, such as aboard a ship.
     *
     * @param game The {@code Game} this message belongs to.
     * @param element The {@code Element} to use to create the message.
     */
    public DiplomacyMessage(Game game, Element element) {
        super(TAG, OUR_ID_TAG, getStringAttribute(element, OUR_ID_TAG),
              OTHER_ID_TAG, getStringAttribute(element, OTHER_ID_TAG));

        this.agreement = getChild(game, element, 0, false, DiplomaticTrade.class);
        this.extraUnit = getChild(game, element, 1, true, Unit.class);
    }

    /**
     * Create a new {@code DiplomacyMessage} from a stream.
     *
     * @param game The {@code Game} this message belongs to.
     * @param xr The {@code FreeColXMLReader} to read from.
     */
    public DiplomacyMessage(Game game, FreeColXMLReader xr)
        throws XMLStreamException {
        super(TAG, xr, OUR_ID_TAG, OTHER_ID_TAG);

        this.agreement = null;
        this.extraUnit = null;
        while (xr.moreTags()) {
            String tag = xr.getLocalName();
            if (DiplomaticTrade.TAG.equals(tag)) {
                if (this.agreement == null) {
                    this.agreement = xr.readFreeColObject(game, DiplomaticTrade.class);
                } else {
                    expected(TAG, tag);
                }
            } else if (Unit.TAG.equals(tag)) {
                if (this.extraUnit == null) {
                    this.extraUnit = xr.readFreeColObject(game, Unit.class);
                } else {
                    expected(TAG, tag);
                }
            } else {
                expected((this.agreement == null) ? DiplomaticTrade.TAG : Unit.TAG, tag);
            }
            xr.expectTag(tag);
        }
        xr.expectTag(TAG);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MessagePriority getPriority() {
        return Message.MessagePriority.LATE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void aiHandler(FreeColServer freeColServer, AIPlayer aiPlayer) {
        final Game game = freeColServer.getGame();
        final FreeColGameObject our = getOurFCGO(game);
        final FreeColGameObject other = getOtherFCGO(game);
        final DiplomaticTrade agreement = getAgreement();

        aiPlayer.diplomacyHandler(our, other, agreement);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ChangeSet serverHandler(FreeColServer freeColServer,
                                   ServerPlayer serverPlayer) {
        final Game game = freeColServer.getGame();

        if (this.agreement == null) {
            return serverPlayer.clientError("Null diplomatic agreement");
        }

        Unit ourUnit = null;
        Colony ourColony = null;
        FreeColGameObject our = getOurFCGO(game);
        ChangeSet cs = null;
        String ourId = getStringAttribute(OUR_ID_TAG);
        String otherId = getStringAttribute(OTHER_ID_TAG);
        if (our == null) {
            cs = serverPlayer.clientError("Missing our object: " + ourId);
        } if (our instanceof Unit) {
            ourUnit = (Unit)our;
            if (!serverPlayer.owns(ourUnit)) {
                cs = serverPlayer.clientError("Not our unit: " + ourId);
            } else if (!ourUnit.hasTile()) {
                cs = serverPlayer.clientError("Our unit is not on the map: "
                    + ourId);
            }
        } else if (our instanceof Colony) {
            ourColony = (Colony)our;
            if (!serverPlayer.owns(ourColony)) {
                cs = serverPlayer.clientError("Not our settlement: " + ourId);
            }
        } else {
            cs = serverPlayer.clientError("Our object is bogus: " + our);
        }
        if (cs != null) return cs;
        
        Unit otherUnit = null;
        Colony otherColony = null;
        Player otherPlayer = null;
        FreeColGameObject other = getOtherFCGO(game);
        if (other == null) {
            cs = serverPlayer.clientError("Missing other object: " + otherId);
        } else if (other instanceof Unit) {
            otherUnit = (Unit)other;
            if (serverPlayer.owns(otherUnit)) {
                cs = serverPlayer.clientError("Contacting our unit? "
                    + otherId);
            } else if (!otherUnit.hasTile()) {
                cs = serverPlayer.clientError("Other unit is not on the map: "
                    + otherId);
            } else if (ourUnit != null
                && !ourUnit.getTile().isAdjacent(otherUnit.getTile())) {
                cs = serverPlayer.clientError("Our unit " + ourId
                    + " is not adjacent to other unit " + otherId);
            } else if (ourColony != null
                && !ourColony.getTile().isAdjacent(otherUnit.getTile())) {
                cs = serverPlayer.clientError("Our colony " + ourId
                    + " is not adjacent to other unit " + otherId);
            } else {
                otherPlayer = otherUnit.getOwner();
            }
        } else if (other instanceof Colony) {
            otherColony = (Colony)other;
            if (serverPlayer.owns(otherColony)) {
                cs = serverPlayer.clientError("Contacting our colony? "
                    + otherId);
            } else if (ourUnit != null
                && !ourUnit.getTile().isAdjacent(otherColony.getTile())) {
                cs = serverPlayer.clientError("Our unit " + ourId
                    + " is not adjacent to other colony " + otherId);
            } else if (ourColony != null
                && !ourColony.getTile().isAdjacent(otherColony.getTile())) {
                cs = serverPlayer.clientError("Our colony " + ourId
                    + " is not adjacent to other colony " + otherId);
            } else {
                otherPlayer = otherColony.getOwner();
            }
        } else {
            cs = serverPlayer.clientError("Other object is bogus: " + other);
        }
        if (cs != null) return cs;
        if (ourUnit == null && otherUnit == null) {
            return serverPlayer.clientError("Both units null");
        }

        Player senderPlayer = this.agreement.getSender();
        Player recipientPlayer = this.agreement.getRecipient();
        if (senderPlayer == null) {
            cs = serverPlayer.clientError("Null sender in agreement.");
        } else if (recipientPlayer == null) {
            cs = serverPlayer.clientError("Null recipient in agreement.");
        } else if (senderPlayer.isREF() || recipientPlayer.isREF()) {
            cs = serverPlayer.clientError("The REF does not negotiate");
        }
        if (cs != null) return cs;

        final InGameController igc = freeColServer.getInGameController();
        switch (this.agreement.getContext()) {
        case CONTACT:
            cs = igc.europeanFirstContact(serverPlayer, ourUnit, ourColony,
                otherUnit, otherColony, this.agreement);
            break;
        case DIPLOMATIC:
            cs = (ourUnit != null) 
                ? ((!ourUnit.hasAbility(Ability.NEGOTIATE))
                    ? serverPlayer.clientError("Unit lacks ability"
                        + " to negotiate: " + ourUnit)
                    : (otherColony == null)
                    ? serverPlayer.clientError("Null other settlement")
                    : igc.diplomacy(serverPlayer, ourUnit, otherColony,
                                    this.agreement))
                : ((!otherUnit.hasAbility(Ability.NEGOTIATE))
                    ? serverPlayer.clientError("Unit lacks ability"
                        + " to negotiate: " + otherUnit)
                    : igc.diplomacy(serverPlayer, ourColony, otherUnit,
                                    this.agreement));
            break;
        case TRADE:
            cs = (ourUnit != null)
                ? ((!ourUnit.isCarrier())
                    ? serverPlayer.clientError("Unit is not a carrier: "
                        + ourUnit)
                    : (!serverPlayer.hasAbility(Ability.TRADE_WITH_FOREIGN_COLONIES))
                    ? serverPlayer.clientError("Player lacks ability"
                        + " to trade with other Europeans: " + serverPlayer)
                    : (otherColony == null)
                    ? serverPlayer.clientError("Null other settlement")
                    : igc.diplomacy(serverPlayer, ourUnit, otherColony,
                                    this.agreement))
                : ((!otherUnit.isCarrier())
                    ? serverPlayer.clientError("Unit is not a carrier: "
                        + otherUnit)
                    : (!otherPlayer.hasAbility(Ability.TRADE_WITH_FOREIGN_COLONIES))
                    ? serverPlayer.clientError("Player lacks ability"
                        + " to trade with other Europeans: " + otherPlayer)
                    : igc.diplomacy(serverPlayer, ourColony, otherUnit,
                                    this.agreement));
            break;
        case TRIBUTE:
            cs = (ourUnit != null)
                ? ((!ourUnit.isOffensiveUnit() || ourUnit.isNaval())
                    ? serverPlayer.clientError("Unit is not an offensive"
                        + " land unit: " + ourUnit)
                    : (otherColony == null)
                    ? serverPlayer.clientError("Null other settlement")
                    : igc.diplomacy(serverPlayer, ourUnit, otherColony,
                                    this.agreement))
                : ((!otherUnit.isOffensiveUnit() || otherUnit.isNaval())
                    ? serverPlayer.clientError("Unit is not an offensive"
                        + " land unit: " + otherUnit)
                    : igc.diplomacy(serverPlayer, ourColony, otherUnit,
                                    this.agreement));
            break;
        default:
            break;
        }
        if (cs == null) cs = serverPlayer.clientError("Invalid diplomacy for "
            + this.agreement.getContext());
        return cs;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeChildren(FreeColXMLWriter xw) throws XMLStreamException {
        if (this.agreement != null) this.agreement.toXML(xw);
        if (this.extraUnit != null) this.extraUnit.toXML(xw);
    }

    /**
     * Convert this DiplomacyMessage to XML.
     *
     * @return The XML representation of this message.
     */
    @Override
    public Element toXMLElement() {
        return new DOMMessage(TAG,
            OUR_ID_TAG, getStringAttribute(OUR_ID_TAG),
            OTHER_ID_TAG, getStringAttribute(OTHER_ID_TAG))
            .add(this.agreement)
            .add(this.extraUnit).toXMLElement();
    }


    // Public interface

    /**
     * Get our FCGO.
     *
     * @param game The {@code Game} to extract the FCGO from.
     * @return Our {@code FreeColGameObject}.
     */
    public FreeColGameObject getOurFCGO(Game game) {
        return game.getFreeColGameObject(getStringAttribute(OUR_ID_TAG));
    }

    /**
     * Get the other FCGO.
     *
     * @param game The {@code Game} to extract the FCGO from.
     * @return The other {@code FreeColGameObject}.
     */
    public FreeColGameObject getOtherFCGO(Game game) {
        return game.getFreeColGameObject(getStringAttribute(OTHER_ID_TAG));
    }

    /**
     * Get the agreement (a {@code DiplomaticTrade}) in this message.
     *
     * @return The agreement in this message.
     */
    public DiplomaticTrade getAgreement() {
        return this.agreement;
    }

    /**
     * Set the agreement (a {@code DiplomaticTrade}) in this message.
     *
     * @param agreement The {@code DiplomaticTrade} to set.
     * @return This message.
     */
    public DiplomacyMessage setAgreement(DiplomaticTrade agreement) {
        this.agreement = agreement;
        return this;
    }

    /**
     * Get the extra {@code Unit}.
     *
     * @return The extra {@code Unit}, or null if none.
     */
    public Unit getExtraUnit() {
        return this.extraUnit;
    }
}
