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

import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.HistoryEvent;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Region;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.control.InGameController;
import net.sf.freecol.server.model.ServerPlayer;


/**
 * The message sent when naming a new region.
 */
public class NewRegionNameMessage extends Message {
    /**
     * The new name.
     */
    private String newRegionName;

    /**
     * The ID of the unit that discovered the region.
     */
    private String unitId;

    /**
     * Create a new <code>NewRegionNameMessage</code> with the
     * supplied name.
     *
     * @param newRegionName The new region name.
     */
    public NewRegionNameMessage(String newRegionName, Unit unit) {
        this.newRegionName = newRegionName;
        this.unitId = unit.getId();
    }

    /**
     * Create a new <code>NewRegionNameMessage</code> from a
     * supplied element.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     */
    public NewRegionNameMessage(Game game, Element element) {
        this.newRegionName = element.getAttribute("newRegionName");
        this.unitId = element.getAttribute("unit");
    }

    /**
     * Handle a "newRegionName"-message.
     *
     * @param server The <code>FreeColServer</code> handling the message.
     * @param player The <code>Player</code> the message applies to.
     * @param connection The <code>Connection</code> message was received on.
     *
     * @return An update containing the newRegionNamed unit,
     *         or an error <code>Element</code> on failure.
     */
    public Element handle(FreeColServer server, Player player,
                          Connection connection) {
        ServerPlayer serverPlayer = server.getPlayer(connection);
        Game game = server.getGame();
        Unit unit;
        try {
            unit = server.getUnitSafely(unitId, serverPlayer);
        } catch (Exception e) {
            return Message.clientError(e.getMessage());
        }
        Tile tile = unit.getTile();
        if (tile == null) {
            return Message.clientError("Unit is not on the map: " + unitId);
        }
        Region region = tile.getDiscoverableRegion();
        if (region == null) {
            return Message.clientError("No discoverable region for: " + unitId);
        }
        if (region.isPacific()) {
            return Message.clientError("Can not rename the Pacific!");
        }

        // Do the discovery
        InGameController controller = (InGameController) server.getController();
        HistoryEvent h = region.discover(serverPlayer,
                                         serverPlayer.getGame().getTurn(),
                                         newRegionName);
        controller.sendUpdateToAll(serverPlayer, (FreeColObject) region);

        // Reply, updating the region and history.
        Element reply = Message.createNewRootElement("multiple");
        Document doc = reply.getOwnerDocument();
        Element update = doc.createElement("update");
        reply.appendChild(update);
        update.appendChild(region.toXMLElement(player, doc));
        Element history = doc.createElement("addHistory");
        reply.appendChild(history);
        h.addToOwnedElement(history, player);
        return reply;
    }

    /**
     * Convert this NewRegionNameMessage to XML.
     *
     * @return The XML representation of this message.
     */
    public Element toXMLElement() {
        Element result = createNewRootElement(getXMLElementTagName());
        result.setAttribute("newRegionName", newRegionName);
        result.setAttribute("unit", unitId);
        return result;
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "newRegionName".
     */
    public static String getXMLElementTagName() {
        return "newRegionName";
    }
}
