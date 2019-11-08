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

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Nation;
import net.sf.freecol.common.model.NationOptions.NationState;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;


/**
 * The message that sets the player nation.
 */
public class SetNationMessage extends AttributeMessage {

    public static final String TAG = "setNation";
    private static final String PLAYER_TAG = "player";
    private static final String VALUE_TAG = "value";
    

    /**
     * Create a new {@code SetNationMessage}.
     *
     * @param player The {@code Player} to set the nation for.
     * @param nation The {@code Nation} to set.
     */
    public SetNationMessage(Player player, Nation nation) {
        super(TAG, PLAYER_TAG, (player == null) ? null : player.getId(),
            VALUE_TAG, (nation == null) ? null : nation.getId());
    }

    /**
     * Create a new {@code SetNationMessage} from a stream.
     *
     * @param game The {@code Game} this message belongs to (null here).
     * @param xr The {@code FreeColXMLReader} to read from.
     * @exception XMLStreamException if the stream is corrupt.
     */
    public SetNationMessage(Game game, FreeColXMLReader xr)
        throws XMLStreamException {
        super(TAG, xr, PLAYER_TAG, VALUE_TAG);
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
    public void clientHandler(FreeColClient freeColClient) {
        final Game game = freeColClient.getGame();
        final Player player = getPlayer(game);
        final Nation nation = getValue(game.getSpecification());

        if (player != null && nation != null) {
            player.setNation(nation);
            freeColClient.getGUI().refreshPlayersTable();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ChangeSet serverHandler(FreeColServer freeColServer,
                                   ServerPlayer serverPlayer) {
        if (serverPlayer == null) {
            logger.warning("setNation from unknown connection.");
        }
        
        final Game game = freeColServer.getGame();
        final Player other = getPlayer(game);
        if (other != null && other != (Player)serverPlayer) {
            return serverPlayer.clientError("Player " + other.getId()
                + " set from " + serverPlayer.getId());
        }

        final Specification spec = game.getSpecification();
        final Nation nation = getValue(spec);
        if (nation == null
            || game.getNationOptions().getNations().get(nation) != NationState.AVAILABLE) {
            return serverPlayer.clientError(StringTemplate
                .template("server.badNation")
                .addName("%nation%", (nation == null) ? "null"
                    : nation.getId()));
        }

        return pgc(freeColServer)
            .setNation(serverPlayer, nation);
    }


    // Public interface

    /**
     * Get the player that is setting its nation.
     *
     * @param game The {@code Game} to look up the player in.
     * @return The {@code Player} found, or null.
     */
    public Player getPlayer(Game game) {
        return game.getFreeColGameObject(getStringAttribute(PLAYER_TAG), Player.class);
    }

    /**
     * Get the nation to set.
     *
     * @param spec The {@code Specification} to look up the nation in.
     * @return The {@code Nation}.
     */
    public Nation getValue(Specification spec) {
        return spec.getNation(getStringAttribute(VALUE_TAG));
    }
}
