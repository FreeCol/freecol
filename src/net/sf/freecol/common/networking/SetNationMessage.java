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
import net.sf.freecol.common.model.Nation;
import net.sf.freecol.common.model.NationOptions.NationState;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.control.ChangeSet;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


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
     * Create a new {@code SetNationMessage} from a supplied element.
     *
     * @param game The {@code Game} this message belongs to (null here).
     * @param element The {@code Element} to use to create the message.
     */
    public SetNationMessage(Game game, Element element) {
        super(TAG, PLAYER_TAG, getStringAttribute(element, PLAYER_TAG),
              VALUE_TAG, getStringAttribute(element, VALUE_TAG));
    }


    // Public interface

    /**
     * Get the player that is setting its nation.
     *
     * @param game The {@code Game} to look up the player in.
     * @return The {@code Player} found, or null.
     */
    public Player getPlayer(Game game) {
        return game.getFreeColGameObject(getAttribute(PLAYER_TAG), Player.class);
    }

    /**
     * Get the nation to set.
     *
     * @param spec The {@code Specification} to look up the nation in.
     * @return The {@code Nation}.
     */
    public Nation getValue(Specification spec) {
        return spec.getNation(getAttribute(VALUE_TAG));
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
            Player other = getPlayer(game);
            if (other != null && (ServerPlayer)other != serverPlayer) {
                logger.warning("Player " + other.getId()
                    + " set from " + serverPlayer.getId());
                return null;
            }
            Nation nation = getValue(spec);
            if (nation != null
                && game.getNationOptions().getNations().get(nation)
                    == NationState.AVAILABLE) {
                serverPlayer.setNation(nation);
                freeColServer.sendToAll(new SetNationMessage(serverPlayer, nation),
                                        serverPlayer);
            } else {
                return serverPlayer.clientError(StringTemplate
                    .template("server.badNation")
                    .addName("%nation%", (nation == null) ? "null"
                        : nation.getId()));
            }
        } else {
            logger.warning("setNation from unknown connection.");
        }
        return null;
    }
}
