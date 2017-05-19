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
import net.sf.freecol.server.model.ServerPlayer;

import net.sf.freecol.common.util.DOMUtils;
import org.w3c.dom.Element;


/**
 * The message sent to update game objects.
 */
public class UpdateMessage extends ObjectMessage {

    public static final String TAG = "update";

    /** The player to specialize the objects for. */
    private final ServerPlayer destination;


    /**
     * Create a new {@code UpdateMessage}.
     *
     * @param destination The destination {@code ServerPlayer}.
     */
    private UpdateMessage(ServerPlayer destination) {
        super(TAG);

        this.destination = destination;
    }
    
    /**
     * Create a new {@code UpdateMessage}.
     *
     * @param destination The destination {@code ServerPlayer}.
     * @param fco A {@code FreeColObject}s to add (FIXME: currently
     *     only {@code FreeColGameObject}s are actually allowed).
     */
    public UpdateMessage(ServerPlayer destination,
                         FreeColObject fco) {
        this(destination);

        add1(fco);
    }

    /**
     * Create a new {@code UpdateMessage}.
     *
     * @param destination The destination {@code ServerPlayer}.
     * @param fcos A list of {@code FreeColObject}s to add.
     */
    public UpdateMessage(ServerPlayer destination,
                         List<FreeColObject> fcos) {
        this(destination);

        if (fcos != null) addAll(fcos);
    }

    /**
     * Create a new {@code UpdateMessage} from a supplied
     * element.
     *
     * @param game The {@code Game} this message belongs to.
     * @param element The {@code Element} to use to create the message.
     */
    public UpdateMessage(Game game, Element element) {
        this((ServerPlayer)null, DOMUtils.getChildren(game, element, true));
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
        this((ServerPlayer)null);

        while (xr.moreTags()) {
            String tag = xr.getLocalName();
            add1(xr.readFreeColObject(game));
            xr.expectTag(tag);
        }
        xr.expectTag(TAG);
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
            addAll(other.getChildren());
            return true;
        }
        return false;
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
        } else {
            pgc(freeColClient).updateHandler(getChildren());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeChildren(FreeColXMLWriter xw) throws XMLStreamException {
        WriteScope ws = null;
        if (this.destination != null) {
            ws = xw.replaceScope(WriteScope.toClient(this.destination));
        }
        try {
            for (FreeColObject fco : getChildren()) {
                fco.toXML(xw);
            }
        } finally {
            if (ws != null) xw.replaceScope(ws);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Element toXMLElement() {
        DOMMessage message = new DOMMessage(TAG);
        for (FreeColObject fco : getChildren()) {
            message.add(fco, this.destination);
        }
        return message.toXMLElement();
    }
}
