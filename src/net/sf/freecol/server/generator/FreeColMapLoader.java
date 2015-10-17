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

package net.sf.freecol.server.generator;

import java.io.File;
import java.util.HashMap;

import net.sf.freecol.common.io.FreeColSavegameFile;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Map.Layer;
import net.sf.freecol.common.model.Nation;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Region;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileItemContainer;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerGame;
import net.sf.freecol.server.model.ServerIndianSettlement;
import net.sf.freecol.server.model.ServerPlayer;
import net.sf.freecol.server.model.ServerRegion;


/**
 * Load a map.
 */
public class FreeColMapLoader implements MapLoader {

    private final ServerGame importGame;

    public FreeColMapLoader(File file) throws Exception {
        importGame = FreeColServer.readGame(new FreeColSavegameFile(file), null, null);
    }


    @Override
    public Layer loadMap(Game game, Layer layer) {
        Map importMap = importGame.getMap();
        Layer highestLayer = layer.compareTo(importMap.getLayer()) < 0
            ? layer : importMap.getLayer();
        int width = importMap.getWidth();
        int height = importMap.getHeight();

        java.util.Map<String, ServerRegion> regions = new HashMap<>();

        Map map = new Map(game, width, height);
        if (highestLayer == Layer.LAND) {
            // import only the land / water distinction
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    Tile t = new Tile(game,
                        (importMap.getTile(x, y).getType().isWater())
                        ? TileType.WATER : TileType.LAND,
                        x, y);
                    map.setTile(t, x, y);
                }
            }
        } else {
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    Tile template = importMap.getTile(x, y);
                    Tile tile = new Tile(game, null, x, y);
                    map.setTile(tile, x, y);

                    // import tile types
                    tile.setType(game.getSpecification().getTileType(template.getType().getId()));
                    tile.setMoveToEurope(template.getMoveToEurope());
                    if (highestLayer.compareTo(Layer.REGIONS) >= 0) {
                        // import regions
                        Region region = template.getRegion();
                        if (region != null) {
                            ServerRegion ours = regions.get(region.getId());
                            if (ours == null) {
                                ours = new ServerRegion(game, region);
                                regions.put(region.getId(), ours);
                            }
                            tile.setRegion(ours);
                            ours.addTile(tile);
                        }
                        if (highestLayer.compareTo(Layer.RIVERS) >= 0) {
                            // import tile improvements
                            tile.setTileItemContainer(new TileItemContainer(game, tile, template
                                                                            .getTileItemContainer(), layer));
                            if (layer.compareTo(Layer.NATIVES) >= 0) {
                                // import native settlements
                                if (template.getOwner() != null) {
                                    String nationId = template.getOwner().getNationId();
                                    Player player = game.getPlayerByNationId(nationId);
                                    if (player == null) {
                                        Nation nation = game.getSpecification().getNation(nationId);
                                        player = new ServerPlayer(game, false, nation, null, null);
                                        game.addPlayer(player);
                                    }
                                    tile.setOwner(player);
                                    if (template.getOwningSettlement() != null) {
                                        IndianSettlement settlement = (IndianSettlement) template.getOwningSettlement();
                                        tile.setOwningSettlement(new ServerIndianSettlement(game,
                                            player, tile, settlement));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        for (Region region : regions.values()) {
            map.addRegion(region);
        }
        map.setLayer(highestLayer);
        game.setMap(map);
        return highestLayer;
    }


    @Override
    public Layer getHighestLayer() {
        return Layer.NATIVES;
    }
}
