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
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 * The message sent when a unit joins a colony.
 */
public class JoinColonyMessage extends DOMMessage {

    /** The identifier of the colony. */
    private final String colonyId;

    /** The identifier of the unit that is building the colony. */
    private final String builderId;


    /**
     * Create a new <code>JoinColonyMessage</code> with the supplied name
     * and building unit.
     *
     * @param colony a <code>Colony</code> value
     * @param builder The <code>Unit</code> to do the building.
     */
    public JoinColonyMessage(Colony colony, Unit builder) {
        super(getXMLElementTagName());

        this.colonyId = colony.getId();
        this.builderId = builder.getId();
    }

    /**
     * Create a new <code>JoinColonyMessage</code> from a supplied element.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     */
    public JoinColonyMessage(Game game, Element element) {
        super(getXMLElementTagName());

        this.colonyId = element.getAttribute("colony");
        this.builderId = element.getAttribute("unit");
    }


    /**
     * Handle a "joinColony"-message.
     *
     * @param server The <code>FreeColServer</code> handling the request.
     * @param player The <code>Player</code> building the colony.
     * @param connection The <code>Connection</code> the message is from.
     * @return An update <code>Element</code> defining the new colony
     *     and updating its surrounding tiles, or an error
     *     <code>Element</code> on failure.
     */
    public Element handle(FreeColServer server, Player player,
                          Connection connection) {
        final ServerPlayer serverPlayer = server.getPlayer(connection);

        Unit unit;
        try {
            unit = player.getOurFreeColGameObject(builderId, Unit.class);
        } catch (Exception e) {
            return DOMMessage.clientError(e.getMessage());
        }

        Colony colony;
        try {
            colony = player.getOurFreeColGameObject(colonyId, Colony.class);
        } catch (Exception e) {
            return DOMMessage.clientError(e.getMessage());
        }

        // Try to buy.
        return server.getInGameController()
            .joinColony(serverPlayer, unit, colony);
    }

    /**
     * Convert this JoinColonyMessage to XML.
     *
     * @return The XML representation of this message.
     */
    @Override
    public Element toXMLElement() {
        return createMessage(getXMLElementTagName(),
            "colony", colonyId,
            "unit", builderId);
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "joinColony".
     */
    public static String getXMLElementTagName() {
        return "joinColony";
    }
}
