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

import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 * The message sent when changing a unit state.
 */
public class ChangeStateMessage extends DOMMessage {

    public static final String TAG = "changeState";
    private static final String STATE_TAG = "state";
    private static final String UNIT_TAG = "unit";

    /** The identifier of the unit to change. */
    private final String unitId;

    /** The state as a string. */
    private final String stateString;


    /**
     * Create a new <code>ChangeStateMessage</code> with the
     * supplied unit and state.
     *
     * @param unit The <code>Unit</code> to change the state of.
     * @param state The new state.
     */
    public ChangeStateMessage(Unit unit, UnitState state) {
        super(getTagName());

        this.unitId = unit.getId();
        this.stateString = String.valueOf(state);
    }

    /**
     * Create a new <code>ChangeStateMessage</code> from a
     * supplied element.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     */
    public ChangeStateMessage(Game game, Element element) {
        super(getTagName());

        this.unitId = getStringAttribute(element, UNIT_TAG);
        this.stateString = getStringAttribute(element, STATE_TAG);
    }


    /**
     * Handle a "changeState"-message.
     *
     * @param server The <code>FreeColServer</code> handling the message.
     * @param player The <code>Player</code> the message applies to.
     * @param connection The <code>Connection</code> message was received on.
     * @return An update containing the changed unit, or an error
     *     <code>Element</code> on failure.
     */
    public Element handle(FreeColServer server, Player player,
                          Connection connection) {
        final ServerPlayer serverPlayer = server.getPlayer(connection);

        Unit unit;
        try {
            unit = player.getOurFreeColGameObject(this.unitId, Unit.class);
        } catch (Exception e) {
            return serverPlayer.clientError(e.getMessage())
                .build(serverPlayer);
        }
        // Do not test if it is on the map, units in Europe can change state.

        UnitState state;
        try {
            state = Enum.valueOf(UnitState.class, this.stateString);
        } catch (Exception e) {
            return serverPlayer.clientError(e.getMessage())
                .build(serverPlayer);
        }
        if (!unit.checkSetState(state)) {
            return serverPlayer.clientError("Unit " + this.unitId
                + " can not change state: " + unit.getState().toString()
                + " -> " + this.stateString)
                .build(serverPlayer);
        }

        // Proceed to change.
        return server.getInGameController()
            .changeState(serverPlayer, unit, state)
            .build(serverPlayer);
    }

    /**
     * Convert this ChangeStateMessage to XML.
     *
     * @return The XML representation of this message.
     */
    @Override
    public Element toXMLElement() {
        return new DOMMessage(getTagName(),
            UNIT_TAG, this.unitId,
            STATE_TAG, this.stateString).toXMLElement();
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "changeState".
     */
    public static String getTagName() {
        return TAG;
    }
}
