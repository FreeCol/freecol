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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.ai.AIPlayer;
import net.sf.freecol.server.control.InGameController;
import net.sf.freecol.server.control.PreGameController;
import net.sf.freecol.server.model.ServerPlayer;


/**
 * The basic trivial message, with just a name.
 */
public abstract class TrivialMessage extends Message {

    /*
     * True trivial messages have no distinguishing parts, so we might
     * as well just use some explicit constants.
     */
    public static final ContinueMessage continueMessage
        = new ContinueMessage();
    public static final DisconnectMessage disconnectMessage
        = new DisconnectMessage();
    public static final EndTurnMessage endTurnMessage
        = new EndTurnMessage();
    public static final EnterRevengeModeMessage enterRevengeModeMessage
        = new EnterRevengeModeMessage();
    public static final ReconnectMessage reconnectMessage
        = new ReconnectMessage();
    public static final RequestLaunchMessage requestLaunchMessage
        = new RequestLaunchMessage();
    public static final RetireMessage retireMessage
        = new RetireMessage();
    public static final StartGameMessage startGameMessage
        = new StartGameMessage();

    /** The actual message type. */
    private String type;


    /**
     * Create a new {@code TrivialMessage} of a given type.
     *
     * @param type The message type.
     */
    protected TrivialMessage(String type) {
        super();

        this.type = type;
    }

    /**
     * Create a new {@code TrivialMessage} from a stream.
     *
     * Note: only call this from direct subclasses of TrivialMessage as it
     * consumes the whole message.
     *
     * @param tag The message tag.
     * @param game The {@code Game} this message belongs to.
     * @param xr The {@code FreeColXMLReader} to read from.
     * @exception XMLStreamException if the stream is corrupt.
     */
    protected TrivialMessage(String tag, @SuppressWarnings("unused") Game game,
                             FreeColXMLReader xr) throws XMLStreamException {
        this(tag);

        xr.closeTag(tag);
    }


    // Implement Message

    /**
     * {@inheritDoc}
     */
    public String getType() {
        return this.type;
    }

    /**
     * {@inheritDoc}
     */
    protected void setType(String type) {
        this.type = type;
    }

    /**
     * {@inheritDoc}
     */
    protected boolean hasAttribute(String key) {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    protected String getStringAttribute(String key) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    protected void setStringAttribute(String key, String value) {
        if (key == null || value == null) {
            ; // Always OK to set nothing
        } else { // Nope
            throw new RuntimeException(getType() + ".setStringAttribute NYI");
        }
    }
    
    /**
     * {@inheritDoc}
     */
    protected Map<String,String> getStringAttributeMap() {
        return Collections.<String,String>emptyMap();
    }

    /**
     * {@inheritDoc}
     */
    protected int getChildCount() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    protected List<FreeColObject> getChildren() {
        return Collections.<FreeColObject>emptyList();
    }
    
    /**
     * {@inheritDoc}
     */
    protected void setChildren(List<? extends FreeColObject> fcos) {
        if (fcos == null || fcos.isEmpty()) {
            ; // Always OK to set nothing
        } else { // Nope
            throw new RuntimeException(getType() + ".setChildren NYI");
        }
    }
    
    /**
     * {@inheritDoc}
     */
    protected <T extends FreeColObject> void appendChild(T fco) {
        if (fco == null) {
            ; // Always OK to add nothing
        } else {
            throw new RuntimeException(getType() + ".append NYI");
        }
    }

    /**
     * {@inheritDoc}
     */
    protected <T extends FreeColObject> void appendChildren(Collection<T> fcos) {
        if (fcos == null) {
            ; // Always OK to add nothing
        } else {
            throw new RuntimeException(getType() + ".append NYI");
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean currentPlayerMessage() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public MessagePriority getPriority() {
        return Message.MessagePriority.NORMAL;
    }

    /**
     * {@inheritDoc}
     */
    public void aiHandler(FreeColServer freeColServer, AIPlayer aiPlayer)
        throws FreeColException {
        throw new FreeColException(getType() + " aiHandler NYI");
    }

    /**
     * {@inheritDoc}
     */
    public void clientHandler(FreeColClient freeColClient)
        throws FreeColException {
        throw new FreeColException(getType() + " clientHandler NYI");
    }

    /**
     * {@inheritDoc}
     */
    public ChangeSet serverHandler(FreeColServer freeColServer,
                                   ServerPlayer serverPlayer) {
        return serverPlayer.clientError("Invalid message type: " + getType());
    }
}
