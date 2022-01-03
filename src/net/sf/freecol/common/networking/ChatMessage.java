/**
 *  Copyright (C) 2002-2022   The FreeCol Team
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

import java.awt.Color;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.ai.AIPlayer;
import net.sf.freecol.server.model.ServerPlayer;


/**
 * The message that contains a chat string.
 */
public class ChatMessage extends AttributeMessage {

    public static final String TAG = "chat";
    private static final String COLOR_TAG = "color";
    private static final String MESSAGE_TAG = "message";
    private static final String PRIVATE_TAG = "private";
    private static final String SENDER_TAG = "sender";


    /**
     * Create a new {@code ChatMessage} with the
     * supplied message.
     *
     * @param player The player that is sending the message.
     * @param message The text of the message to send.
     * @param privateChat Whether this message is private.
     */
    public ChatMessage(Player player, String message, boolean privateChat) {
        super(TAG, COLOR_TAG, String.valueOf(player.getNationColor().getRGB()),
              MESSAGE_TAG, message,
              PRIVATE_TAG, String.valueOf(privateChat),
              SENDER_TAG, player.getName());
    }

    /**
     * Create a new {@code ChatMessage} from a stream.
     *
     * @param game The {@code Game} this message belongs to.
     * @param xr The {@code FreeColXMLReader} to read from.
     * @exception XMLStreamException if the stream is corrupt.
     */
    public ChatMessage(@SuppressWarnings("unused") Game game,
                       FreeColXMLReader xr)
        throws XMLStreamException {
        super(TAG, xr, COLOR_TAG, MESSAGE_TAG, SENDER_TAG, PRIVATE_TAG);
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
    public void aiHandler(FreeColServer freeColServer, AIPlayer aiPlayer) {
        // Ignored
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clientHandler(FreeColClient freeColClient) {
        final Game game = freeColClient.getGame();
        final Color color = getColor();
        final String sender = getSender();
        final String text = getMessage();
        final boolean isPrivate = isPrivate();
        if (sender == null || text == null) return;

        if (freeColClient.isInGame()) {
            igc(freeColClient).chatHandler(sender, text, color, isPrivate);
        } else {
            pgc(freeColClient).chatHandler(sender, text, color, isPrivate);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ChangeSet serverHandler(FreeColServer freeColServer,
                                   ServerPlayer serverPlayer) {
        /* Do not trust the client-supplied sender name */
        setStringAttribute(SENDER_TAG, serverPlayer.getId());

        // IGC routine the same as PGC
        return igc(freeColServer)
            .chat(serverPlayer, getMessage(), isPrivate());
    }        


    // Public interface

    /**
     * Get the color.
     *
     * @return The new {@code Color}.
     */
    public Color getColor() {
        Color color = null;
        try {
            int rgb = getIntegerAttribute(COLOR_TAG, 0);
            color = new Color(rgb);
        } catch (NumberFormatException nfe) {}
        return color;
    }

    /**
     * What is the text of this ChatMessage?
     *
     * @return The text of this ChatMessage.
     */
    public String getMessage() {
        return getStringAttribute(MESSAGE_TAG);
    }

    /**
     * Is this ChatMessage private?
     *
     * @return True if this ChatMessage is private.
     */
    public boolean isPrivate() {
        return getBooleanAttribute(PRIVATE_TAG, Boolean.FALSE);
    }

    /**
     * Who sent this ChatMessage?
     *
     * @return The sender tag for this ChatMessage.
     */
    public String getSender() {
        return getStringAttribute(SENDER_TAG);
    }
}
