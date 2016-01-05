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
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 * The message sent when an error occurs.
 */
public class ErrorMessage extends DOMMessage {

    public static final String MESSAGE_ID_TAG = "messageId";
    public static final String MESSAGE_TAG = "message";

    /** A message identifier, if available. */
    private final String messageId;

    /** A more detailed but non-i18n message for logging/debugging. */
    private final String message;


    /**
     * Create a new <code>ErrorMessage</code> with the given message
     * identifier and message.
     *
     * @param messageId The message identifier.
     * @param message The message.
     */
    public ErrorMessage(String messageId, String message) {
        super(getTagName());

        this.messageId = messageId;
        this.message = message;
    }

    /**
     * Create a new <code>ErrorMessage</code> from a
     * supplied element.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     */
    public ErrorMessage(Game game, Element element) {
        super(getTagName());

        this.messageId = element.getAttribute(MESSAGE_ID_TAG);
        this.message = element.getAttribute(MESSAGE_TAG);
    }


    /**
     * Get the message identifier.
     *
     * @return The message identifier.
     */
    public String getMessageId() {
        return this.messageId;
    }
    
    /**
     * Get the non-i18n message.
     *
     * @return The message.
     */
    public String getMessage() {
        return this.message;
    }
    
    /**
     * Handle a "error"-message.
     *
     * @param server The <code>FreeColServer</code> handling the message.
     * @param player The <code>Player</code> the message applies to.
     * @param connection The <code>Connection</code> message was received on.
     *
     * @return An update containing the errord unit,
     *         or an error <code>Element</code> on failure.
     */
    public Element handle(FreeColServer server, Player player,
                          Connection connection) {
        // Not needed, error messages are only sent by the server
        return null;
    }

    /**
     * Convert this ErrorMessage to XML.
     *
     * @return The XML representation of this message.
     */
    @Override
    public Element toXMLElement() {
        DOMMessage result = new DOMMessage(getTagName());
        if (this.messageId != null) {
            result.setAttribute(MESSAGE_ID_TAG, this.messageId);
        }
        if (this.message != null) {
            result.setAttribute(MESSAGE_TAG, this.message);
        }
        return result.toXMLElement();
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "error".
     */
    public static String getTagName() {
        return "error";
    }
}
