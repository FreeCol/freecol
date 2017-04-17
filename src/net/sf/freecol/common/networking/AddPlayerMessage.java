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
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.ai.AIPlayer;
import net.sf.freecol.server.model.ServerPlayer;

import net.sf.freecol.common.util.DOMUtils;
import org.w3c.dom.Element;


/**
 * The message sent to add or update players to the game.
 */
public class AddPlayerMessage extends ObjectMessage {

    public static final String TAG = "addPlayer";

    /** The players to add. */
    private final List<Player> players = new ArrayList<>();


    /**
     * Create a new {@code AddPlayerMessage}.
     */
    public AddPlayerMessage() {
        super(TAG);

        this.players.clear();
    }

    /**
     * Create a new {@code AddPlayerMessage} from a supplied
     * element.
     *
     * @param game The {@code Game} this message belongs to.
     * @param element The {@code Element} to use to create the message.
     */
    public AddPlayerMessage(Game game, Element element) {
        this();

        // This implicitly updates the game.
        // TODO: should this do a non-interning read and have the client
        // handlers do more checking?
        this.players.addAll(DOMUtils.getChildren(game, element, Player.class));
    }

    /**
     * Create a new {@code AddPlayerMessage} from a stream.
     *
     * @param game The {@code Game} this message belongs to.
     * @param xr The {@code FreeColXMLReader} to read from.
     * @exception XMLStreamException if there is a problem reading the stream.
     */
    public AddPlayerMessage(Game game, FreeColXMLReader xr)
        throws XMLStreamException {
        this();

        while (xr.moreTags()) {
            String tag = xr.getLocalName();
            if (Player.TAG.equals(tag)) {
                Player p = xr.readFreeColObject(game, Player.class);
                if (p != null) this.players.add(p);
            } else {
                expected(Player.TAG, tag);
            }
        }
        xr.expectTag(TAG);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public MessagePriority getPriority() {
        return Message.MessagePriority.LATE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Element toXMLElement() {
        return new DOMMessage(TAG)
            .add(this.players).toXMLElement();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeChildren(FreeColXMLWriter xw) throws XMLStreamException {
        for (Player p : this.players) p.toXML(xw);
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
    public void clientHandler(FreeColClient client) {
        // Do not need to do anything, reading the player in does
        // enough for now.
    }


    // Public interface

    /**
     * Get the attached players.
     *
     * @return The list of {@code Player}s to add.
     */
    public List<Player> getPlayers() {
        return this.players;
    }

    /**
     * Add a player to the message.
     *
     * @param p The {@code Player} to add.
     * @return This message.
     */
    public AddPlayerMessage addPlayer(Player p) {
        this.players.add(p);
        return this;
    }
}
