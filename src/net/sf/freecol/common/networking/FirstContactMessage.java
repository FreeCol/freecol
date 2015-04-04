/**
 *  Copyright (C) 2002-2015   The FreeCol Team
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
        super(getXMLElementTagName());

        this.playerId = player.getId();
        this.otherId = other.getId();
        this.tileId = (tile == null) ? null : tile.getId();
        this.settlementCount = (other.isEuropean()) ? null
            : Integer.toString(other.getNumberOfSettlements());
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
        super(getXMLElementTagName());

        this.playerId = element.getAttribute("player");
        this.otherId = element.getAttribute("other");
        this.tileId = (!element.hasAttribute("tile")) ? null
            : element.getAttribute("tile");
        this.settlementCount = (!element.hasAttribute("camps")) ? null
            : element.getAttribute("camps");
        this.result = (!element.hasAttribute("result")) ? null
            : element.getAttribute("result");
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
            return DOMMessage.clientError("Invalid player: " + playerId);
        } else if (serverPlayer.getId().equals(playerId)) {
            ; // OK
        } else {
            return DOMMessage.clientError("Not our player: " + playerId);
        }

        ServerPlayer otherPlayer = (ServerPlayer)getOtherPlayer(game);
        if (otherPlayer == null) {
            return DOMMessage.clientError("Invalid other player: " + otherId);
        } else if (otherPlayer == serverPlayer) {
            return DOMMessage.clientError("First contact with self!?!");
        }

        // Proceed to contact.
        return server.getInGameController()
            .nativeFirstContact(serverPlayer, otherPlayer,
                                getTile(game), getResult());
    }

    /**
     * Convert this FirstContactMessage to XML.
     *
     * @return The XML representation of this message.
     */
    @Override
    public Element toXMLElement() {
        Element element = createMessage(getXMLElementTagName(),
            "player", this.playerId,
            "other", this.otherId);
        if (this.tileId != null) {
            element.setAttribute("tile", this.tileId);
        }
        if (this.settlementCount != null) {
            element.setAttribute("camps", this.settlementCount);
        }
        if (this.result != null) {
            element.setAttribute("result", this.result);
        }
        return element;
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "firstContact".
     */
    public static String getXMLElementTagName() {
        return "firstContact";
    }
}
