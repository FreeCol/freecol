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
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;


/**
 * The message sent when changing a unit state.
 */
public class ChangeStateMessage extends AttributeMessage {

    public static final String TAG = "changeState";
    private static final String STATE_TAG = "state";
    private static final String UNIT_TAG = "unit";


    /**
     * Create a new {@code ChangeStateMessage} with the
     * supplied unit and state.
     *
     * @param unit The {@code Unit} to change the state of.
     * @param state The new state.
     */
    public ChangeStateMessage(Unit unit, UnitState state) {
        super(TAG, UNIT_TAG, unit.getId(), STATE_TAG, String.valueOf(state));
    }

    /**
     * Create a new {@code ChangeStateMessage} from a stream.
     *
     * @param game The {@code Game} this message belongs to.
     * @param xr The {@code FreeColXMLReader} to read from.
     * @exception XMLStreamException if the stream is corrupt.
     */
    public ChangeStateMessage(Game game, FreeColXMLReader xr)
        throws XMLStreamException {
        super(TAG, xr, UNIT_TAG, STATE_TAG);
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
        final String stateString = getStringAttribute(STATE_TAG);

        Unit unit;
        try {
            unit = serverPlayer.getOurFreeColGameObject(unitId, Unit.class);
        } catch (Exception e) {
            return serverPlayer.clientError(e.getMessage());
        }
        // Do not test if it is on the map, units in Europe can change state.

        UnitState state;
        try {
            state = Enum.valueOf(UnitState.class, stateString);
        } catch (Exception e) {
            return serverPlayer.clientError(e.getMessage());
        }
        if (!unit.checkSetState(state)) {
            return serverPlayer.clientError("Unit " + unitId
                + " can not change state: " + unit.getState()
                + " -> " + stateString);
        }

        // Proceed to change.
        return igc(freeColServer)
            .changeState(serverPlayer, unit, state);
    }
}
