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
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 * The message sent when the client requests setting a unit destination.
 */
public class SetDestinationMessage extends TrivialMessage {

    public static final String TAG = "setDestination";
    private static final String DESTINATION_TAG = "destination";
    private static final String UNIT_TAG = "unit";


    /**
     * Create a new {@code SetDestinationMessage} with the supplied unit
     * and destination.
     *
     * @param unit The {@code Unit} whose destination is to be set
     * @param destination The destination to set (may be null)
     */
    public SetDestinationMessage(Unit unit, Location destination) {
        super(TAG, UNIT_TAG, unit.getId(),
              DESTINATION_TAG, ((destination == null) ? null : destination.getId()));
    }

    /**
     * Create a new {@code SetDestinationMessage} from a supplied element.
     *
     * @param game The {@code Game} this message belongs to.
     * @param element The {@code Element} to use to create the message.
     */
    public SetDestinationMessage(Game game, Element element) {
        super(TAG, UNIT_TAG, getStringAttribute(element, UNIT_TAG),
              DESTINATION_TAG, getStringAttribute(element, DESTINATION_TAG));
    }


    /**
     * Handle a "setDestination"-message.
     *
     * @param server The {@code FreeColServer} handling the message.
     * @param connection The {@code Connection} the message is from.
     * @return An update containing the unit with the new destination,
     *     or an error {@code Element} on failure.
     */
    public Element handle(FreeColServer server, Connection connection) {
        final ServerPlayer serverPlayer = server.getPlayer(connection);
        final Game game = serverPlayer.getGame();
        final String unitId = getAttribute(UNIT_TAG);
        final String destinationId = getAttribute(DESTINATION_TAG);

        Unit unit;
        try {
            unit = serverPlayer.getOurFreeColGameObject(unitId, Unit.class);
        } catch (Exception e) {
            return serverPlayer.clientError(e.getMessage())
                .build(serverPlayer);
        }

        // destination == null is OK.
        Location destination = (destinationId == null) ? null
            : game.findFreeColLocation(destinationId);

        // Set destination
        return server.getInGameController()
            .setDestination(serverPlayer, unit, destination)
            .build(serverPlayer);
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "setDestination".
     */
    public static String getTagName() {
        return TAG;
    }
}
