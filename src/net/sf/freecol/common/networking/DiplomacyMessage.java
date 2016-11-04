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

package net.sf.freecol.common.networking;

import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.DiplomaticTrade;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.control.ChangeSet;
import net.sf.freecol.server.control.InGameController;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 * The message sent when executing a diplomatic trade.
 */
public class DiplomacyMessage extends DOMMessage {

    public static final String TAG = "diplomacy";
    private static final String OTHER_ID_TAG = "otherId";
    private static final String OUR_ID_TAG = "ourId";

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
     * Create a new {@code DiplomacyMessage}.
     *
     * @param our Our {@code FreeColGameObject} that is negotiating.
     * @param other The other {@code FreeColGameObject} to negotiate with.
     * @param agreement The {@code DiplomaticTrade} to make.
     */
    public DiplomacyMessage(FreeColGameObject our, FreeColGameObject other,
                            DiplomaticTrade agreement) {
        super(TAG);

        this.ourId = our.getId();
        this.otherId = other.getId();
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
        super(TAG);

        this.ourId = getStringAttribute(element, OUR_ID_TAG);
        this.otherId = getStringAttribute(element, OTHER_ID_TAG);
        this.agreement = getChild(game, element, 0, false, DiplomaticTrade.class);
        this.extraUnit = getChild(game, element, 1, true, Unit.class);
    }


    // Public interface

    /**
     * Get the extra {@code Unit}.
     *
     * @return The extra {@code Unit}, or null if none.
     */
    public Unit getExtraUnit() {
        return this.extraUnit;
    }

    /**
     * Get our FCGO.
     *
     * @param game The {@code Game} to extract the FCGO from.
     * @return Our {@code FreeColGameObject}.
     */
    public FreeColGameObject getOurFCGO(Game game) {
        return game.getFreeColGameObject(this.ourId);
    }

    /**
     * Get the other FCGO.
     *
     * @param game The {@code Game} to extract the FCGO from.
     * @return The other {@code FreeColGameObject}.
     */
    public FreeColGameObject getOtherFCGO(Game game) {
        return game.getFreeColGameObject(this.otherId);
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
     * Handle a "diplomacy"-message.
     *
     * @param server The {@code FreeColServer} that handles the message.
     * @param connection The {@code Connection} the message is from.
     * @return An {@code Element} describing the trade with
     *     either "accept" or "reject" status, null on trade failure,
     *     or an error {@code Element} on outright error.
     */
    public Element handle(FreeColServer server, Connection connection) {
        final ServerPlayer serverPlayer = server.getPlayer(connection);
        final Game game = serverPlayer.getGame();

        if (this.agreement == null) {
            return serverPlayer.clientError("Null diplomatic agreement.")
                .build(serverPlayer);
        }

        Unit ourUnit = null;
        Colony ourColony = null;
        FreeColGameObject our = getOurFCGO(game);
        ChangeSet cs = null;
        if (our == null) {
            cs = serverPlayer.clientError("Missing our object: " + this.ourId);
        } if (our instanceof Unit) {
            ourUnit = (Unit)our;
            if (!serverPlayer.owns(ourUnit)) {
                cs = serverPlayer.clientError("Not our unit: " + this.ourId);
            } else if (!ourUnit.hasTile()) {
                cs = serverPlayer.clientError("Our unit is not on the map: "
                    + this.ourId);
            }
        } else if (our instanceof Colony) {
            ourColony = (Colony)our;
            if (!serverPlayer.owns(ourColony)) {
                cs = serverPlayer.clientError("Not our settlement: " + this.ourId);
            }
        } else {
            cs = serverPlayer.clientError("Our object is bogus: " + our);
        }
        if (cs != null) return cs.build(serverPlayer);
        
        Unit otherUnit = null;
        Colony otherColony = null;
        Player otherPlayer = null;
        FreeColGameObject other = getOtherFCGO(game);
        if (other == null) {
            cs = serverPlayer.clientError("Missing other object: " + this.otherId);
        } else if (other instanceof Unit) {
            otherUnit = (Unit)other;
            if (serverPlayer.owns(otherUnit)) {
                cs = serverPlayer.clientError("Contacting our unit? "
                    + this.otherId);
            } else if (!otherUnit.hasTile()) {
                cs = serverPlayer.clientError("Other unit is not on the map: "
                    + this.otherId);
            } else if (ourUnit != null
                && !ourUnit.getTile().isAdjacent(otherUnit.getTile())) {
                cs = serverPlayer.clientError("Our unit " + this.ourId
                    + " is not adjacent to other unit " + this.otherId);
            } else if (ourColony != null
                && !ourColony.getTile().isAdjacent(otherUnit.getTile())) {
                cs = serverPlayer.clientError("Our colony " + this.ourId
                    + " is not adjacent to other unit " + this.otherId);
            } else {
                otherPlayer = otherUnit.getOwner();
            }
        } else if (other instanceof Colony) {
            otherColony = (Colony)other;
            if (serverPlayer.owns(otherColony)) {
                cs = serverPlayer.clientError("Contacting our colony? "
                    + this.otherId);
            } else if (ourUnit != null
                && !ourUnit.getTile().isAdjacent(otherColony.getTile())) {
                cs = serverPlayer.clientError("Our unit " + this.ourId
                    + " is not adjacent to other colony " + this.otherId);
            } else if (ourColony != null
                && !ourColony.getTile().isAdjacent(otherColony.getTile())) {
                cs = serverPlayer.clientError("Our colony " + this.ourId
                    + " is not adjacent to other colony " + this.otherId);
            } else {
                otherPlayer = otherColony.getOwner();
            }
        } else {
            cs = serverPlayer.clientError("Other object is bogus: " + other);
        }
        if (cs != null) return cs.build(serverPlayer);
        if (ourUnit == null && otherUnit == null) {
            return serverPlayer.clientError("Both units null")
                .build(serverPlayer);
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
        if (cs != null) return cs.build(serverPlayer);

        final InGameController igc = server.getInGameController();
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
        return cs.build(serverPlayer);
    }

    /**
     * Convert this DiplomacyMessage to XML.
     *
     * @return The XML representation of this message.
     */
    @Override
    public Element toXMLElement() {
        return new DOMMessage(TAG,
            OUR_ID_TAG, this.ourId,
            OTHER_ID_TAG, this.otherId)
            .add(this.agreement)
            .add(this.extraUnit).toXMLElement();
    }
}
