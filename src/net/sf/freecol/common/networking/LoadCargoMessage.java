/**
 *  Copyright (C) 2002-2013   The FreeCol Team
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
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 * The message sent when loading cargo onto a carrier.
 */
public class LoadCargoMessage extends DOMMessage {

    /** The goods to be loaded. */
    private Goods goods;

    /** The identifier of the carrier. */
    private String carrierId;


    /**
     * Create a new <code>LoadCargoMessage</code> with the
     * supplied goods and carrier.
     *
     * @param goods The <code>Goods</code> to load.
     * @param carrier The <code>Unit</code> to load onto.
     */
    public LoadCargoMessage(Goods goods, Unit carrier) {
        super(getXMLElementTagName());

        this.goods = goods;
        this.carrierId = carrier.getId();
    }

    /**
     * Create a new <code>LoadCargoMessage</code> from a
     * supplied element.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     */
    public LoadCargoMessage(Game game, Element element) {
        super(getXMLElementTagName());

        this.carrierId = element.getAttribute("carrier");
        this.goods = new Goods(game, (Element)element.getChildNodes().item(0));
    }


    /**
     * Handle a "loadCargo"-message.
     *
     * @param server The <code>FreeColServer</code> handling the message.
     * @param player The <code>Player</code> the message applies to.
     * @param connection The <code>Connection</code> message received on.
     * @return An update containing the carrier, or an error
     *     <code>Element</code> on failure.
     */
    public Element handle(FreeColServer server, Player player,
                          Connection connection) {
        final ServerPlayer serverPlayer = server.getPlayer(connection);

        Unit unit;
        try {
            unit = player.getOurFreeColGameObject(carrierId, Unit.class);
        } catch (Exception e) {
            return DOMMessage.clientError(e.getMessage());
        }

        if (goods.getLocation() == null) {
            return DOMMessage.clientError("Goods with no location: " + goods);
        }

        // Perform the load.
        return server.getInGameController()
            .loadCargo(serverPlayer, unit, goods);
    }

    /**
     * Convert this LoadCargoMessage to XML.
     *
     * @return The XML representation of this message.
     */
    public Element toXMLElement() {
        Element result = createMessage(getXMLElementTagName(),
            "carrier", carrierId);
        result.appendChild(goods.toXMLElement(result.getOwnerDocument()));
        return result;
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "loadCargo".
     */
    public static String getXMLElementTagName() {
        return "loadCargo";
    }
}
