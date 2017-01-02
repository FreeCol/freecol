/**
 *  Copyright (C) 2002-2017   The FreeCol Team
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
import net.sf.freecol.server.control.ChangeSet;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 * The message sent when moving a unit across the high seas.
 */
public class MoveToMessage extends AttributeMessage {

    public static final String TAG = "moveTo";
    private static final String DESTINATION_TAG = "destination";
    private static final String UNIT_TAG = "unit";


    /**
     * Create a new {@code MoveToMessage} for the supplied unit
     * and destination.
     *
     * @param unit The {@code Unit} to move.
     * @param destination The {@code Location} to move to.
     */
    public MoveToMessage(Unit unit, Location destination) {
        super(TAG, UNIT_TAG, unit.getId(),
              DESTINATION_TAG, destination.getId());
    }

    /**
     * Create a new {@code MoveToMessage} from a
     * supplied element.
     *
     * @param game The {@code Game} this message belongs to.
     * @param element The {@code Element} to use to create the message.
     */
    public MoveToMessage(Game game, Element element) {
        super(TAG, UNIT_TAG, getStringAttribute(element, UNIT_TAG),
              DESTINATION_TAG, getStringAttribute(element, DESTINATION_TAG));
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public ChangeSet serverHandler(FreeColServer freeColServer,
                                   ServerPlayer serverPlayer) {
        final Game game = serverPlayer.getGame();
        final String unitId = getAttribute(UNIT_TAG);
        final String destinationId = getAttribute(DESTINATION_TAG);

        Unit unit;
        try {
            unit = serverPlayer.getOurFreeColGameObject(unitId, Unit.class);
        } catch (Exception e) {
            return serverPlayer.clientError(e.getMessage());
        }

        Location destination = game.findFreeColLocation(destinationId);
        if (destination == null) {
            return serverPlayer.clientError("Not a location: "
                + destinationId);
        }

        // Proceed to move.
        return freeColServer.getInGameController()
            .moveTo(serverPlayer, unit, destination);
    }
}
