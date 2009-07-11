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

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.HistoryEvent;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Map.CircleIterator;
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
     * @param builder The <code>Unit</code> to do the building.
     */
    public BuildColonyMessage(String colonyName, Unit builder) {
        this.colonyName = colonyName;
        this.builderId = builder.getId();
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
     * @param server The <code>FreeColServer</code> handling the request.
     * @param player The <code>Player</code> building the colony.
     * @param connection The <code>Connection</code> the message is from.
     *
     * @return An update <code>Element</code> defining the new colony
     *         and updating its surrounding tiles,
     *         or an error <code>Element</code> on failure.
     */
    public Element handle(FreeColServer server, Player player, Connection connection) {
        Game game = player.getGame();
        ServerPlayer serverPlayer = server.getPlayer(connection);
        Unit unit;

        try {
            unit = server.getUnitSafely(builderId, serverPlayer);
        } catch (Exception e) {
            return Message.clientError(e.getMessage());
        }
        if (colonyName == null || colonyName.length() == 0) {
            return Message.createError("server.buildColony.badName",
                                       "Empty colony name");
        } else if (player.getColony(colonyName) != null) {
            return Message.createError("server.buildColony.badName",
                                       "Non-unique colony name " + colonyName);
        } else if (!unit.canBuildColony()) {
            return Message.createError("server.buildColony.badUnit",
                                       "Unit " + builderId
                                       + " can not build colony " + colonyName);
        }
        Tile tile = unit.getTile();
        Colony colony = new Colony(game, serverPlayer, colonyName, tile);
        unit.buildColony(colony);
        HistoryEvent h = new HistoryEvent(game.getTurn().getNumber(),
                                          HistoryEvent.Type.FOUND_COLONY,
                                          "%colony%", colony.getName());
        player.getHistory().add(h);
        server.getInGameInputHandler().sendUpdatedTileToAll(tile, serverPlayer);

        Map map = game.getMap();
        Element reply = Message.createNewRootElement("multiple");
        Document doc = reply.getOwnerDocument();
        Element update = doc.createElement("update");
        Element history = doc.createElement("addHistory");
        reply.appendChild(update);
        reply.appendChild(history);
        update.appendChild(tile.toXMLElement(player, doc));
        // Send the surrounding tiles that are now owned by the colony
        for (Tile t : map.getSurroundingTiles(tile, colony.getRadius())) {
            if (t.getOwningSettlement() == colony) {
                update.appendChild(t.toXMLElement(player, doc));
            }
        }
        // Send any tiles that can now be seen because the colony can see
        // further than the founding unit.
        for (int range = unit.getLineOfSight() + 1; 
             range <= colony.getLineOfSight(); range++) {
            CircleIterator circle = map.getCircleIterator(tile.getPosition(),
                                                          false, range);
            while (circle.hasNext()) {
                update.appendChild(map.getTile(circle.next()).toXMLElement(player, doc));
            }
        }
        history.appendChild(h.toXMLElement(player, doc));
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
