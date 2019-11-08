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
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Direction;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;
import net.sf.freecol.server.model.ServerUnit;


/**
 * The message sent when embarking.
 */
public class EmbarkMessage extends AttributeMessage {

    public static final String TAG = "embark";
    private static final String CARRIER_TAG = "carrier";
    private static final String DIRECTION_TAG = "direction";
    private static final String UNIT_TAG = "unit";


    /**
     * Create a new {@code EmbarkMessage} with the
     * supplied unit, carrier and optional direction.
     *
     * @param unit The {@code Unit} to embark.
     * @param carrier The carrier {@code Unit} to embark on.
     * @param direction An option direction to embark in.
     */
    public EmbarkMessage(Unit unit, Unit carrier, Direction direction) {
        super(TAG, UNIT_TAG, unit.getId(), CARRIER_TAG, carrier.getId(),
              DIRECTION_TAG, (direction == null) ? null : String.valueOf(direction));
    }

    /**
     * Create a new {@code EmbarkMessage} from a stream.
     *
     * @param game The {@code Game} this message belongs to.
     * @param xr The {@code FreeColXMLReader} to read from.
     * @exception XMLStreamException if the stream is corrupt.
     */
    public EmbarkMessage(Game game, FreeColXMLReader xr)
        throws XMLStreamException {
        super(TAG, xr, UNIT_TAG, CARRIER_TAG, DIRECTION_TAG);
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
        final String unitId = getStringAttribute(UNIT_TAG);
        final String carrierId = getStringAttribute(CARRIER_TAG);
        final String directionString = getStringAttribute(DIRECTION_TAG);

        ServerUnit unit;
        try {
            unit = serverPlayer.getOurFreeColGameObject(unitId, ServerUnit.class);
        } catch (Exception e) {
            return serverPlayer.clientError(e.getMessage());
        }

        Unit carrier;
        try {
            carrier = serverPlayer.getOurFreeColGameObject(carrierId, Unit.class);
        } catch (Exception e) {
            return serverPlayer.clientError(e.getMessage());
        }

        Location sourceLocation = unit.getLocation();
        if (directionString == null) {
            // Locations must be the same, or the source is also a
            // carrier in the same location as the carrier, or they
            // must be on the same tile.
            if (!carrier.isAtLocation(sourceLocation)) {
                return serverPlayer.clientError("Unit " + unitId
                    + " at " + sourceLocation.getId()
                    + " and carrier " + carrierId
                    + " at " + carrier.getLocation().getId()
                    + " are not co-located.");
            }
        } else {
            // Units have to be on the map and have moves left if a
            // move is involved.
            if (unit.getMovesLeft() <= 0) {
                return serverPlayer.clientError("Unit has no moves left: "
                    + unitId);
            }

            Tile destinationTile = null;
            try {
                destinationTile = unit.getNeighbourTile(directionString);
            } catch (Exception e) {
                return serverPlayer.clientError(e.getMessage());
            }
            if (carrier.getTile() != destinationTile) {
                return serverPlayer.clientError("Carrier: " + carrierId
                    + " is not at destination tile: " + destinationTile);
            }
        }

        // Proceed to embark
        return igc(freeColServer)
            .embarkUnit(serverPlayer, unit, carrier);
    }
}
