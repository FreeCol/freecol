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
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.ai.AIPlayer;
import net.sf.freecol.server.model.ServerPlayer;


/**
 * The message that contains other messages.
 */
public class MultipleMessage extends AttributeMessage {

    public static final String TAG = "multiple";

    /** The list of messages. */
    private final List<Message> messages = new ArrayList<>();


    /**
     * Create a new {@code MultipleMessage}.
     */
    public MultipleMessage() {
        super(TAG);

        this.messages.clear();
    }

    /**
     * Create a new {@code MultipleMessage} with the given messages.
     *
     * @param messages The {@code Message}s to add.
     */
    public MultipleMessage(List<Message> messages) {
        this();
        
        if (messages != null) this.messages.addAll(messages);
    }

    /**
     * Create a new {@code MultipleMessage} from a stream.
     *
     * @param game The {@code Game} to read within.
     * @param xr The {@code FreeColXMLReader} to read from.
     * @exception XMLStreamException if the stream is corrupt.
     * @exception FreeColException if the internal message can not be read.
     */
    public MultipleMessage(Game game, FreeColXMLReader xr)
        throws XMLStreamException, FreeColException {
        this();
        
        this.messages.clear();
        while (xr.moreTags()) {
            final String mt = xr.getLocalName();
            Message m = Message.read(game, xr);
            if (m != null) this.messages.add(m);
            xr.expectTag(mt);
        }
        xr.expectTag(TAG);
    }


    /**
     * {@inheritDoc}
     */
    public boolean isEmpty() {
        return super.isEmpty() && this.messages.isEmpty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void aiHandler(FreeColServer freeColServer, AIPlayer aiPlayer)
        throws FreeColException {
        final Connection conn = aiPlayer.getConnection();
        if (conn == null) return;

        if (!this.messages.isEmpty()) {
            for (Message m : this.messages) {
                Message ret = conn.handle(m);
                assert ret == null;
            }
        }
    }
 
    /**
     * {@inheritDoc}
     */
    @Override
    public void clientHandler(FreeColClient freeColClient)
        throws FreeColException {
        final Connection conn = freeColClient.askServer().getConnection();
        if (conn == null) return;

        if (!this.messages.isEmpty()) {
            for (Message m : this.messages) {
                Message ret = conn.handle(m);
                assert ret == null;
            }
        }                 
        clientGeneric(freeColClient);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ChangeSet serverHandler(FreeColServer freeColServer,
                                   ServerPlayer serverPlayer) {
        final Connection conn = serverPlayer.getConnection();
        if (conn == null) return null;

        if (!this.messages.isEmpty()) {
            ChangeSet cs = new ChangeSet();
            for (Message m : this.messages) {
                try {
                    Message r = conn.handle(m);
                    if (r != null) cs.add(ChangeSet.See.only(serverPlayer), r);
                } catch (FreeColException fce) {
                    logger.log(Level.WARNING, "MultipleMessage server fail", fce);
                }
            }
            return (cs.isEmpty()) ? null : cs;
        }

        return null;
    }
            
    /**
     * {@inheritDoc}
     */
    @Override
    public void writeChildren(FreeColXMLWriter xw) throws XMLStreamException {
        for (Message m : this.messages) m.toXML(xw);
    }

    // Public interface

    /**
     * Simplify this message.
     *
     * Called from ChangeSet.build with the intent of minimizing traffic.
     *
     * @return A simplified {@code Message}.
     */
    public Message simplify() {
        Message ret;
        switch (this.messages.size()) {
        case 0:
            ret = (isEmpty()) ? null : this;
            break;
        case 1:
            ret = this.messages.get(0);
            if (this.getStringAttributeMap().isEmpty()) {
                ; // child is good
            } else if (ret instanceof AttributeMessage) {
                ret.setStringAttributeMap(this.getStringAttributeMap());
            } else {
                ret = this;
            }
            break;
        default:
            ret = this;
            break;
        }
        return ret;
    }        


    // Override Object
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(64);
        sb.append('[');
        pretty(sb, getType(), getStringAttributeMap(), null);
        for (Message m : this.messages) sb.append(' ').append(m);
        sb.append(']');
        return sb.toString();
    }
}
