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
import net.sf.freecol.common.model.NationType;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.control.ChangeSet;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


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
     * Create a new {@code SetNationTypeMessage} from a supplied element.
     *
     * @param game The {@code Game} this message belongs to (null here).
     * @param element The {@code Element} to use to create the message.
     */
    public SetNationTypeMessage(Game game, Element element) {
        super(TAG, PLAYER_TAG, getStringAttribute(element, PLAYER_TAG),
              VALUE_TAG, getStringAttribute(element, VALUE_TAG));
    }


    // Public interface

    /**
     * Get the player that is setting its nationType.
     *
     * @param game The {@code Game} to look up the player in.
     * @return The {@code Player} found, or null.
     */
    public Player getPlayer(Game game) {
        return game.getFreeColGameObject(getAttribute(PLAYER_TAG), Player.class);
    }

    /**
     * Get the nationType to set.
     *
     * @param spec The {@code Specification} to look up the nationType in.
     * @return The {@code NationType}.
     */
    public NationType getValue(Specification spec) {
        return spec.getNationType(getAttribute(VALUE_TAG));
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public ChangeSet serverHandler(FreeColServer freeColServer,
                                   ServerPlayer serverPlayer) {
        final Game game = freeColServer.getGame();
        final Specification spec = game.getSpecification();

        if (serverPlayer != null) {
            NationType fixedNationType
                = spec.getNation(serverPlayer.getNationId()).getType();
            NationType nationType = getValue(spec);
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
            if (ok) {
                serverPlayer.changeNationType(nationType);
                freeColServer.sendToAll(new SetNationTypeMessage(serverPlayer, nationType),
                                        serverPlayer);
            } else {
                return serverPlayer.clientError(StringTemplate
                    .template("server.badNationType")
                    .addName("%nationType%", String.valueOf(nationType)));
            }
        } else {
            logger.warning("setNationType from unknown connection.");
        }
        return null;
    }
}
