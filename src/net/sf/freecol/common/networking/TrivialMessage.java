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
import net.sf.freecol.server.control.InGameController;
import net.sf.freecol.server.control.PreGameController;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 * The basic trivial message, with just a name.
 */
public abstract class TrivialMessage extends DOMMessage {

    /*
     * True trivial messages have no distinguishing parts, so we might
     * as well just use some explicit constants.
     */
    public static final CloseMenusMessage closeMenusMessage
        = new CloseMenusMessage();
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
    private final String type;


    /**
     * Create a new {@code TrivialMessage} of a given type.
     *
     * @param type The message type.
     */
    protected TrivialMessage(String type) {
        super(type);

        this.type = type;
    }

    /**
     * Create a new {@code TrivialMessage} from a supplied element.
     *
     * @param tag The message tag.
     * @param game The {@code Game} this message belongs to.
     * @param element The {@code Element} to use to create the message.
     */
    protected TrivialMessage(String tag, @SuppressWarnings("unused") Game game,
                             Element element) {
        this(tag);
    }

    /**
     * Create a new {@code TrivialMessage} from a stream.
     *
     * @param tag The message tag.
     * @param game The {@code Game} this message belongs to.
     * @param xr The {@code FreeColXMLReader} to read from.
     * @exception XMLStreamException if the stream is corrupt.
     * @exception FreeColException if the internal message can not be read.
     */
    protected TrivialMessage(String tag, @SuppressWarnings("unused") Game game,
                             FreeColXMLReader xr)
        throws FreeColException, XMLStreamException {
        this(tag);

        xr.nextTag();
        xr.closeTag(tag);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String getType() {
        return this.type;
    }

    // Do not override DOMMessage.setType, yet
    ///**
    // * {@inheritDoc}
    // */
    //@Override
    //public void setType(String type) {
    //    throw new RuntimeException("Reset of type: " + type);
    //}

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasAttribute(String key) {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getStringAttribute(String key) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setStringAttribute(String key, String value) {}
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String,String> getStringAttributes() {
        return Collections.<String,String>emptyMap();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public List<FreeColObject> getChildren() {
        return Collections.<FreeColObject>emptyList();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void setChildren(List<? extends FreeColObject> fcos) {
        throw new RuntimeException("TrivialMessage.setChildren not implemented");
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
    public void toXML(FreeColXMLWriter xw) throws XMLStreamException {
        // FIXME: When TrivialMessage is no longer a DOMMessage this
        // routine is redundant.
        xw.writeStartElement(getType());

        writeAttributes(xw);

        writeChildren(xw);

        xw.writeEndElement();
    }


    // Convenience methods for the subclasses

    protected net.sf.freecol.client.control.InGameController
        igc(FreeColClient freeColClient) {
        return freeColClient.getInGameController();
    }

    protected net.sf.freecol.server.control.InGameController
        igc(FreeColServer freeColServer) {
        return freeColServer.getInGameController();
    }

    protected void invokeAndWait(FreeColClient freeColClient,
                                 Runnable runnable) {
        freeColClient.getGUI().invokeNowOrWait(runnable);
    }

    protected void invokeLater(FreeColClient freeColClient,
                               Runnable runnable) {
        freeColClient.getGUI().invokeNowOrLater(runnable);
    }

    protected net.sf.freecol.client.control.PreGameController
        pgc(FreeColClient freeColClient) {
        return freeColClient.getPreGameController();
    }

    protected net.sf.freecol.server.control.PreGameController
        pgc(FreeColServer freeColServer) {
        return freeColServer.getPreGameController();
    }
}
