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
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.WorkLocation;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;


/**
 * The message sent to handle changes in work location.
 */
public class WorkMessage extends AttributeMessage {

    public static final String TAG = "work";
    private static final String UNIT_TAG = "unit";
    private static final String WORK_LOCATION_TAG = "workLocation";


    /**
     * Create a new {@code WorkMessage} for the supplied unit and
     * work location.
     *
     * @param unit The {@code Unit} to change the work location of.
     * @param workLocation The {@code WorkLocation} to change to.
     */
    public WorkMessage(Unit unit, WorkLocation workLocation) {
        super(TAG, UNIT_TAG, unit.getId(),
              WORK_LOCATION_TAG, workLocation.getId());
    }

    /**
     * Create a new {@code WorkMessage} from a stream.
     *
     * @param game The {@code Game} this message belongs to (null here).
     * @param xr The {@code FreeColXMLReader} to read from.
     * @exception XMLStreamException if the stream is corrupt.
     */
    public WorkMessage(Game game, FreeColXMLReader xr)
        throws XMLStreamException {
        super(TAG, xr, UNIT_TAG, WORK_LOCATION_TAG);
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
        final String unitId = getStringAttribute(UNIT_TAG);
        final String workLocationId = getStringAttribute(WORK_LOCATION_TAG);

        Unit unit;
        try {
            unit = serverPlayer.getOurFreeColGameObject(unitId, Unit.class);
        } catch (Exception e) {
            return serverPlayer.clientError(e.getMessage());
        }

        if (!unit.hasTile()) {
            return serverPlayer.clientError("Unit is not on the map: "
                + unitId);
        }

        Colony colony = unit.getTile().getColony();
        if (colony == null) {
            return serverPlayer.clientError("Unit is not at a colony: "
                + unitId);
        }

        WorkLocation workLocation = game
            .getFreeColGameObject(workLocationId, WorkLocation.class);
        if (workLocation == null) {
            return serverPlayer.clientError("Not a work location: "
                + workLocationId);
        } else if (workLocation.getColony() != colony) {
            return serverPlayer.clientError("Work location is not in colony"
                + colony.getId() + " where the unit is: " + workLocationId);
        } else if (!workLocation.canAdd(unit)) {
            return serverPlayer.clientError("Can not add " + unit
                + " to " + workLocation
                + ": " + workLocation.getNoAddReason(unit));
        }

        // Work.
        return igc(freeColServer)
            .work(serverPlayer, unit, workLocation);
    }
}
