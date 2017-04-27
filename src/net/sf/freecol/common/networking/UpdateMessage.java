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

    /** The objects to update. */
    private final List<FreeColGameObject> fcgos = new ArrayList<>();

    /** Limited fields for partial objects. */
    private final List<List<String>> fields = new ArrayList<>();


    /**
     * Create a new {@code UpdateMessage}.
     *
     * @param destination The destination {@code ServerPlayer}.
     */
    private UpdateMessage(ServerPlayer destination) {
        super(TAG);

        this.destination = destination;
        this.fcgos.clear();
        this.fields.clear();
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

        append(fco, null);
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

        if (fcos != null) {
            for (FreeColObject fco : fcos) append(fco, null);
        }
    }

    /**
     * Create a new {@code UpdateMessage}.
     *
     * @param destination The destination {@code ServerPlayer}.
     * @param fco A {@code FreeColObject}s to add (FIXME: currently
     *     only {@code FreeColGameObject}s are actually allowed).
     * @param flds A list of fields to update.
     */
    public UpdateMessage(ServerPlayer destination,
                         FreeColObject fco, List<String> flds) {
        this(destination);

        append(fco, flds);
    }

    /**
     * Create a new {@code UpdateMessage} from a supplied
     * element.
     *
     * @param game The {@code Game} this message belongs to.
     * @param element The {@code Element} to use to create the message.
     */
    public UpdateMessage(Game game, Element element) {
        this((ServerPlayer)null, DOMUtils.getChildren(game, element));
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
            this.fcgos.addAll(xr.readFreeColObject(game));
            xr.expectTag(tag);
        }
        xr.expectTag(TAG);
    }

    /**
     * Append another object and optional partial fields to update.
     *
     * @param fco The {@code FreeColObject} to update.
     * @param flds An optional list of fields to update.
     */
    private void append(FreeColObject fco, List<String> flds) {
        if (fco != null
            && FreeColGameObject.class.isAssignableFrom(fco.getClass())) {
            this.fcgos.add((FreeColGameObject)fco);
            this.fields.add(flds);
        }
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
            this.fcgos.addAll(other.getObjects());
            this.fields.addAll(other.getFields());
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
            igc(freeColClient).updateHandler(getObjects());
        } else {
            pgc(freeColClient).updateHandler(getObjects());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeChildren(FreeColXMLWriter xw) throws XMLStreamException {
        WriteScope ws = null;
        if (this.destination != null) {
            ws = xw.getWriteScope();
            xw.setWriteScope(WriteScope.toClient(this.destination));
        }
        final int n = this.fcgos.size();
        for (int i = 0; i < n; i++) {
            FreeColGameObject fcgo = this.fcgos.get(i);
            List<String> parts = this.fields.get(i);
            if (parts == null) {
                fcgo.toXML(xw);
            } else {
                fcgo.toXMLPartial(xw, parts);
            }
        }
        if (ws != null) xw.setWriteScope(ws);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Element toXMLElement() {
        DOMMessage message = new DOMMessage(TAG);
        final int n = this.fcgos.size();
        for (int i = 0; i < n; i++) {
            List<String> part = this.fields.get(i);
            if (part == null) {
                message.add(this.fcgos.get(i), this.destination);
            } else {
                message.add(this.fcgos.get(i), part);
            }
        }
        return message.toXMLElement();
    }


    // Public interface

    /**
     * Get the objects attached to this message.
     *
     * @return The list of {@code FreeColGameObject}s.
     */
    public List<FreeColGameObject> getObjects() {
        return this.fcgos;
    }

    /**
     * Get the attribute fields attached to this message.
     *
     * @return The list of fields lists.
     */
    public List<List<String>> getFields() {
        return this.fields;
    }
}
