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
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.MoveType;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.ai.AIPlayer;
import net.sf.freecol.server.model.ServerPlayer;


/**
 * The message sent when speaking to a chief.
 */
public class ScoutSpeakToChiefMessage extends AttributeMessage {

    public static final String TAG = "scoutSpeakToChief";
    private static final String RESULT_TAG = "result";
    private static final String SETTLEMENT_TAG = "settlement";
    private static final String UNIT_TAG = "unit";


    /**
     * Create a new {@code ScoutSpeakToChiefMessage} with the
     * supplied unit, settlement and result.
     *
     * Result is null in a request.
     *
     * @param unit The {@code Unit} that is learning.
     * @param is The {@code IndianSettlement} to talk to.
     * @param result The result of speaking.
     */
    public ScoutSpeakToChiefMessage(Unit unit, IndianSettlement is,
                                    String result) {
        super(TAG, UNIT_TAG, unit.getId(), SETTLEMENT_TAG, is.getId(),
              RESULT_TAG, result);
    }

    /**
     * Create a new {@code ScoutSpeakToChiefMessage} from a stream.
     *
     * @param game The {@code Game} this message belongs to.
     * @param xr A {@code FreeColXMLReader} to read from.
     * @exception XMLStreamException if the stream is corrupt.
     */
    public ScoutSpeakToChiefMessage(Game game, FreeColXMLReader xr)
        throws XMLStreamException {
        super(TAG, xr, UNIT_TAG, SETTLEMENT_TAG, RESULT_TAG);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean currentPlayerMessage() {
        return true;
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
        // Ignored
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clientHandler(FreeColClient freeColClient) {
        final Game game = freeColClient.getGame();
        final Unit unit = getUnit(game);
        final IndianSettlement is = getSettlement(game);
        final String result = getResult();

        igc(freeColClient).scoutSpeakToChiefHandler(unit, is, result);
        clientGeneric(freeColClient);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ChangeSet serverHandler(FreeColServer freeColServer,
                                   ServerPlayer serverPlayer) {
        final String unitId = getStringAttribute(UNIT_TAG);
        final String settlementId = getStringAttribute(SETTLEMENT_TAG);

        Unit unit;
        try {
            unit = serverPlayer.getOurFreeColGameObject(unitId, Unit.class);
        } catch (Exception e) {
            return serverPlayer.clientError(e.getMessage());
        }
        if (!unit.hasAbility(Ability.SPEAK_WITH_CHIEF)) {
            return serverPlayer.clientError("Unit lacks ability to speak to chief: "
                + unitId);
        }

        IndianSettlement is;
        try {
            is = unit.getAdjacentSettlement(settlementId,
                                            IndianSettlement.class);
        } catch (Exception e) {
            return serverPlayer.clientError(e.getMessage());
        }

        MoveType type = unit.getMoveType(is.getTile());
        if (type != MoveType.ENTER_INDIAN_SETTLEMENT_WITH_SCOUT) {
            return serverPlayer.clientError("Unable to enter "
                + is.getName() + ": " + type.whyIllegal());
        }

        // Valid request, do the scouting.
        return igc(freeColServer)
            .scoutSpeakToChief(serverPlayer, unit, is);
    }


    // Public interface

    public Unit getUnit(Game game) {
        return game.getFreeColGameObject(getStringAttribute(UNIT_TAG), Unit.class);
    }

    public IndianSettlement getSettlement(Game game) {
        return game.getFreeColGameObject(getStringAttribute(SETTLEMENT_TAG),
                                         IndianSettlement.class);
    }

    public String getResult() {
        String result = getStringAttribute(RESULT_TAG);
        return (result == null) ? "" : result;
    }
}
