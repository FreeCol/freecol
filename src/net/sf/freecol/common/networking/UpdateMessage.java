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
import java.util.List;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.io.FreeColXMLWriter.WriteScope;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.ai.AIPlayer;


/**
 * The message sent to update game objects.
 */
public class UpdateMessage extends ObjectMessage {

    public static final String TAG = "update";

    /** The player to specialize the objects for. */
    private final Player destination;


    /**
     * Create a new {@code UpdateMessage}.
     *
     * @param destination The destination {@code Player}.
     */
    private UpdateMessage(Player destination) {
        super(TAG);

        this.destination = destination;
    }
    
    /**
     * Create a new {@code UpdateMessage}.
     *
     * @param destination The destination {@code Player}.
     * @param fcgo A {@code FreeColGameObject}s to add.
     */
    public UpdateMessage(Player destination,
                         FreeColGameObject fcgo) {
        this(destination);

        appendChild(fcgo);
    }

    /**
     * Create a new {@code UpdateMessage}.
     *
     * @param destination The destination {@code Player}.
     * @param fcgos A list of {@code FreeColObject}s to add.
     */
    public UpdateMessage(Player destination,
                         List<FreeColGameObject> fcgos) {
        this(destination);

        appendChildren(fcgos);
    }

    /**
     * Create a new {@code UpdateMessage} from a stream.
     *
     * @param game The {@code Game} this message belongs to.
     * @param xr The {@code FreeColXMLReader} to read from.
     * @exception XMLStreamException if there is a problem reading the stream.
     */
    public UpdateMessage(Game game, FreeColXMLReader xr)
        throws XMLStreamException {
        this((Player)null);

        FreeColXMLReader.ReadScope rs
            = xr.replaceScope(FreeColXMLReader.ReadScope.NOINTERN);
        List<FreeColObject> fcos = new ArrayList<>();
        try {
            while (xr.moreTags()) {
                String tag = xr.getLocalName();
                fcos.add(xr.readFreeColObject(game));
                xr.expectTag(tag);
            }
            xr.expectTag(TAG);
        } finally {
            xr.replaceScope(rs);
        }
        appendChildren(fcos);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public MessagePriority getPriority() {
        return Message.MessagePriority.UPDATE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean merge(Message message) {
        if (message instanceof UpdateMessage) {
            UpdateMessage other = (UpdateMessage)message;
            appendChildren(other.getChildren());
            return true;
        }
        return false;
    }
              
    /**
     * {@inheritDoc}
     */
    @Override
    public void toXML(FreeColXMLWriter xw) throws XMLStreamException {
        FreeColXMLWriter.WriteScope ws = null;
        if (this.destination != null) {
            ws = xw.replaceScope(FreeColXMLWriter.WriteScope
                .toClient(this.destination));
        }
        super.toXML(xw);
        if (this.destination != null) xw.replaceScope(ws);
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
        if (freeColClient.isInGame()) {
            igc(freeColClient).updateHandler(getChildren());
            clientGeneric(freeColClient);
        } else {
            pgc(freeColClient).updateHandler(getChildren());
        }
    }
}
