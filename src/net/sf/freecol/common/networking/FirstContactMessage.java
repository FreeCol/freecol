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
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 * The message sent when making first contact between players.
 */
public class FirstContactMessage extends TrivialMessage {

    public static final String TAG = "firstContact";
    private static final String CAMPS_TAG = "camps";
    private static final String OTHER_TAG = "other";
    private static final String PLAYER_TAG = "player";
    private static final String RESULT_TAG = "result";
    private static final String TILE_TAG = "tile";


    /**
     * Create a new {@code FirstContactMessage}.
     *
     * @param player The {@code Player} making contact.
     * @param other The {@code Player} being contacted.
     * @param tile An optional {@code Tile} to offer.
     */
    public FirstContactMessage(Player player, Player other, Tile tile) {
        super(TAG, PLAYER_TAG, player.getId(), OTHER_TAG, other.getId(),
              TILE_TAG, (tile == null) ? null : tile.getId(),
              CAMPS_TAG, ((other.isEuropean()) ? null
                  : String.valueOf(other.getSettlementCount())));
    }

    /**
     * Create a new {@code FirstContactMessage} from a
     * supplied element.
     *
     * @param game The {@code Game} this message belongs to.
     * @param element The {@code Element} to use to create the message.
     */
    public FirstContactMessage(Game game, Element element) {
        super(TAG, PLAYER_TAG, getStringAttribute(element, PLAYER_TAG),
              OTHER_TAG, getStringAttribute(element, OTHER_TAG),
              TILE_TAG, getStringAttribute(element, TILE_TAG),
              CAMPS_TAG, getStringAttribute(element, CAMPS_TAG),
              RESULT_TAG, getStringAttribute(element, RESULT_TAG));
    }


    // Public interface

    public Player getPlayer(Game game) {
        return game.getFreeColGameObject(getAttribute(PLAYER_TAG), Player.class);
    }

    public Player getOtherPlayer(Game game) {
        return game.getFreeColGameObject(getAttribute(OTHER_TAG), Player.class);
    }

    public Tile getTile(Game game) {
        return game.getFreeColGameObject(getAttribute(TILE_TAG), Tile.class);
    }

    public int getSettlementCount() {
        return getIntegerAttribute(CAMPS_TAG);
    }
            
    public boolean getResult() {
        return getBooleanAttribute(RESULT_TAG);
    }

    public FirstContactMessage setResult(boolean result) {
        setAttribute(RESULT_TAG, String.valueOf(result));
        return this;
    }


    /**
     * Handle a "firstContact"-message.
     *
     * @param server The {@code FreeColServer} handling the message.
     * @param connection The {@code Connection} message was received on.
     * @return An update containing the firstContactd unit, or an error
     *     {@code Element} on failure.
     */
    public Element handle(FreeColServer server, Connection connection) {
        final ServerPlayer serverPlayer = server.getPlayer(connection);
        final Game game = serverPlayer.getGame();
        final String playerId = getAttribute(PLAYER_TAG);
        final String otherId = getAttribute(OTHER_TAG);

        Player first = getPlayer(game);
        if (first == null) {
            return serverPlayer.clientError("Invalid player: " + playerId)
                .build(serverPlayer);
        } else if (serverPlayer.getId().equals(playerId)) {
            ; // OK
        } else {
            return serverPlayer.clientError("Not our player: " + playerId)
                .build(serverPlayer);
        }

        ServerPlayer otherPlayer = (ServerPlayer)getOtherPlayer(game);
        if (otherPlayer == null) {
            return serverPlayer.clientError("Invalid other player: " + otherId)
                .build(serverPlayer);
        } else if (otherPlayer == serverPlayer) {
            return serverPlayer.clientError("First contact with self!?!")
                .build(serverPlayer);
        }

        // Proceed to contact.
        return server.getInGameController()
            .nativeFirstContact(serverPlayer, otherPlayer,
                                getTile(game), getResult())
            .build(serverPlayer);
    }
}
