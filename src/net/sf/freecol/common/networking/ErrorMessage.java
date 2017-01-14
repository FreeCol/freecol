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

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 * The message sent when an error occurs.
 */
public class ErrorMessage extends DOMMessage {

    public static final String TAG = "error";
    private static final String MESSAGE_TAG = "message";

    /** A template to present to the client. */
    private StringTemplate template;

    /** An optional detailed but non-i18n message for logging/debugging. */
    private String message;


    /**
     * Create a new {@code ErrorMessage} with the given template
     * and optional message.
     *
     * @param template The {@code StringTemplate} to send.
     * @param message An optional non-i18n message.
     */
    public ErrorMessage(StringTemplate template, String message) {
        super(TAG);

        this.template = template;
        this.message = message;
    }

    /**
     * Create a new {@code ErrorMessage} with the given template
     *
     * @param template The {@code StringTemplate} to send.
     */
    public ErrorMessage(StringTemplate template) {
        this(template, null);
    }

    /**
     * Create a new {@code ErrorMessage} with the standard client
     * error template and given message.
     *
     * @param message The message.
     */
    public ErrorMessage(String message) {
        this(StringTemplate.template("server.reject"), message);
    }

    /**
     * Create a new {@code ErrorMessage} from an exception with
     * the standard client error template as the fallback.
     *
     * @param ex The {@code Exception} to use.
     */
    public ErrorMessage(Exception ex) {
        this(FreeCol.errorFromException(ex, "server.reject"));
    }

    /**
     * Create a new {@code ErrorMessage} from a
     * supplied element.
     *
     * @param game The {@code Game} this message belongs to.
     * @param element The {@code Element} to use to create the message.
     */
    public ErrorMessage(Game game, Element element) {
        super(TAG);

        this.message = element.getAttribute(MESSAGE_TAG);
        this.template = getChild(game, element, 0, StringTemplate.class);
    }


    // Public interface

    /**
     * Get the template.
     *
     * @return The template.
     */
    public StringTemplate getTemplate() {
        return this.template;
    }
    
    /**
     * Set the template.
     *
     * @param template The new template.
     */
    public void setTemplate(StringTemplate template) {
        this.template = template;
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
     * @param server The {@code FreeColServer} handling the message.
     * @param serverPlayer The {@code ServerPlayer} the message applies to.
     * @return Null.
     */
    public Element handle(FreeColServer server, ServerPlayer serverPlayer) {
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
        return new DOMMessage(TAG,
            MESSAGE_TAG, this.message)
            .add(this.template).toXMLElement();
    }
}
