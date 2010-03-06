/**
 *  Copyright (C) 2002-2007  The FreeCol Team
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

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;


/**
 * The message sent when the client requests claiming land.
 */
public class ClaimLandMessage extends Message {
	public static int STEAL_LAND = -1;
    /**
     * The ID of the tile to claim.
     */
    private String tileId;

    /**
     * The ID of the Settlement to own the tile, if any.
     */
    private String settlementId;

    /**
     * The price to pay for the tile.
     */
    private int price;


    /**
     * Create a new <code>ClaimLandMessage</code>.
     *
     * @param tile The <code>Tile</code> to claim.
     * @param settlement The <code>Settlement</code> to own the tile, if any.
     * @param price The price to pay for the tile, negative if stealing.
     */
    public ClaimLandMessage(Tile tile, Settlement settlement, int price) {
        this.tileId = tile.getId();
        this.settlementId = (settlement == null) ? null : settlement.getId();
        this.price = price;
    }

    /**
     * Create a new <code>ClaimLandMessage</code> from a supplied element.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     */
    public ClaimLandMessage(Game game, Element element) {
        this.tileId = element.getAttribute("tile");
        this.settlementId = (element.hasAttribute("settlement"))
            ? element.getAttribute("settlement")
            : null;
        this.price = Integer.parseInt(element.getAttribute("price"));
    }

    /**
     * Handle a "claimLand"-message.
     *
     * @param server The <code>FreeColServer</code> handling the message.
     * @param player The <code>Player</code> the message applies to.
     * @param connection The <code>Connection</code> the message was from.
     *
     * @return An update, or error <code>Element</code> on failure.
     */
    public Element handle(FreeColServer server, Player player, Connection connection) {
        ServerPlayer serverPlayer = server.getPlayer(connection);
        Game game = server.getGame();
        Tile tile;
        if (game.getFreeColGameObjectSafely(tileId) instanceof Tile) {
            tile = (Tile) game.getFreeColGameObjectSafely(tileId);
        } else {
            return Message.clientError("Invalid tileId");
        }
        Settlement settlement;
        if (settlementId == null) {
            settlement = null;
        } else if (game.getFreeColGameObjectSafely(settlementId) instanceof Settlement) {
            settlement = (Settlement) game.getFreeColGameObjectSafely(settlementId);
        } else {
            return Message.clientError("Invalid settlementId");
        }

        // Request is well formed, but there are more possibilities...
        int value = player.getLandPrice(tile);
        Player owner = tile.getOwner();
        Settlement ownerSettlement = tile.getOwningSettlement();
        if (owner == null) { // unclaimed, always free
            price = 0;
        } else if (owner == player) { // capture vacant colony tiles only
            if (settlement != null
                && ownerSettlement != null
                && ownerSettlement instanceof Colony
                && ((Colony) ownerSettlement).getColonyTile(tile).getUnit() != null) {
                return Message.createError("tileTakenSelf", null);
            }
            price = 0;
        } else if (owner.isEuropean()) {
            if (tile.getOwningSettlement() == null  // its not "nailed down"
                || tile.getOwningSettlement() == settlement) { // pre-attached
                price = 0;
            } else { // must fail
                return Message.createError("tileTakenEuro", null);
            }
        } else { // natives
            if (price < 0 || price >= value) { // price is valid
                ;
            } else { // refuse
                return Message.createError("tileTakenInd", null);
            }
        }

        // All is well
        serverPlayer.claimLand(tile, settlement, price);
        server.getInGameController().sendUpdateToAll(serverPlayer, (FreeColObject) tile);

        // Update the tile, and any now-angrier owners, and the player gold
        // if a price was paid.
        Element reply = Message.createNewRootElement("update");
        Document doc = reply.getOwnerDocument();
        reply.appendChild(tile.toXMLElement(player, doc));
        if (price < 0) {
            if (ownerSettlement != null) {
                reply.appendChild(ownerSettlement.toXMLElement(player, doc, false, false));
                // previous owner will be less happy, so an update is required
                reply.appendChild(ownerSettlement.getOwner().toXMLElement(player, doc, false, false));
            }
            else{
            	logger.warning("Stealing land from a non-existing settlement");
            }
        } else if (price > 0) {
            reply.appendChild(player.toXMLElementPartial(doc, "gold"));
        }
        return reply;
    }

    /**
     * Convert this ClaimLandMessage to XML.
     *
     * @return The XML representation of this message.
     */
    public Element toXMLElement() {
        Element result = createNewRootElement(getXMLElementTagName());
        result.setAttribute("tile", tileId);
        if (settlementId != null) {
            result.setAttribute("settlement", settlementId);
        }
        result.setAttribute("price", Integer.toString(price));
        return result;
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "claimLand".
     */
    public static String getXMLElementTagName() {
        return "claimLand";
    }
}
