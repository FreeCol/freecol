/**
 *  Copyright (C) 2002-2015   The FreeCol Team
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
 * The message that contains a chat string.
 */
public class ChatMessage extends DOMMessage {

    /** The object identifier of the sender player. */
    private String sender;

    /** The text of the message. */
    private final String message;

    /** Whether this is a private message or not. */
    private final boolean privateChat;


    /**
     * Create a new <code>ChatMessage</code> with the
     * supplied message.
     *
     * @param player The player that is sending the message.
     * @param message The text of the message to send.
     * @param privateChat Whether this message is private.
     */
    public ChatMessage(Player player, String message, boolean privateChat) {
        super(getXMLElementTagName());

        this.sender = player.getId();
        this.message = message;
        this.privateChat = privateChat;
    }

    /**
     * Create a new <code>ChatMessage</code> from a
     * supplied element.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     * @throws IllegalStateException if there is problem with the senderID.
     */
    public ChatMessage(Game game, Element element) {
        super(getXMLElementTagName());

        sender = element.getAttribute("sender");
        message = element.getAttribute("message");
        privateChat = Boolean.parseBoolean(element.getAttribute("privateChat"));
    }


    // Public interface

    /**
     * Who sent this ChatMessage?
     *
     * @param game The <code>Game</code> the player is in.
     * @return The player that sent this ChatMessage.
     */
    public Player getPlayer(Game game) {
        return game.getFreeColGameObject(sender, Player.class);
    }

    /**
     * What is the text of this ChatMessage?
     *
     * @return The text of this ChatMessage.
     */
    public String getMessage() {
        return message;
    }

    /**
     * Is this ChatMessage private?
     *
     * @return True if this ChatMessage is private.
     */
    public boolean isPrivate() {
        return privateChat;
    }


    /**
     * Handle a "chat"-message.
     *
     * @param server The <code>FreeColServer</code> that handles the message.
     * @param connection The <code>Connection</code> message was received on.
     *
     * @return Null.
     */
    public Element handle(FreeColServer server, Connection connection) {
        final ServerPlayer serverPlayer = server.getPlayer(connection);

        /* Do not trust the client-supplied sender name */
        sender = serverPlayer.getId();

        return server.getInGameController().chat(serverPlayer, message,
                                                 privateChat);
    }

    /**
     * Convert this ChatMessage to XML.
     *
     * @return The XML representation of this message.
     */
    @Override
    public Element toXMLElement() {
        return createMessage(getXMLElementTagName(),
            "sender", sender,
            "message", message,
            "privateChat", String.valueOf(privateChat));
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "chat".
     */
    public static String getXMLElementTagName() {
        return "chat";
    }
}
