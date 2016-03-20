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

import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 * The message sent to resolve natives making demands of a colony.
 */
public class IndianDemandMessage extends DOMMessage {

    public static final String TAG = "indianDemand";
    private static final String AMOUNT_TAG = "amount";
    private static final String COLONY_TAG = "colony";
    private static final String RESULT_TAG = "result";
    private static final String TYPE_TAG = "type";
    private static final String UNIT_TAG = "unit";
    
    /** The identifier of the unit that is demanding. */
    private final String unitId;

    /** The identifier of the colony being demanded of. */
    private final String colonyId;

    /** The type of goods being demanded, null implies gold. */
    private final String typeId;

    /** The amount of goods being demanded. */
    private final String amount;

    /** The result of this demand: null implies not decided yet. */
    private String result;


    /**
     * Create a new <code>IndianDemandMessage</code> with the
     * supplied unit, colony and demands.
     *
     * @param unit The <code>Unit</code> that is demanding.
     * @param colony The <code>Colony</code> being demanded of.
     * @param type The <code>GoodsType</code> being demanded.
     * @param amount The amount of goods being demanded.
     */
    public IndianDemandMessage(Unit unit, Colony colony,
                               GoodsType type, int amount) {
        super(getTagName());

        this.unitId = unit.getId();
        this.colonyId = colony.getId();
        this.typeId = (type == null) ? null : type.getId();
        this.amount = Integer.toString(amount);
        this.result = null;
    }

    /**
     * Create a new <code>IndianDemandMessage</code> from a
     * supplied element.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     */
    public IndianDemandMessage(Game game, Element element) {
        super(getTagName());

        this.unitId = getStringAttribute(element, UNIT_TAG);
        this.colonyId = getStringAttribute(element, COLONY_TAG);
        this.typeId = getStringAttribute(element, TYPE_TAG);
        this.amount = getStringAttribute(element, AMOUNT_TAG);
        this.result = getStringAttribute(element, RESULT_TAG);
    }


    // Public interface

    /**
     * Client-side convenience function to get the unit in this message.
     *
     * @param game The <code>Game</code> to look for the unit in.
     * @return The <code>Unit</code> found.
     */
    public Unit getUnit(Game game) {
        return game.getFreeColGameObject(unitId, Unit.class);
    }

    /**
     * Client-side convenience function to get the colony in this message.
     *
     * @param game The <code>Game</code> to look for the colony in.
     * @return The <code>Colony</code> found.
     */
    public Colony getColony(Game game) {
        return game.getFreeColGameObject(colonyId, Colony.class);
    }

    /**
     * Client-side convenience function to get the goods type in this message.
     *
     * @param game The <code>Game</code> to look for the goods type in.
     * @return The <code>GoodsType</code> found.
     */
    public GoodsType getType(Game game) {
        return (typeId == null) ? null
            : game.getSpecification().getGoodsType(typeId);
    }

    /**
     * Client-side convenience function to get the gold in this message.
     *
     * @return The amount of gold specified by this message, or -1 if
     *     none or invalid.
     */
    public int getAmount() {
        try {
            return Integer.parseInt(amount);
        } catch (NumberFormatException nfe) {}
        return -1;
    }

    /**
     * Client-side convenience function to set the result of this message.
     *
     * @return The result of this demand.
     */
    public Boolean getResult() {
        return (result == null) ? null : Boolean.parseBoolean(result);
    }

    /**
     * Client-side convenience function to set the result of this message.
     *
     * @param result The new result of this demand.
     */
    public void setResult(boolean result) {
        this.result = Boolean.toString(result);
    }


    /**
     * Handle a "indianDemand"-message.
     *
     * @param server The <code>FreeColServer</code> handling the message.
     * @param player The <code>Player</code> the message applies to.
     * @param connection The <code>Connection</code> message was received on.
     * @return An update containing the indianDemandd unit, or an
     *     error <code>Element</code> on failure.
     */
    public Element handle(FreeColServer server, Player player,
                          Connection connection) {
        final ServerPlayer serverPlayer = server.getPlayer(connection);
        final Game game = player.getGame();

        Unit unit;
        try {
            if (result == null) { // Initial demand
                unit = player.getOurFreeColGameObject(this.unitId, Unit.class);
                if (unit.getMovesLeft() <= 0) {
                    return serverPlayer.clientError("Unit has no moves left: "
                        + this.unitId)
                        .build(serverPlayer);
                }
            } else { // Reply from colony
                unit = game.getFreeColGameObject(unitId, Unit.class);
                if (unit == null) {
                    return serverPlayer.clientError("Not a unit: "
                        + this.unitId)
                        .build(serverPlayer);
                }
            }
        } catch (Exception e) {
            return serverPlayer.clientError(e.getMessage())
                .build(serverPlayer);
        }

        Colony colony;
        try {
            Settlement settlement
                = unit.getAdjacentSettlementSafely(this.colonyId);
            if (!(settlement instanceof Colony)) {
                return serverPlayer.clientError("Not a colony: "
                    + this.colonyId)
                    .build(serverPlayer);
            }
            colony = (Colony)settlement;
        } catch (Exception e) {
            return serverPlayer.clientError(e.getMessage())
                .build(serverPlayer);
        }

        if (getAmount() <= 0) {
            return serverPlayer.clientError("Bad amount: " + this.amount)
                .build(serverPlayer);
        }

        // Proceed to demand.
        return server.getInGameController()
            .indianDemand(serverPlayer, unit, colony,
                          getType(game), getAmount())
            .build(serverPlayer);
    }

    /**
     * Convert this IndianDemandMessage to XML.
     *
     * @return The XML representation of this message.
     */
    @Override
    public Element toXMLElement() {
        return new DOMMessage(getTagName(),
            UNIT_TAG, this.unitId,
            COLONY_TAG, this.colonyId,
            AMOUNT_TAG, this.amount,
            TYPE_TAG, this.typeId,
            RESULT_TAG, this.result).toXMLElement();
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "indianDemand".
     */
    public static String getTagName() {
        return TAG;
    }
}
