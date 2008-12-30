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

import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Tile;

import net.sf.freecol.server.FreeColServer;

public class BuyLandMessage extends Message {

    /**
     * Describe tile here.
     */
    private Tile tile;

    public BuyLandMessage(Tile tile) {
        this.tile = tile;
    }


    public BuyLandMessage(Game game, Element element) {
        tile = (Tile) game.getFreeColGameObject(element.getAttribute("tile"));
    }


    /**
     * Get the <code>Tile</code> value.
     *
     * @return a <code>Tile</code> value
     */
    public final Tile getTile() {
        return tile;
    }

    /**
     * Set the <code>Tile</code> value.
     *
     * @param newTile The new Tile value.
     */
    public final void setTile(final Tile newTile) {
        this.tile = newTile;
    }

    public Element handle(FreeColServer server, Player player, Connection connection) {

        if (tile == null) {
            throw new IllegalStateException("Tile must not be 'null'.");
        } else if (tile.getOwner() == null) {
            tile.setOwner(player);
        } else if (tile.getOwner().isEuropean()) {
            throw new IllegalStateException("Can not buy land from European players!");
        } else {
            player.buyLand(tile);
        }
        return null;
    }

    public Element toXMLElement() {
        Element result = createNewRootElement(getXMLElementTagName());
        result.setAttribute("tile", tile.getId());
        return result;
    }

    public static String getXMLElementTagName() {
        return "buyLand";
    }

}
