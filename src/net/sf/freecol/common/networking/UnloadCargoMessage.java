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
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 * The message sent when unloading cargo onto a carrier.
 */
public class UnloadCargoMessage extends DOMMessage {

    /**
     * The goods to be unloaded.
     */
    private Goods goods;

    /**
     * Create a new <code>UnloadCargoMessage</code> with the
     * supplied goods and carrier.
     *
     * @param goods The <code>Goods</code> to unload.
     */
    public UnloadCargoMessage(Goods goods) {
        this.goods = goods;
    }

    /**
     * Create a new <code>UnloadCargoMessage</code> from a
     * supplied element.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     */
    public UnloadCargoMessage(Game game, Element element) {
        this.goods = new Goods(game, (Element) element.getChildNodes().item(0));
    }

    /**
     * Handle a "unloadCargo"-message.
     *
     * @param server The <code>FreeColServer</code> handling the message.
     * @param player The <code>Player</code> the message applies to.
     * @param connection The <code>Connection</code> message received on.
     *
     * @return An update containing the containing location
     *         or an error <code>Element</code> on failure.
     */
    public Element handle(FreeColServer server, Player player,
                          Connection connection) {
        ServerPlayer serverPlayer = server.getPlayer(connection);

        Location loc = goods.getLocation();
        if (loc == null) {
            return DOMMessage.clientError("Goods in a null location.");
        } else if (!(loc instanceof Unit)) {
            return DOMMessage.clientError("Unload from non-unit.");
        }
        Unit unit = (Unit) loc;
        if (!player.owns(unit)) {
            return DOMMessage.clientError("Unload from non-owned unit.");
        }

        // Perform the unload.
        return server.getInGameController()
            .unloadCargo(serverPlayer, unit, goods);
    }

    /**
     * Convert this UnloadCargoMessage to XML.
     *
     * @return The XML representation of this message.
     */
    public Element toXMLElement() {
        Element result = createMessage(getXMLElementTagName());
        result.appendChild(goods.toXMLElement(null,
                                              result.getOwnerDocument()));
        return result;
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "unloadCargo".
     */
    public static String getXMLElementTagName() {
        return "unloadCargo";
    }
}
