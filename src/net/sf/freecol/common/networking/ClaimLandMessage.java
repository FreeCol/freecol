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

import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Player.NoClaimReason;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 * The message sent when the client requests claiming land.
 */
public class ClaimLandMessage extends DOMMessage {

    public static final String TAG = "claimLand";
    private static final String CLAIMANT_TAG = "claimant";
    private static final String PRICE_TAG = "price";
    private static final String TILE_TAG = "tile";

    /** The tile to claim. */
    private final String tileId;

    /** The unit or settlement claiming the land. */
    private final String claimantId;

    /** The price to pay for the tile. */
    private final String priceString;


    /**
     * Create a new <code>ClaimLandMessage</code>.
     *
     * @param tile The <code>Tile</code> to claim.
     * @param claimant The <code>Unit</code> or <code>Settlement</code>
     *     claiming the tile.
     * @param price The price to pay for the tile, negative if stealing.
     */
    public ClaimLandMessage(Tile tile, FreeColGameObject claimant, int price) {
        super(getTagName());

        this.tileId = tile.getId();
        this.claimantId = claimant.getId();
        this.priceString = Integer.toString(price);
    }

    /**
     * Create a new <code>ClaimLandMessage</code> from a supplied element.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     */
    public ClaimLandMessage(Game game, Element element) {
        super(getTagName());

        this.tileId = getStringAttribute(element, TILE_TAG);
        this.claimantId = getStringAttribute(element, CLAIMANT_TAG);
        this.priceString = getStringAttribute(element, PRICE_TAG);
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
    public Element handle(FreeColServer server, Player player,
                          Connection connection) {
        final ServerPlayer serverPlayer = server.getPlayer(connection);
        final Game game = server.getGame();

        Tile tile = game.getFreeColGameObject(this.tileId, Tile.class);
        if (tile == null) {
            return serverPlayer.clientError("Not a file: " + this.tileId)
                .build(serverPlayer);
        }

        Unit unit = null;
        try {
            unit = player.getOurFreeColGameObject(this.claimantId, Unit.class);
        } catch (IllegalStateException e) {} // Expected to fail sometimes...
        Settlement settlement = null;
        try {
            settlement = player.getOurFreeColGameObject(this.claimantId,
                                                        Settlement.class);
        } catch (IllegalStateException e) {} // ...as is this one...
        if (unit != null) {
            if (unit.getTile() != tile) {
                return serverPlayer.clientError("Unit not at tile: "
                    + this.tileId)
                    .build(serverPlayer);
            }
        } else if (settlement != null) {
            if (settlement.getOwner().isEuropean()
                && !settlement.getTile().isAdjacent(tile)) {
                return serverPlayer.clientError("Settlement can not claim tile: "
                    + this.tileId)
                    .build(serverPlayer);
            }
        } else { // ...but not both of them.
            return serverPlayer.clientError("Not a unit or settlement: "
                + this.claimantId)
                .build(serverPlayer);
        }

        int price;
        try {
            price = Integer.parseInt(this.priceString);
        } catch (NumberFormatException e) {
            return serverPlayer.clientError("Bad price: " + this.priceString)
                .build(serverPlayer);
        }

        // Request is well formed, but there are more possibilities...
        int value = player.getLandPrice(tile);
        Player owner = tile.getOwner();
        Settlement ownerSettlement = tile.getOwningSettlement();
        if (owner == null) { // unclaimed, always free
            price = 0;
        } else if (owner == player) { // capture vacant colony tiles only
            if (settlement != null && ownerSettlement != null
                && tile.isInUse()) {
                return serverPlayer.clientError("Can not claim tile "
                    + tile.getId() + ": already owned.")
                    .build(serverPlayer);
            }
            price = 0;
        } else if (owner.isEuropean()) {
            if (tile.getOwningSettlement() == null  // its not "nailed down"
                || tile.getOwningSettlement() == settlement) { // pre-attached
                price = 0;
            } else { // must fail
                return serverPlayer.clientError("Can not claim tile " 
                    + tile.getId() + ": European owners will not sell.")
                    .build(serverPlayer);
            }
        } else { // natives
            NoClaimReason why = player.canClaimForSettlementReason(tile);
            switch (why) {
            case NONE:
                break; // Succeed.
            case NATIVES:
                if (price >= 0) {
                    if (price < value) {
                        return serverPlayer.clientError("Can not claim tile "
                            + tile.getId() + ": insufficient offer.")
                            .build(serverPlayer);
                    }
                    if (!player.checkGold(price)) {
                        return serverPlayer.clientError("Can not pay for tile: "
                            + tile.getId() + ": insufficient funds.")
                            .build(serverPlayer);
                    }
                    // Succeed, sufficient offer
                } // else succeed, stealing
                break;
            default: // Fail
                return serverPlayer.clientError("Can not claim tile "
                    + tile.getId() + ": " + why)
                    .build(serverPlayer);
            }
        }

        // Proceed to claim.  Note, does not require unit, it is only
        // required for permission checking above.  Settlement is required
        // to set owning settlement.
        return server.getInGameController()
            .claimLand(serverPlayer, tile, settlement, price)
            .build(serverPlayer);
    }

    /**
     * Convert this ClaimLandMessage to XML.
     *
     * @return The XML representation of this message.
     */
    @Override
    public Element toXMLElement() {
        return new DOMMessage(getTagName(),
            TILE_TAG, this.tileId,
            CLAIMANT_TAG, this.claimantId,
            PRICE_TAG, this.priceString).toXMLElement();
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "claimLand".
     */
    public static String getTagName() {
        return TAG;
    }
}
