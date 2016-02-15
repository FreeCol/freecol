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
public class NewRegionNameMessage extends DOMMessage {

    public static final String TAG = "newRegionName";
    private static final String NEW_REGION_NAME_TAG = "newRegionName";
    private static final String REGION_TAG = "region";
    private static final String TILE_TAG = "tile";
    private static final String UNIT_TAG = "unit";

    /** The object identifier of the region being discovered. */
    private final String regionId;

    /** The tile where the region is discovered. */
    private final String tileId;

    /** The unit making the discovery. */
    private final String unitId;

    /** The new name. */
    private final String newRegionName;


    /**
     * Create a new <code>NewRegionNameMessage</code> with the
     * supplied name.
     *
     * @param region The <code>Region</code> being discovered.
     * @param tile The <code>Tile</code> where the region is discovered.
     * @param unit The <code>Unit</code> that discovers the region.
     * @param newRegionName The default new region name.
     */
    public NewRegionNameMessage(Region region, Tile tile, Unit unit,
                                String newRegionName) {
        super(getTagName());

        this.regionId = region.getId();
        this.tileId = tile.getId();
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
        super(getTagName());

        this.regionId = getStringAttribute(element, REGION_TAG);
        this.tileId = getStringAttribute(element, TILE_TAG);
        this.unitId = getStringAttribute(element, UNIT_TAG);
        this.newRegionName = getStringAttribute(element, NEW_REGION_NAME_TAG);
    }


    // Public interface

    /**
     * Public accessor for the region.
     *
     * @param game The <code>Game</code> to look for a region in.
     * @return The region of this message.
     */
    public Region getRegion(Game game) {
        return game.getFreeColGameObject(regionId, Region.class);
    }

    /**
     * Public accessor for the tile.
     *
     * @param game The <code>Game</code> to look for a tile in.
     * @return The tile of this message.
     */
    public Tile getTile(Game game) {
        return game.getFreeColGameObject(tileId, Tile.class);
    }

    /**
     * Public accessor for the unit.
     *
     * @param player The <code>Player</code> who owns the unit.
     * @return The <code>Unit</code> of this message.
     */
    public Unit getUnit(Player player) {
        return player.getOurFreeColGameObject(unitId, Unit.class);
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
     * @return An update setting the new region name, or an error
     *     <code>Element</code> on failure.
     */
    public Element handle(FreeColServer server, Player player,
                          Connection connection) {
        final ServerPlayer serverPlayer = server.getPlayer(connection);
        final Game game = server.getGame();

        Tile tile = getTile(game);
        if (!serverPlayer.hasExplored(tile)) {
            return serverPlayer.clientError("Can not claim discovery in unexplored tile: " + tileId)
                .build(serverPlayer);
        }

        Unit unit;
        try {
            unit = getUnit(player);
        } catch (Exception e) {
            return serverPlayer.clientError(e.getMessage())
                .build(serverPlayer);
        }

        Region region = tile.getDiscoverableRegion();
        if (region == null) {
            return serverPlayer.clientError("No discoverable region in: "
                + tileId)
                .build(serverPlayer);
        }
        if (!region.getId().equals(this.regionId)) {
            return serverPlayer.clientError("Region mismatch, "
                + region.getId() + " != " + this.regionId)
                .build(serverPlayer);
        }
        
        // Do the discovery
        return server.getInGameController()
            .setNewRegionName(serverPlayer, unit, region, this.newRegionName)
            .build(serverPlayer);
    }

    /**
     * Convert this NewRegionNameMessage to XML.
     *
     * @return The XML representation of this message.
     */
    @Override
    public Element toXMLElement() {
        return new DOMMessage(getTagName(),
            REGION_TAG, this.regionId,
            TILE_TAG, this.tileId,
            UNIT_TAG, this.unitId,
            NEW_REGION_NAME_TAG, this.newRegionName).toXMLElement();
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "newRegionName".
     */
    public static String getTagName() {
        return TAG;
    }
}
