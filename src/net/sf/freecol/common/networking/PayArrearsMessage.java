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
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 * The message sent when paying tax arrears.
 */
public class PayArrearsMessage extends TrivialMessage {

    public static final String TAG = "payArrears";
    private static final String GOODS_TYPE_TAG = "goodsType";


    /**
     * Create a new {@code PayArrearsMessage} with the
     * supplied goods type.
     *
     * @param type The {@code GoodsType} to pay arrears for.
     */
    public PayArrearsMessage(GoodsType type) {
        super(TAG, GOODS_TYPE_TAG, type.getId());
    }

    /**
     * Create a new {@code PayArrearsMessage} from a
     * supplied element.
     *
     * @param game The {@code Game} this message belongs to.
     * @param element The {@code Element} to use to create the message.
     */
    public PayArrearsMessage(Game game, Element element) {
        super(TAG, GOODS_TYPE_TAG, getStringAttribute(element, GOODS_TYPE_TAG));
    }


    /**
     * Handle a "payArrears"-message.
     *
     * @param server The {@code FreeColServer} handling the message.
     * @param player The {@code Player} the message applies to.
     * @param connection The {@code Connection} message was received on.
     *
     * @return An update containing the payArrearsd unit,
     *         or an error {@code Element} on failure.
     */
    public Element handle(FreeColServer server, Player player,
                          Connection connection) {
        final ServerPlayer serverPlayer = server.getPlayer(connection);
        GoodsType goodsType = server.getSpecification()
            .getGoodsType(getAttribute(GOODS_TYPE_TAG));

        // Proceed to pay.
        return server.getInGameController()
            .payArrears(serverPlayer, goodsType)
            .build(serverPlayer);
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "payArrears".
     */
    public static String getTagName() {
        return TAG;
    }
}
