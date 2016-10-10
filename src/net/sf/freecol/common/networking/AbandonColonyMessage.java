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

import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 * The message sent when the client requests abandoning of a colony.
 */
public class AbandonColonyMessage extends TrivialMessage {

    public static final String TAG = "abandonColony";
    private static final String COLONY_TAG = "colony";


    /**
     * Create a new {@code AbandonColonyMessage} with the specified
     * colony.
     *
     * @param colony The {@code Colony} to abandon.
     */
    public AbandonColonyMessage(Colony colony) {
        super(TAG, COLONY_TAG, colony.getId());
    }

    /**
     * Create a new {@code AbandonColonyMessage} from a supplied element.
     *
     * @param game The {@code Game} this message belongs to.
     * @param element The {@code Element} to use to create the message.
     */
    public AbandonColonyMessage(Game game, Element element) {
        super(TAG, COLONY_TAG, getStringAttribute(element, COLONY_TAG));
    }


    /**
     * Handle a "abandonColony"-message.
     *
     * @param server The {@code FreeColServer} handling the request.
     * @param player The {@code Player} abandoning the colony.
     * @param connection The {@code Connection} the message is from.
     * @return An update {@code Element} defining the new colony
     *     and updating its surrounding tiles, or an error
     *     {@code Element} on failure.
     */
    public Element handle(FreeColServer server, Player player,
                          Connection connection) {
        final ServerPlayer serverPlayer = server.getPlayer(connection);
        final String colonyId = getAttribute(COLONY_TAG);

        Colony colony;
        try {
            colony = player.getOurFreeColGameObject(colonyId, Colony.class);
        } catch (Exception e) {
            return serverPlayer.clientError(e.getMessage())
                .build(serverPlayer);
        }
        if (colony.getUnitCount() != 0) {
            return serverPlayer.clientError("Attempt to abandon colony "
                + colonyId + " with non-zero unit count "
                + Integer.toString(colony.getUnitCount()))
                .build(serverPlayer);
        }

        // Proceed to abandon
        // FIXME: Player.settlements is still being fixed on the client side.
        return server.getInGameController()
            .abandonSettlement(serverPlayer, colony)
            .build(serverPlayer);
    }
}
