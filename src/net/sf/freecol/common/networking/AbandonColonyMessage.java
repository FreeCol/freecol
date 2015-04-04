/**
 *  Copyright (C) 2002-2015   The FreeCol Team
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
public class AbandonColonyMessage extends DOMMessage {

    /** The identifier of the colony to abandon. */
    private final String colonyId;


    /**
     * Create a new <code>AbandonColonyMessage</code> with the specified
     * colony.
     *
     * @param colony The <code>Colony</code> to abandon.
     */
    public AbandonColonyMessage(Colony colony) {
        super(getXMLElementTagName());

        this.colonyId = colony.getId();
    }

    /**
     * Create a new <code>AbandonColonyMessage</code> from a supplied element.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     */
    public AbandonColonyMessage(Game game, Element element) {
        super(getXMLElementTagName());

        this.colonyId = element.getAttribute("colony");
    }


    /**
     * Handle a "abandonColony"-message.
     *
     * @param server The <code>FreeColServer</code> handling the request.
     * @param player The <code>Player</code> abandoning the colony.
     * @param connection The <code>Connection</code> the message is from.
     * @return An update <code>Element</code> defining the new colony
     *     and updating its surrounding tiles, or an error
     *     <code>Element</code> on failure.
     */
    public Element handle(FreeColServer server, Player player,
                          Connection connection) {
        final ServerPlayer serverPlayer = server.getPlayer(connection);

        Colony colony;
        try {
            colony = player.getOurFreeColGameObject(colonyId, Colony.class);
        } catch (Exception e) {
            return DOMMessage.clientError(e.getMessage());
        }
        if (colony.getUnitCount() != 0) {
            return DOMMessage.clientError("Attempt to abandon colony "
                + colonyId + " with non-zero unit count "
                + Integer.toString(colony.getUnitCount()));
        }

        // Proceed to abandon
        // FIXME: Player.settlements is still being fixed on the client side.
        return server.getInGameController()
            .abandonSettlement(serverPlayer, colony);
    }

    /**
     * Convert this AbandonColonyMessage to XML.
     *
     * @return The XML representation of this message.
     */
    @Override
    public Element toXMLElement() {
        return createMessage(getXMLElementTagName(),
            "colony", colonyId);
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "abandonColony".
     */
    public static String getXMLElementTagName() {
        return "abandonColony";
    }
}
