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
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.ai.AIPlayer;
import net.sf.freecol.server.model.ServerPlayer;


/**
 * The message sent when naming a new land.
 */
public class NewLandNameMessage extends AttributeMessage {

    public static final String TAG = "newLandName";
    private static final String NEW_LAND_NAME_TAG = "newLandName";
    private static final String UNIT_TAG = "unit";


    /**
     * Create a new {@code NewLandNameMessage} with the
     * supplied name.
     *
     * @param unit The {@code Unit} that has come ashore.
     * @param newLandName The new land name.
     */
    public NewLandNameMessage(Unit unit, String newLandName) {
        super(TAG, UNIT_TAG, unit.getId(), NEW_LAND_NAME_TAG, newLandName);
    }

    /**
     * Create a new {@code NewLandNameMessage} from a stream.
     *
     * @param game The {@code Game} to read within.
     * @param xr The {@code FreeColXMLReader} to read from.
     * @exception XMLStreamException if the stream is corrupt.
     */
    public NewLandNameMessage(Game game, FreeColXMLReader xr)
        throws XMLStreamException {
        super(TAG, xr, UNIT_TAG, NEW_LAND_NAME_TAG);
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
        final Unit unit = getUnit(aiPlayer.getPlayer());
        final String name = getNewLandName();

        aiPlayer.newLandNameHandler(unit, name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clientHandler(FreeColClient freeColClient) {
        final Unit unit = getUnit(freeColClient.getMyPlayer());
        final String defaultName = getNewLandName();

        if (defaultName == null || !unit.hasTile()) return;

        igc(freeColClient).newLandNameHandler(unit, defaultName);
        clientGeneric(freeColClient);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ChangeSet serverHandler(FreeColServer freeColServer,
                                   ServerPlayer serverPlayer) {
        Unit unit;
        try {
            unit = getUnit(serverPlayer);
        } catch (Exception e) {
            return serverPlayer.clientError(e.getMessage());
        }

        Tile tile = unit.getTile();
        if (tile == null) {
            return serverPlayer.clientError("Unit is not on the map: "
                + unit.getId());
        } else if (!tile.isLand()) {
            return serverPlayer.clientError("Unit is not in the new world: "
                + unit.getId());
        }

        String newLandName = getNewLandName();
        if (newLandName == null || newLandName.isEmpty()) {
            return serverPlayer.clientError("Empty new land name");
        }

        // Set name.
        return igc(freeColServer)
            .setNewLandName(serverPlayer, unit, newLandName);
    }


    // Public interface

    /**
     * Public accessor for the unit.
     *
     * @param player The {@code Player} who owns the unit.
     * @return The {@code Unit} of this message.
     */
    public Unit getUnit(Player player) {
        return player.getOurFreeColGameObject(getStringAttribute(UNIT_TAG), Unit.class);
    }

    /**
     * Public accessor for the new land name.
     *
     * @return The new land name of this message.
     */
    public String getNewLandName() {
        return getStringAttribute(NEW_LAND_NAME_TAG);
    }
}
