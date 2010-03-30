/**
 *  Copyright (C) 2002-2007  The FreeCol Team
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
public class ChatMessage extends Message {
    /**
     * The sending player.
     */
    private Player player;

    /**
     * The ID of the sender player.
     */
    private String sender;

    /**
     * The text of the message.
     */
    private String message;

    /**
     * Whether this is a private message or not.
     */
    private boolean privateChat;

    /**
     * Create a new <code>ChatMessage</code> with the
     * supplied message.
     *
     * @param player The player that is sending the message.
     * @param message The text of the message to send.
     * @param privateChat Whether this message is private.
     */
    public ChatMessage(Player player, String message, boolean privateChat) {
        this.player = player;
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
        sender = element.getAttribute("sender");
        if (sender == null) {
            throw new IllegalStateException("sender is null");
        } else if (!(game.getFreeColGameObject(sender) instanceof Player)) {
            throw new IllegalStateException("not a player: " + sender);
        }
        player = (Player) game.getFreeColGameObject(sender);
        message = element.getAttribute("message");
        privateChat = Boolean.valueOf(element.getAttribute("privateChat")).booleanValue();
    }

    /**
     * Who sent this ChatMessage?
     *
     * @return The name of the player that sent this ChatMessage.
     */
    public Player getPlayer() {
        return player;
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
        ServerPlayer serverPlayer = server.getPlayer(connection);

        /* Do not trust the client-supplied sender name */
        sender = serverPlayer.getId();

        server.getInGameController().sendToOthers(serverPlayer,
                                                  toXMLElement());
        return null;
    }

    /**
     * Convert this ChatMessage to XML.
     *
     * @return The XML representation of this message.
     */
    public Element toXMLElement() {
        Element result = createNewRootElement(getXMLElementTagName());
        result.setAttribute("sender", sender);
        result.setAttribute("message", message);
        result.setAttribute("privateChat", String.valueOf(privateChat));
        return result;
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
