/**
 *  Copyright (C) 2002-2019   The FreeCol Team
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.Game;
import static net.sf.freecol.common.util.CollectionUtils.*;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.ai.AIPlayer;
import net.sf.freecol.server.model.ServerPlayer;


/**
 * The message sent when removing a FreeColGameObject.
 */
public class RemoveMessage extends AttributeMessage {

    public static final String TAG = "remove";
    private static final String DIVERT_TAG = "divert";


    /**
     * Create a new {@code RemoveMessage}.
     *
     * @param divertId The identifier of an object to divert message
     *     attribution to.
     * @param objects A list of {@code FreeColGameObject}s to remove.
     */
    public RemoveMessage(String divertId,
                         List<? extends FreeColGameObject> objects) {
        super(TAG, DIVERT_TAG, divertId);
        
        setArrayAttributes(transform(objects, alwaysTrue(),
                           FreeColObject::getId));
    }

    /**
     * Create a new {@code RemoveMessage} from a stream.
     *
     * @param game The {@code Game} this message belongs to.
     * @param xr A {@code FreeColXMLReader} to read from.
     * @exception XMLStreamException if the stream is corrupt.
     */
    public RemoveMessage(Game game, FreeColXMLReader xr)
        throws XMLStreamException {
        super(TAG, getAttributeMap(xr));

        xr.closeTag(TAG);
    }

    /**
     * Get the attributes as a map.
     *
     * @param xr A {@code FreeColXMLReader} to read from.
     * @return A map of the attributes.
     */
    private static Map<String, String> getAttributeMap(FreeColXMLReader xr) {
        Map<String, String> ret = xr.getArrayAttributeMap();
        ret.put(DIVERT_TAG, xr.getAttribute(DIVERT_TAG, (String)null));
        return ret;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public MessagePriority getPriority() {
        return Message.MessagePriority.REMOVE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void aiHandler(FreeColServer freeColServer, AIPlayer aiPlayer) {
        // Ignored
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clientHandler(FreeColClient freeColClient) {
        final Game game = freeColClient.getGame();
        final FreeColGameObject divert = getDivertObject(game);
        final List<FreeColGameObject> objects = getRemovals(game);

        if (objects.isEmpty()) return;

        igc(freeColClient).removeHandler(objects, divert);
        clientGeneric(freeColClient);
    }


    // Public interface

    /**
     * Get the divert object.
     *
     * @param game The {@code Game} to look in.
     * @return The divert object, or null if none found.
     */
    public FreeColGameObject getDivertObject(Game game) {
        return game.getFreeColGameObject(getStringAttribute(DIVERT_TAG));
    }

    /**
     * Get the objects to remove.
     *
     * @param game The {@code Game} to look in.
     * @return A list of {@code FreeColGameObject}s to remove.
     */
    public List<FreeColGameObject> getRemovals(Game game) {
        List<FreeColGameObject> ret = new ArrayList<>();
        for (String id : getArrayAttributes()) {
            FreeColGameObject fcgo = game.getFreeColGameObject(id);
            if (fcgo != null) ret.add(fcgo);
        }
        return ret;
    }
}
