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
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 * The message sent when moving a unit across the high seas.
 */
public class MoveToMessage extends DOMMessage {

    public static final String TAG = "moveTo";
    private static final String DESTINATION_TAG = "destination";
    private static final String UNIT_TAG = "unit";

    /** The identifier of the object to be moved. */
    private final String unitId;

    /** The identifier of the destination to be moved to. */
    private final String destinationId;


    /**
     * Create a new <code>MoveToMessage</code> for the supplied unit
     * and destination.
     *
     * @param unit The <code>Unit</code> to move.
     * @param destination The <code>Location</code> to move to.
     */
    public MoveToMessage(Unit unit, Location destination) {
        super(getTagName());

        this.unitId = unit.getId();
        this.destinationId = destination.getId();
    }

    /**
     * Create a new <code>MoveToMessage</code> from a
     * supplied element.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     */
    public MoveToMessage(Game game, Element element) {
        super(getTagName());

        this.unitId = getStringAttribute(element, UNIT_TAG);
        this.destinationId = getStringAttribute(element, DESTINATION_TAG);
    }


    /**
     * Handle a "moveTo"-message.
     *
     * @param server The <code>FreeColServer</code> handling the message.
     * @param player The <code>Player</code> the message applies to.
     * @param connection The <code>Connection</code> message was received on.
     * @return An update containing the moved unit, or an error
     *     <code>Element</code> on failure.
     */
    public Element handle(FreeColServer server, Player player,
                          Connection connection) {
        final ServerPlayer serverPlayer = server.getPlayer(connection);
        final Game game = player.getGame();

        Unit unit;
        try {
            unit = player.getOurFreeColGameObject(this.unitId, Unit.class);
        } catch (Exception e) {
            return serverPlayer.clientError(e.getMessage())
                .build(serverPlayer);
        }

        Location destination = game.findFreeColLocation(this.destinationId);
        if (destination == null) {
            return serverPlayer.clientError("Not a location: "
                + this.destinationId)
                .build(serverPlayer);
        }

        // Proceed to move.
        return server.getInGameController()
            .moveTo(serverPlayer, unit, destination)
            .build(serverPlayer);
    }

    /**
     * Convert this MoveToMessage to XML.
     *
     * @return The XML representation of this message.
     */
    @Override
    public Element toXMLElement() {
        return new DOMMessage(getTagName(),
            UNIT_TAG, this.unitId,
            DESTINATION_TAG, this.destinationId).toXMLElement();
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "moveTo".
     */
    public static String getTagName() {
        return TAG;
    }
}
