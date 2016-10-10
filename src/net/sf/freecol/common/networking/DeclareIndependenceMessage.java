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
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 * The message sent when a player declares independence.
 */
public class DeclareIndependenceMessage extends AttributeMessage {

    public static final String TAG = "declareIndependence";
    private static final String COUNTRY_NAME_TAG = "countryName";
    private static final String NATION_NAME_TAG = "nationName";


    /**
     * Create a new {@code DeclareIndependenceMessage} with the
     * supplied name.
     *
     * @param nationName The new name for the rebelling nation.
     * @param countryName The new name for the rebelling country.
     */
    public DeclareIndependenceMessage(String nationName, String countryName) {
        super(TAG, NATION_NAME_TAG, nationName, COUNTRY_NAME_TAG, countryName);
    }

    /**
     * Create a new {@code DeclareIndependenceMessage} from a
     * supplied element.
     *
     * @param game The {@code Game} this message belongs to.
     * @param element The {@code Element} to use to create the message.
     */
    public DeclareIndependenceMessage(Game game, Element element) {
        this(getStringAttribute(element, NATION_NAME_TAG),
             getStringAttribute(element, COUNTRY_NAME_TAG));
    }


    /**
     * Handle a "declareIndependence"-message.
     *
     * @param server The {@code FreeColServer} handling the message.
     * @param player The {@code Player} the message applies to.
     * @param connection The {@code Connection} the message is from.
     *
     * @return An update {@code Element} describing the REF and the
     *         rebel player, or an error {@code Element} on failure.
     */
    public Element handle(FreeColServer server, Player player,
                          Connection connection) {
        final ServerPlayer serverPlayer = server.getPlayer(connection);
        final String nationName = getAttribute(NATION_NAME_TAG);
        final String countryName = getAttribute(COUNTRY_NAME_TAG);
        
        if (nationName == null || nationName.isEmpty()) {
            return serverPlayer.clientError("Empty nation name.")
                .build(serverPlayer);
        }

        if (countryName == null || countryName.isEmpty()) {
            return serverPlayer.clientError("Empty country name.")
                .build(serverPlayer);
        }

        StringTemplate problem = player.checkDeclareIndependence();
        if (problem != null) {
            return serverPlayer.clientError("Declaration blocked")
                .build(serverPlayer);
        }

        // Declare.
        return server.getInGameController()
            .declareIndependence(serverPlayer, nationName, countryName)
            .build(serverPlayer);
    }
}
