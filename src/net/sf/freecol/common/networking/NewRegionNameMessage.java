/**
 *  Copyright (C) 2002-2012   The FreeCol Team
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
import net.sf.freecol.common.model.Region;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 * The message sent when naming a new region.
 */
public class NewRegionNameMessage extends DOMMessage {

    /**
     * The ID of the region being discovered.
     */
    private String regionId;

    /**
     * The tile where the region is discovered.
     */
    private String tileId;

    /**
     * The new name.
     */
    private String newRegionName;

    /**
     * Create a new <code>NewRegionNameMessage</code> with the
     * supplied name.
     *
     * @param region The <code>Region</code> being discovered.
     * @param tile The <code>Tile</code> where the region is discovered.
     * @param newRegionName The default new region name.
     */
    public NewRegionNameMessage(Region region, Tile tile,
                                String newRegionName) {
        this.regionId = region.getId();
        this.tileId = tile.getId();
        this.newRegionName = newRegionName;
    }

    /**
     * Create a new <code>NewRegionNameMessage</code> from a
     * supplied element.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     */
    public NewRegionNameMessage(Game game, Element element) {
        this.regionId = element.getAttribute("region");
        this.tileId = element.getAttribute("tile");
        this.newRegionName = element.getAttribute("newRegionName");
    }

    /**
     * Public accessor for the region.
     *
     * @param game The <code>Game</code> to look for a region in.
     * @return The region of this message.
     */
    public Region getRegion(Game game) {
        Object o = game.getFreeColGameObjectSafely(regionId);
        return (o instanceof Region) ? (Region) o : null;
    }

    /**
     * Public accessor for the tile.
     *
     * @param game The <code>Game</code> to look for a tile in.
     * @return The tile of this message.
     */
    public Tile getTile(Game game) {
        Object o = game.getFreeColGameObjectSafely(tileId);
        return (o instanceof Tile) ? (Tile)o : null;
    }

    /**
     * Public accessor for the new region name.
     *
     * @return The new region name of this message.
     */
    public String getNewRegionName() {
        return newRegionName;
    }

    /**
     * Handle a "newRegionName"-message.
     *
     * @param server The <code>FreeColServer</code> handling the message.
     * @param player The <code>Player</code> the message applies to.
     * @param connection The <code>Connection</code> message was received on.
     *
     * @return Null.
     */
    public Element handle(FreeColServer server, Player player,
                          Connection connection) {
        Game game = server.getGame();
        ServerPlayer serverPlayer = server.getPlayer(connection);

        Tile tile = getTile(game);
        if (!tile.isExploredBy(player)) {
            return DOMMessage.clientError("Can not claim discovery in unexplored tile: " + tileId);
        }

        Region region = tile.getDiscoverableRegion();
        if (region == null) {
            return DOMMessage.clientError("No discoverable region in: "
                + tileId);
        }
        if (region.isPacific()) {
            return DOMMessage.clientError("Can not rename the Pacific!");
        }
        if (!region.getId().equals(regionId)) {
            return DOMMessage.clientError("Region mismatch, " + region.getId()
                + " != " + regionId);
        }

        // Do the discovery
        return server.getInGameController()
            .setNewRegionName(serverPlayer, region, newRegionName);
    }

    /**
     * Convert this NewRegionNameMessage to XML.
     *
     * @return The XML representation of this message.
     */
    public Element toXMLElement() {
        return createMessage(getXMLElementTagName(),
            "region", regionId,
            "tile", tileId,
            "newRegionName", newRegionName);
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
