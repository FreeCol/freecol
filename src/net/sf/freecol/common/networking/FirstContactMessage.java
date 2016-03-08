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
public class FirstContactMessage extends DOMMessage {

    public static final String TAG = "firstContact";
    private static final String CAMPS_TAG = "camps";
    private static final String OTHER_TAG = "other";
    private static final String PLAYER_TAG = "player";
    private static final String RESULT_TAG = "result";
    private static final String TILE_TAG = "tile";

    /** The identifier for the player making contact. */
    private final String playerId;

    /** The identifier for the player being contacted. */
    private final String otherId;

    /**
     * The identifier for a tile to offer the contacting player if this is a
     * first landing and the contacted player is a friendly native.
     */
    private final String tileId;

    /** The number of settlements the contacted player has, if native. */
    private final String settlementCount;

    /** The result of the contact. */
    private String result;


    /**
     * Create a new <code>FirstContactMessage</code>.
     *
     * @param player The <code>Player</code> making contact.
     * @param other The <code>Player</code> being contacted.
     * @param tile An optional <code>Tile</code> to offer.
     */
    public FirstContactMessage(Player player, Player other, Tile tile) {
        super(getTagName());

        this.playerId = player.getId();
        this.otherId = other.getId();
        this.tileId = (tile == null) ? null : tile.getId();
        this.settlementCount = (other.isEuropean()) ? null
            : Integer.toString(other.getSettlements().size());
        this.result = null;
    }

    /**
     * Create a new <code>FirstContactMessage</code> from a
     * supplied element.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     */
    public FirstContactMessage(Game game, Element element) {
        super(getTagName());

        this.playerId = getStringAttribute(element, PLAYER_TAG);
        this.otherId = getStringAttribute(element, OTHER_TAG);
        this.tileId = getStringAttribute(element, TILE_TAG);
        this.settlementCount = getStringAttribute(element, CAMPS_TAG);
        this.result = getStringAttribute(element, RESULT_TAG);
    }


    // Public interface

    public Player getPlayer(Game game) {
        return game.getFreeColGameObject(playerId, Player.class);
    }

    public Player getOtherPlayer(Game game) {
        return game.getFreeColGameObject(otherId, Player.class);
    }

    public Tile getTile(Game game) {
        return game.getFreeColGameObject(tileId, Tile.class);
    }

    public int getSettlementCount() {
        try {
            return Integer.parseInt(settlementCount);
        } catch (NumberFormatException nfe) {}
        return -1;
    }
            
    public boolean getResult() {
        return Boolean.parseBoolean(result);
    }

    public FirstContactMessage setResult(boolean result) {
        this.result = String.valueOf(result);
        return this;
    }


    /**
     * Handle a "firstContact"-message.
     *
     * @param server The <code>FreeColServer</code> handling the message.
     * @param connection The <code>Connection</code> message was received on.
     * @return An update containing the firstContactd unit, or an error
     *     <code>Element</code> on failure.
     */
    public Element handle(FreeColServer server, Connection connection) {
        final ServerPlayer serverPlayer = server.getPlayer(connection);
        final Game game = serverPlayer.getGame();

        Player first = getPlayer(game);
        if (first == null) {
            return serverPlayer.clientError("Invalid player: " + this.playerId)
                .build(serverPlayer);
        } else if (serverPlayer.getId().equals(this.playerId)) {
            ; // OK
        } else {
            return serverPlayer.clientError("Not our player: " + this.playerId)
                .build(serverPlayer);
        }

        ServerPlayer otherPlayer = (ServerPlayer)getOtherPlayer(game);
        if (otherPlayer == null) {
            return serverPlayer.clientError("Invalid other player: "
                + this.otherId)
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

    /**
     * Convert this FirstContactMessage to XML.
     *
     * @return The XML representation of this message.
     */
    @Override
    public Element toXMLElement() {
        return new DOMMessage(getTagName(),
            PLAYER_TAG, this.playerId,
            OTHER_TAG, this.otherId,
            TILE_TAG, this.tileId,
            CAMPS_TAG, this.settlementCount,
            RESULT_TAG, this.result).toXMLElement();
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "firstContact".
     */
    public static String getTagName() {
        return TAG;
    }
}
