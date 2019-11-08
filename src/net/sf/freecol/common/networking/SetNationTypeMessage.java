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
import net.sf.freecol.common.model.NationType;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;


/**
 * The message that sets the player nationType.
 */
public class SetNationTypeMessage extends AttributeMessage {

    public static final String TAG = "setNationType";
    private static final String PLAYER_TAG = "player";
    private static final String VALUE_TAG = "value";
    

    /**
     * Create a new {@code SetNationTypeMessage}.
     *
     * @param player The {@code Player} to change the type for.
     * @param nationType The {@code NationType} to set.
     */
    public SetNationTypeMessage(Player player, NationType nationType) {
        super(TAG, PLAYER_TAG, (player == null) ? null : player.getId(),
              VALUE_TAG, nationType.getId());
    }

    /**
     * Create a new {@code SetNationTypeMessage} from a stream.
     *
     * @param game The {@code Game} this message belongs to (null here).
     * @param xr The {@code FreeColXMLReader} to read from.
     * @exception XMLStreamException if the stream is corrupt.
     */
    public SetNationTypeMessage(Game game, FreeColXMLReader xr)
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
        final NationType nationType = getValue(game.getSpecification());

        pgc(freeColClient).setNationTypeHandler(nationType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ChangeSet serverHandler(FreeColServer freeColServer,
                                   ServerPlayer serverPlayer) {
        if (serverPlayer == null) {
            logger.warning("setNationType from unknown connection.");
            return null;
        }
        
        final Game game = freeColServer.getGame();
        final Specification spec = game.getSpecification();
        final NationType fixedNationType
            = spec.getNation(serverPlayer.getNationId()).getType();
        final NationType nationType = getValue(spec);
        boolean ok;
        switch (game.getNationOptions().getNationalAdvantages()) {
        case SELECTABLE:
            ok = true;
            break;
        case FIXED:
            if (nationType.equals(fixedNationType)) return null;
            ok = false;
            break;
        case NONE:
            ok = nationType == spec.getDefaultNationType();
            break;
        default:
            ok = false;
            break;
        }
        if (!ok) {
            return serverPlayer.clientError(StringTemplate
                .template("server.badNationType")
                .addName("%nationType%", String.valueOf(nationType)));
        }

        return pgc(freeColServer)
            .setNationType(serverPlayer, nationType);
    }


    // Public interface

    /**
     * Get the player that is setting its nationType.
     *
     * @param game The {@code Game} to look up the player in.
     * @return The {@code Player} found, or null.
     */
    public Player getPlayer(Game game) {
        return game.getFreeColGameObject(getStringAttribute(PLAYER_TAG), Player.class);
    }

    /**
     * Get the nationType to set.
     *
     * @param spec The {@code Specification} to look up the nationType in.
     * @return The {@code NationType}.
     */
    public NationType getValue(Specification spec) {
        return spec.getNationType(getStringAttribute(VALUE_TAG));
    }
}
