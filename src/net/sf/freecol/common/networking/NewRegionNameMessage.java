/**
 *  Copyright (C) 2002-2019   The FreeCol Team
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

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Region;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.ai.AIPlayer;
import net.sf.freecol.server.model.ServerPlayer;


/**
 * The message sent when naming a new region.
 */
public class NewRegionNameMessage extends AttributeMessage {

    public static final String TAG = "newRegionName";
    private static final String NEW_REGION_NAME_TAG = "newRegionName";
    private static final String REGION_TAG = "region";
    private static final String TILE_TAG = "tile";
    private static final String UNIT_TAG = "unit";


    /**
     * Create a new {@code NewRegionNameMessage} with the
     * supplied name.
     *
     * @param region The {@code Region} being discovered.
     * @param tile The {@code Tile} where the region is discovered.
     * @param unit The {@code Unit} that discovers the region.
     * @param newRegionName The default new region name.
     */
    public NewRegionNameMessage(Region region, Tile tile, Unit unit,
                                String newRegionName) {
        super(TAG, REGION_TAG, region.getId(), TILE_TAG, tile.getId(),
              UNIT_TAG, unit.getId(), NEW_REGION_NAME_TAG, newRegionName);
    }

    /**
     * Create a new {@code NewRegionNameMessage} from a stream.
     *
     * @param game The {@code Game} to read within.
     * @param xr The {@code FreeColXMLReader} to read from.
     * @exception XMLStreamException if the stream is corrupt.
     */
    public NewRegionNameMessage(Game game, FreeColXMLReader xr)
        throws XMLStreamException {
        super(TAG, xr, REGION_TAG, TILE_TAG, UNIT_TAG, NEW_REGION_NAME_TAG);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public MessagePriority getPriority() {
        return Message.MessagePriority.LATE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void aiHandler(FreeColServer freeColServer, AIPlayer aiPlayer) {
        final Game game = freeColServer.getGame();
        final Region region = getRegion(game);
        final Tile tile = getTile(game);
        final Unit unit = getUnit(aiPlayer.getPlayer());
        final String name = getNewRegionName();

        aiPlayer.newRegionNameHandler(region, tile, unit, name);
    }
        
    /**
     * {@inheritDoc}
     */
    @Override
    public void clientHandler(FreeColClient freeColClient) {
        final Game game = freeColClient.getGame();
        final Tile tile = getTile(game);
        final Unit unit = getUnit(freeColClient.getMyPlayer());
        final Region region = getRegion(game);
        final String name = getNewRegionName();

        if (name == null || region == null) return;

        igc(freeColClient).newRegionNameHandler(region, tile, unit, name);
        clientGeneric(freeColClient);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ChangeSet serverHandler(FreeColServer freeColServer,
                                   ServerPlayer serverPlayer) {
        final Game game = freeColServer.getGame();

        Tile tile = getTile(game);
        if (!serverPlayer.hasExplored(tile)) {
            return serverPlayer.clientError("Can not claim discovery in unexplored tile: "
                + getStringAttribute(TILE_TAG));
        }

        Unit unit;
        try {
            unit = getUnit(serverPlayer);
        } catch (Exception e) {
            return serverPlayer.clientError(e.getMessage());
        }

        Region region = tile.getDiscoverableRegion();
        if (region == null) {
            return serverPlayer.clientError("No discoverable region in: "
                + tile.getId());
        }
        String regionId = getStringAttribute(REGION_TAG);
        if (!region.getId().equals(regionId)) {
            return serverPlayer.clientError("Region mismatch, "
                + region.getId() + " != " + regionId);
        }

        // Do the discovery
        return igc(freeColServer)
            .setNewRegionName(serverPlayer, unit, region, getNewRegionName());
    }


    // Public interface

    /**
     * Public accessor for the region.
     *
     * @param game The {@code Game} to look for a region in.
     * @return The region of this message.
     */
    public Region getRegion(Game game) {
        return game.getFreeColGameObject(getStringAttribute(REGION_TAG), Region.class);
    }

    /**
     * Public accessor for the tile.
     *
     * @param game The {@code Game} to look for a tile in.
     * @return The tile of this message.
     */
    public Tile getTile(Game game) {
        return game.getFreeColGameObject(getStringAttribute(TILE_TAG), Tile.class);
    }

    /**
     * Public accessor for the unit.
     *
     * @param player The {@code Player} who owns the unit.
     * @return The {@code Unit} of this message.
     */
    public Unit getUnit(Player player) {
        return player.getOurFreeColGameObject(getStringAttribute(UNIT_TAG), Unit.class);
    }

    /**
     * Public accessor for the new region name.
     *
     * @return The new region name of this message.
     */
    public String getNewRegionName() {
        return getStringAttribute(NEW_REGION_NAME_TAG);
    }
}
