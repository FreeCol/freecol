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

package net.sf.freecol.common.networking;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.client.FreeColClient;
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


/**
 * The message sent when executing a diplomatic trade.
 */
public class DiplomacyMessage extends ObjectMessage {

    public static final String TAG = "diplomacy";
    private static final String OTHER_ID_TAG = "otherId";
    private static final String OUR_ID_TAG = "ourId";


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

        appendChild(agreement);
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
     * Create a new {@code DiplomacyMessage} from a stream.
     *
     * @param game The {@code Game} this message belongs to.
     * @param xr The {@code FreeColXMLReader} to read from.
     * @exception XMLStreamException on stream error.
     */
    public DiplomacyMessage(Game game, FreeColXMLReader xr)
        throws XMLStreamException {
        super(TAG, xr, OUR_ID_TAG, OTHER_ID_TAG);

        FreeColXMLReader.ReadScope rs
            = xr.replaceScope(FreeColXMLReader.ReadScope.NOINTERN);
        DiplomaticTrade agreement = null;
        Unit extraUnit = null;
        try {
            while (xr.moreTags()) {
                String tag = xr.getLocalName();
                if (DiplomaticTrade.TAG.equals(tag)) {
                    if (agreement == null) {
                        agreement = xr.readFreeColObject(game, DiplomaticTrade.class);
                    } else {
                        expected(TAG, tag);
                    }
                } else if (Unit.TAG.equals(tag)) {
                    if (extraUnit == null) {
                        extraUnit = xr.readFreeColObject(game, Unit.class);
                    } else {
                        expected(TAG, tag);
                    }
                } else {
                    expected((agreement == null) ? DiplomaticTrade.TAG : Unit.TAG,
                        tag);
                }
                xr.expectTag(tag);
            }
            xr.expectTag(TAG);
        } finally {
            xr.replaceScope(rs);
        }
        appendChild(agreement);
        appendChild(extraUnit);
    }


    /**
     * Get our FCGO.
     *
     * @param game The {@code Game} to extract the FCGO from.
     * @return Our {@code FreeColGameObject}.
     */
    private FreeColGameObject getOurFCGO(Game game) {
        return game.getFreeColGameObject(getStringAttribute(OUR_ID_TAG));
    }

    /**
     * Get the other FCGO.
     *
     * @param game The {@code Game} to extract the FCGO from.
     * @return The other {@code FreeColGameObject}.
     */
    private FreeColGameObject getOtherFCGO(Game game) {
        return game.getFreeColGameObject(getStringAttribute(OTHER_ID_TAG));
    }

    /**
     * Get the agreement (a {@code DiplomaticTrade}) in this message.
     *
     * @return The agreement in this message.
     */
    private DiplomaticTrade getAgreement() {
        return getChild(0, DiplomaticTrade.class);
    }

    /**
     * Get the extra {@code Unit}.
     *
     * @return The extra {@code Unit}, or null if none.
     */
    private Unit getExtraUnit() {
        return getChild(1, Unit.class);
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
    public void clientHandler(FreeColClient freeColClient) {
        final Game game = freeColClient.getGame();
        final DiplomaticTrade agreement = getAgreement();
        final FreeColGameObject our = getOurFCGO(game);
        final FreeColGameObject other = getOtherFCGO(game);
        final Unit extraUnit = getExtraUnit();
        
        if (our == null) {
            logger.warning("Our FCGO omitted from diplomacy message.");
            return;
        }
        if (other == null) {
            logger.warning("Other FCGO omitted from diplomacy message.");
            return;
        }
        if (extraUnit != null) extraUnit.intern();

        igc(freeColClient).diplomacyHandler(our, other, agreement);
        clientGeneric(freeColClient);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ChangeSet serverHandler(FreeColServer freeColServer,
                                   ServerPlayer serverPlayer) {
        final Game game = freeColServer.getGame();
        final DiplomaticTrade agreement = getAgreement();

        if (agreement == null) {
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

        Player senderPlayer = agreement.getSender();
        Player recipientPlayer = agreement.getRecipient();
        if (senderPlayer == null) {
            cs = serverPlayer.clientError("Null sender in agreement.");
        } else if (recipientPlayer == null) {
            cs = serverPlayer.clientError("Null recipient in agreement.");
        } else if (senderPlayer.isREF() || recipientPlayer.isREF()) {
            cs = serverPlayer.clientError("The REF does not negotiate");
        }
        if (cs != null) return cs;

        final InGameController igc = freeColServer.getInGameController();
        switch (agreement.getContext()) {
        case CONTACT:
            cs = igc.europeanFirstContact(serverPlayer, ourUnit, ourColony,
                otherUnit, otherColony, agreement);
            break;
        case DIPLOMATIC:
            cs = (ourUnit != null) 
                ? ((!ourUnit.hasAbility(Ability.NEGOTIATE))
                    ? serverPlayer.clientError("Unit lacks ability"
                        + " to negotiate: " + ourUnit)
                    : (otherColony == null)
                    ? serverPlayer.clientError("Null other settlement")
                    : igc.diplomacy(serverPlayer, ourUnit, otherColony,
                                    agreement))
                : ((!otherUnit.hasAbility(Ability.NEGOTIATE))
                    ? serverPlayer.clientError("Unit lacks ability"
                        + " to negotiate: " + otherUnit)
                    : igc.diplomacy(serverPlayer, ourColony, otherUnit,
                                    agreement));
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
                                    agreement))
                : ((!otherUnit.isCarrier())
                    ? serverPlayer.clientError("Unit is not a carrier: "
                        + otherUnit)
                    : (!otherPlayer.hasAbility(Ability.TRADE_WITH_FOREIGN_COLONIES))
                    ? serverPlayer.clientError("Player lacks ability"
                        + " to trade with other Europeans: " + otherPlayer)
                    : igc.diplomacy(serverPlayer, ourColony, otherUnit,
                                    agreement));
            break;
        case TRIBUTE:
            cs = (ourUnit != null)
                ? ((!ourUnit.isOffensiveUnit() || ourUnit.isNaval())
                    ? serverPlayer.clientError("Unit is not an offensive"
                        + " land unit: " + ourUnit)
                    : (otherColony == null)
                    ? serverPlayer.clientError("Null other settlement")
                    : igc.diplomacy(serverPlayer, ourUnit, otherColony,
                                    agreement))
                : ((!otherUnit.isOffensiveUnit() || otherUnit.isNaval())
                    ? serverPlayer.clientError("Unit is not an offensive"
                        + " land unit: " + otherUnit)
                    : igc.diplomacy(serverPlayer, ourColony, otherUnit,
                                    agreement));
            break;
        default:
            break;
        }
        if (cs == null) cs = serverPlayer.clientError("Invalid diplomacy for "
            + agreement.getContext());
        return cs;
    }
}
