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

import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Player.PlayerType;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 * The message sent when a player declares independence.
 */
public class DeclareIndependenceMessage extends Message {

    /**
     * The new name for the rebelling nation
     */
    private String nationName;

    /**
     * The new name for the rebelling country
     */
    private String countryName;

    /**
     * Create a new <code>DeclareIndependenceMessage</code> with the
     * supplied name.
     *
     * @param nationName The new name for the rebelling nation.
     * @param countryName The new name for the rebelling country.
     */
    public DeclareIndependenceMessage(String nationName, String countryName) {
        this.nationName = nationName;
        this.countryName = countryName;
    }

    /**
     * Create a new <code>DeclareIndependenceMessage</code> from a
     * supplied element.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     */
    public DeclareIndependenceMessage(Game game, Element element) {
        this.nationName = element.getAttribute("nationName");
        this.countryName = element.getAttribute("countryName");
    }

    /**
     * Handle a "declareIndependence"-message.
     *
     * @param server The <code>FreeColServer</code> handling the message.
     * @param player The <code>Player</code> the message applies to.
     * @param connection The <code>Connection</code> the message is from.
     *
     * @return An update <code>Element</code> describing the REF and the
     *         rebel player, or an error <code>Element</code> on failure.
     */
    public Element handle(FreeColServer server, Player player, Connection connection) {
        ServerPlayer serverPlayer = server.getPlayer(connection);

        if (nationName == null || nationName.length() == 0
            || countryName == null || countryName.length() == 0) {
            return Message.clientError("Empty nation or country name.");
        }
        if (player.getSoL() < 50) {
            return Message.clientError("Cannot declare independence with SoL < 50: " + player.getSoL());
        }
        if (player.getPlayerType() != PlayerType.COLONIAL) {
            return Message.clientError("Only colonial players can declare independence.");
        }

        // Declare.
        return server.getInGameController().
            declareIndependence(serverPlayer, nationName, countryName);
    }

    /**
     * Convert this DeclareIndependenceMessage to XML.
     *
     * @return The XML representation of this message.
     */
    public Element toXMLElement() {
        Element result = createNewRootElement(getXMLElementTagName());
        result.setAttribute("nationName", nationName);
        result.setAttribute("countryName", countryName);
        return result;
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "declareIndependence".
     */
    public static String getXMLElementTagName() {
        return "declareIndependence";
    }
}
