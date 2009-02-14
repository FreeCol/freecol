/**
 *  Copyright (C) 2002-2007  The FreeCol Team
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

import org.w3c.dom.Element;

import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;


/**
 * The message sent when the client requests building of a colony.
 */
public class BuildColonyMessage extends Message {

    /**
     * The name of the new colony.
     **/
    String colonyName;

    /**
     * The unit that is building the colony.
     */
    String builderId;


    /**
     * Create a new <code>BuildColonyMessage</code> with the supplied name
     * and building unit.
     *
     * @param colonyName The name for the new colony.
     * @param builderId The ID of the building <code>Unit</code>.
     */
    public BuildColonyMessage(String colonyName, String builderId) {
        this.colonyName = colonyName;
        this.builderId = builderId;
    }

    /**
     * Create a new <code>BuildColonyMessage</code> from a supplied element.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     */
    public BuildColonyMessage(Game game, Element element) {
        this.colonyName = element.getAttribute("name");
        this.builderId = element.getAttribute("unit");
    }

    /**
     * Handle a "buildColony"-message.
     *
     * @param connection The <code>Connection</code> the message was received on.
     * @param player The <code>Player</code> building the colony.
     * @param element The element containing the request.
     *
     * @return Null if the build is not permitted, otherwise an element
     *         defining the new colony and updating its surrounding tiles.
     * @throw IllegalStateException if there is a problem with the message
     *        arguments..
     */
    public Element handle(FreeColServer server, Player player, Connection connection) {
        Game game = player.getGame();
        ServerPlayer serverPlayer = server.getPlayer(connection);
        Unit unit;

        if (colonyName == null || colonyName.length() == 0) {
            throw new IllegalStateException("ColonyName must not be empty.");
        }
        if (player.getColony(colonyName) != null) {
            throw new IllegalStateException("Duplicate colony name.");
        }
        if (builderId == null || builderId.length() == 0) {
            throw new IllegalStateException("BuilderId must not be empty.");
        }
        if (!(game.getFreeColGameObject(builderId) instanceof Unit)) {
            throw new IllegalStateException("Not a unit ID: " + builderId);
        }
        unit = (Unit) player.getGame().getFreeColGameObject(builderId);
        if (unit.getOwner() != serverPlayer) {
            throw new IllegalStateException("Not the owner of building unit: "
                                            + builderId);
        }
        if (!unit.canBuildColony()) {
            logger.warning("BuildColony request for " + colonyName
                           + " with unit " + builderId
                           + " is not permitted!");
            return null;
        }
        Colony colony = new Colony(game, serverPlayer, colonyName, unit.getTile());
        unit.buildColony(colony);

        Element reply = Message.createNewRootElement("buildColonyConfirmed");
        reply.appendChild(colony.toXMLElement(player, reply.getOwnerDocument()));
        Element updateElement = reply.getOwnerDocument().createElement("update");
        int range = colony.getLineOfSight();
        if (range > unit.getLineOfSight()) {
            for (Tile t : game.getMap().getSurroundingTiles(unit.getTile(), range)) {
                updateElement.appendChild(t.toXMLElement(player, reply.getOwnerDocument()));
            }
        }
        updateElement.appendChild(unit.getTile().toXMLElement(player, reply.getOwnerDocument()));
        reply.appendChild(updateElement);
        server.sendUpdatedTileToAll(unit.getTile(), player);
        return reply;
    }

    /**
     * Convert this BuildColonyMessage to XML.
     *
     * @return The XML representation of this message.
     */
    public Element toXMLElement() {
        Element result = createNewRootElement(getXMLElementTagName());
        result.setAttribute("name", colonyName);
        result.setAttribute("unit", builderId);
        return result;
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "buildColony".
     */
    public static String getXMLElementTagName() {
        return "buildColony";
    }
}
