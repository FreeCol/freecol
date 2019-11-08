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

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.ai.AIPlayer;


/**
 * The message sent to add or update players to the game.
 */
public class AddPlayerMessage extends ObjectMessage {

    public static final String TAG = "addPlayer";

    /** The player to specialize the players for. */
    private final Player destination;


    /**
     * Create a new {@code AddPlayerMessage}.
     *
     * @param destination An optional {@code Player} that will see this message.
     * @param players A list of {@code Player}s to add.
     */
    public AddPlayerMessage(Player destination, List<Player> players) {
        super(TAG);

        this.destination = destination;
        appendChildren(players);
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
        super(TAG);

        this.destination = null;
        FreeColXMLReader.ReadScope rs
            = xr.replaceScope(FreeColXMLReader.ReadScope.NOINTERN);
        List<Player> players = new ArrayList<>();
        try {
            while (xr.moreTags()) {
                String tag = xr.getLocalName();
                if (Player.TAG.equals(tag)) {
                    Player p = xr.readFreeColObject(game, Player.class);
                    if (p != null) players.add(p);
                } else {
                    expected(Player.TAG, tag);
                }
                xr.expectTag(tag);
            }
            xr.expectTag(TAG);
        } finally {
            xr.replaceScope(rs);
        }
        appendChildren(players);
    }


    /**
     * Get the attached players.
     *
     * @return The list of {@code Player}s to add.
     */
    private List<Player> getPlayers() {
        return getChildren(Player.class);
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
    public void toXML(FreeColXMLWriter xw) throws XMLStreamException {
        FreeColXMLWriter.WriteScope ws = null;
        if (this.destination != null) {
            ws = xw.replaceScope(FreeColXMLWriter.WriteScope
                .toClient(this.destination));
        }
        super.toXML(xw);
        if (this.destination != null) xw.replaceScope(ws);
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
        List<Player> players = getPlayers();
        
        if (freeColClient.isInGame()) {
            igc(freeColClient).addPlayerHandler(players);
        } else {
            pgc(freeColClient).addPlayerHandler(players);
        }
    }
}
