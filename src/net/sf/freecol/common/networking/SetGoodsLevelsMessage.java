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

import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.ExportData;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 * The message sent when setting goods levels.
 */
public class SetGoodsLevelsMessage extends DOMMessage {

    /** The identifier of the colony where the goods levels are set. */
    private final String colonyId;

    /** The new ExportData. */
    private final ExportData data;


    /**
     * Create a new <code>SetGoodsLevelsMessage</code> with the
     * supplied colony and data.
     *
     * @param colony The <code>Colony</code> where the goods leves are set.
     * @param data The new <code>ExportData</code>.
     */
    public SetGoodsLevelsMessage(Colony colony, ExportData data) {
        super(getXMLElementTagName());

        this.colonyId = colony.getId();
        this.data = data;
    }

    /**
     * Create a new <code>SetGoodsLevelsMessage</code> from a
     * supplied element.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     */
    public SetGoodsLevelsMessage(Game game, Element element) {
        super(getXMLElementTagName());

        colonyId = element.getAttribute("colony");
        data = new ExportData((Element)element.getChildNodes().item(0));
    }


    /**
     * Handle a "setGoodsLevels"-message.
     *
     * @param server The <code>FreeColServer</code> handling the message.
     * @param player The <code>Player</code> the message applies to.
     * @param connection The <code>Connection</code> message was received on.
     *
     * @return An update <code>Element</code> updating the colony.
     */
    public Element handle(FreeColServer server, Player player,
                          Connection connection) {
        final ServerPlayer serverPlayer = server.getPlayer(connection);

        Colony colony;
        try {
            colony = player.getOurFreeColGameObject(colonyId, Colony.class);
        } catch (Exception e) {
            return DOMMessage.clientError(e.getMessage());
        }

        // Proceed to set.
        return server.getInGameController()
            .setGoodsLevels(serverPlayer, colony, data);
    }

    /**
     * Convert this SetGoodsLevelsMessage to XML.
     *
     * @return The XML representation of this message.
     */
    @Override
    public Element toXMLElement() {
        Element result = createMessage(getXMLElementTagName(),
            "colony", colonyId);
        result.appendChild(data.toXMLElement(result.getOwnerDocument()));
        return result;
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "setGoodsLevels".
     */
    public static String getXMLElementTagName() {
        return "setGoodsLevels";
    }
}
