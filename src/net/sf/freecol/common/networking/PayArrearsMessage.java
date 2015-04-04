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

package net.sf.freecol.common.networking;

import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 * The message sent when paying tax arrears.
 */
public class PayArrearsMessage extends DOMMessage {

    /** The identifier of the GoodsType to pay arrears for. */
    private final String goodsTypeId;


    /**
     * Create a new <code>PayArrearsMessage</code> with the
     * supplied goods type.
     *
     * @param type The <code>GoodsType</code> to pay arrears for.
     */
    public PayArrearsMessage(GoodsType type) {
        super(getXMLElementTagName());

        this.goodsTypeId = type.getId();
    }

    /**
     * Create a new <code>PayArrearsMessage</code> from a
     * supplied element.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     */
    public PayArrearsMessage(Game game, Element element) {
        super(getXMLElementTagName());

        this.goodsTypeId = element.getAttribute("goodsType");
    }


    /**
     * Handle a "payArrears"-message.
     *
     * @param server The <code>FreeColServer</code> handling the message.
     * @param player The <code>Player</code> the message applies to.
     * @param connection The <code>Connection</code> message was received on.
     *
     * @return An update containing the payArrearsd unit,
     *         or an error <code>Element</code> on failure.
     */
    public Element handle(FreeColServer server, Player player,
                          Connection connection) {
        final ServerPlayer serverPlayer = server.getPlayer(connection);
        GoodsType goodsType = server.getSpecification()
            .getGoodsType(goodsTypeId);

        // Proceed to pay.
        return server.getInGameController()
            .payArrears(serverPlayer, goodsType);
    }

    /**
     * Convert this PayArrearsMessage to XML.
     *
     * @return The XML representation of this message.
     */
    @Override
    public Element toXMLElement() {
        return createMessage(getXMLElementTagName(),
            "goodsType", goodsTypeId);
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "payArrears".
     */
    public static String getXMLElementTagName() {
        return "payArrears";
    }
}
