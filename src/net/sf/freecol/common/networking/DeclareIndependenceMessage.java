/**
 *  Copyright (C) 2002-2017   The FreeCol Team
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

import net.sf.freecol.common.io.FreeColXMLReader;
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
     * Create a new {@code DeclareIndependenceMessage} from a stream.
     *
     * @param game The {@code Game} this message belongs to.
     * @param xr The {@code FreeColXMLReader} to read from.
     */
    public DeclareIndependenceMessage(Game game, FreeColXMLReader xr) {
        super(TAG, xr, NATION_NAME_TAG, COUNTRY_NAME_TAG);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean currentPlayerMessage() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MessagePriority getPriority() {
        return Message.MessagePriority.NORMAL;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ChangeSet serverHandler(FreeColServer freeColServer,
                                   ServerPlayer serverPlayer) {
        final String nationName = getStringAttribute(NATION_NAME_TAG);
        final String countryName = getStringAttribute(COUNTRY_NAME_TAG);
        
        if (nationName == null || nationName.isEmpty()) {
            return serverPlayer.clientError("Empty nation name.");
        }

        if (countryName == null || countryName.isEmpty()) {
            return serverPlayer.clientError("Empty country name.");
        }

        StringTemplate problem = serverPlayer.checkDeclareIndependence();
        if (problem != null) {
            return serverPlayer.clientError("Declaration blocked");
        }

        // Declare.
        return igc(freeColServer)
            .declareIndependence(serverPlayer, nationName, countryName);
    }
}
