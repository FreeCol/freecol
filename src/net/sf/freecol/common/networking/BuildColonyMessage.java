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

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;


/**
 * The message sent when the client requests building of a colony.
 */
public class BuildColonyMessage extends AttributeMessage {

    public static final String TAG = "buildColony";
    private static final String NAME_TAG = "name";
    private static final String UNIT_TAG = "unit";


    /**
     * Create a new {@code BuildColonyMessage} with the supplied name
     * and building unit.
     *
     * @param colonyName The name for the new colony.
     * @param builder The {@code Unit} to do the building.
     */
    public BuildColonyMessage(String colonyName, Unit builder) {
        super(TAG, NAME_TAG, colonyName, UNIT_TAG, builder.getId());
    }

    /**
     * Create a new {@code BuildColonyMessage} from a stream.
     *
     * @param game The {@code Game} this message belongs to.
     * @param xr The {@code FreeColXMLReader} to read from.
     * @exception XMLStreamException if the stream is corrupt.
     */
    public BuildColonyMessage(Game game, FreeColXMLReader xr)
        throws XMLStreamException {
        super(TAG, xr, NAME_TAG, UNIT_TAG);
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
        return Message.MessagePriority.NORMAL;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ChangeSet serverHandler(FreeColServer freeColServer,
                                   ServerPlayer serverPlayer) {
        final Game game = freeColServer.getGame();
        final String colonyName = getStringAttribute(NAME_TAG);
        final String unitId = getStringAttribute(UNIT_TAG);

        Unit unit;
        try {
            unit = serverPlayer.getOurFreeColGameObject(unitId, Unit.class);
        } catch (Exception e) {
            return serverPlayer.clientError(e.getMessage());
        }
        if (!unit.canBuildColony()) {
            return serverPlayer.clientError("Unit " + unitId
                + " can not build colony.");
        }

        if (colonyName == null) {
            return serverPlayer.clientError("Null colony name");
        } else if (Player.ASSIGN_SETTLEMENT_NAME.equals(colonyName)) {
            ; // ok
        } else if (game.getSettlementByName(colonyName) != null) {
            return serverPlayer.clientError("Non-unique colony name "
                + colonyName);
        }

        Tile tile = unit.getTile();
        if (!serverPlayer.canClaimToFoundSettlement(tile)) {
            return serverPlayer.clientError("Can not build colony on tile "
                + tile + ": " + serverPlayer.canClaimToFoundSettlementReason(tile));
        }

        // Build can proceed.
        return igc(freeColServer)
            .buildSettlement(serverPlayer, unit, colonyName);
    }
}
