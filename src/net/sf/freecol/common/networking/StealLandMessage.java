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
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Tile;

import net.sf.freecol.server.FreeColServer;

public class StealLandMessage extends Message {

    /**
     * Describe tile here.
     */
    private Tile tile;

    /**
     * Describe colony here.
     */
    private Colony colony;


    public StealLandMessage(Tile tile, Colony colony) {
        this.tile = tile;
        this.colony = colony;
    }


    public StealLandMessage(Game game, Element element) {
        tile = (Tile) game.getFreeColGameObject(element.getAttribute("tile"));
        String colonyID = element.getAttribute("colony");
        if (colonyID != null) {
            colony = (Colony) game.getFreeColGameObject(element.getAttribute("colony"));
        }
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

    /**
     * Get the <code>Colony</code> value.
     *
     * @return a <code>Colony</code> value
     */
    public final Colony getColony() {
        return colony;
    }

    /**
     * Set the <code>Colony</code> value.
     *
     * @param newColony The new Colony value.
     */
    public final void setColony(final Colony newColony) {
        this.colony = newColony;
    }


    public Element handle(FreeColServer server, Player player, Connection connection) {

        if (tile.getOwner() != null && tile.getOwner().isEuropean()) {
            throw new IllegalStateException("Can not steal land from European players!");
        }
        tile.takeOwnership(player, colony);
        return null;
    }

    public Element toXMLElement() {
        Element result = createNewRootElement(getXMLElementTagName());
        result.setAttribute("tile", tile.getId());
        if (colony != null) {
            result.setAttribute("colony", colony.getId());
        }
        return result;
    }

    public static String getXMLElementTagName() {
        return "stealLand";
    }

}
