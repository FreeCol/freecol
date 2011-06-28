/**
 *  Copyright (C) 2002-2011  The FreeCol Team
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
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 * The message sent when naming a new region.
 */
public class NewRegionNameMessage extends Message {

    /**
     * The ID of the region being discovered.
     */
    private String regionId;

    /**
     * The ID of the unit that discovered the region.
     */
    private String unitId;

    /**
     * The new name.
     */
    private String newRegionName;

    /**
     * Create a new <code>NewRegionNameMessage</code> with the
     * supplied name.
     *
     * @param region The <code>Region</code> being discovered.
     * @param unit The <code>Unit</code> that is discovering.
     * @param newRegionName The default new region name.
     */
    public NewRegionNameMessage(Region region, Unit unit,
                                String newRegionName) {
        this.regionId = region.getId();
        this.unitId = unit.getId();
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
        this.unitId = element.getAttribute("unit");
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
     * Public accessor for the unit.
     *
     * @param game The <code>Game</code> to look for a unit in.
     * @return The unit of this message.
     */
    public Unit getUnit(Game game) {
        Object o = game.getFreeColGameObjectSafely(unitId);
        return (o instanceof Unit) ? (Unit) o : null;
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
        ServerPlayer serverPlayer = server.getPlayer(connection);
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
        if (!region.getId().equals(regionId)) {
            return Message.clientError("Region mismatch, " + region.getId()
                + " != " + regionId);
        }

        // Do the discovery
        return server.getInGameController()
            .setNewRegionName(serverPlayer, unit, region, newRegionName);
    }

    /**
     * Convert this NewRegionNameMessage to XML.
     *
     * @return The XML representation of this message.
     */
    public Element toXMLElement() {
        Element result = createNewRootElement(getXMLElementTagName());
        result.setAttribute("region", regionId);
        result.setAttribute("unit", unitId);
        result.setAttribute("newRegionName", newRegionName);
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
