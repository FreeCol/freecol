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
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Constants.IndianDemandAction;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.ai.AIPlayer;
import net.sf.freecol.server.model.ServerPlayer;


/**
 * The message sent to resolve natives making demands of a colony.
 */
public class IndianDemandMessage extends AttributeMessage {

    public static final String TAG = "indianDemand";
    private static final String AMOUNT_TAG = "amount";
    private static final String COLONY_TAG = "colony";
    private static final String RESULT_TAG = "result";
    private static final String TYPE_TAG = "type";
    private static final String UNIT_TAG = "unit";


    /**
     * Create a new {@code IndianDemandMessage} with the
     * supplied unit, colony and demands.
     *
     * @param unit The {@code Unit} that is demanding.
     * @param colony The {@code Colony} being demanded of.
     * @param type The {@code GoodsType} being demanded.
     * @param amount The amount of goods being demanded.
     */
    public IndianDemandMessage(Unit unit, Colony colony,
                               GoodsType type, int amount) {
        super(TAG, UNIT_TAG, unit.getId(), COLONY_TAG, colony.getId(),
              TYPE_TAG, (type == null) ? null : type.getId(),
              AMOUNT_TAG, String.valueOf(amount));
    }

    /**
     * Create a new {@code IndianDemandMessage} from a stream.
     *
     * @param game The {@code Game} to read within.
     * @param xr The {@code FreeColXMLReader} to read from.
     * @exception XMLStreamException if the stream is corrupt.
     */
    public IndianDemandMessage(Game game, FreeColXMLReader xr)
        throws XMLStreamException {
        super(TAG, xr, UNIT_TAG, COLONY_TAG, TYPE_TAG, AMOUNT_TAG, RESULT_TAG);
    }


    /**
     * {@inheritDoc}
     */
    public MessagePriority getPriority() {
        return Message.MessagePriority.NORMAL;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void aiHandler(FreeColServer freeColServer, AIPlayer aiPlayer) {
        final Game game = freeColServer.getGame();
        final Unit unit = getUnit(game);
        final Colony colony = getColony(game);
        final GoodsType type = getType(game);
        final int amount = getAmount();
        final IndianDemandAction initialResult = getResult();

        aiPlayer.indianDemandHandler(unit, colony, type, amount, initialResult);
    }
        
    /**
     * {@inheritDoc}
     */
    @Override
    public void clientHandler(FreeColClient freeColClient) {
        final Game game = freeColClient.getGame();
        final Player player = freeColClient.getMyPlayer();
        final Unit unit = getUnit(game);
        final Colony colony = getColony(game);
        final GoodsType goodsType = getType(game);
        final int amount = getAmount();
        
        if (unit == null) {
            logger.warning("IndianDemand with null unit.");
            return;
        }
        if (colony == null) {
            logger.warning("IndianDemand with null colony");
            return;
        } else if (!player.owns(colony)) {
            throw new RuntimeException("Demand to anothers colony: " + colony);
        }

        igc(freeColClient).indianDemandHandler(unit, colony, goodsType, amount);
        clientGeneric(freeColClient);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ChangeSet serverHandler(FreeColServer freeColServer,
                                   ServerPlayer serverPlayer) {
        final Game game = freeColServer.getGame();
        final String unitId = getStringAttribute(UNIT_TAG);
        final String colonyId = getStringAttribute(COLONY_TAG);
        final IndianDemandAction result = getResult();
        
        Unit unit;
        Colony colony;
        try {
            if (serverPlayer.isIndian()) { // Initial demand
                unit = serverPlayer.getOurFreeColGameObject(unitId, Unit.class);
                if (unit.getMovesLeft() <= 0) {
                    return serverPlayer.clientError("Unit has no moves left: "
                        + unitId);
                }
                colony = unit.getAdjacentSettlement(colonyId, Colony.class);
                if (result != IndianDemandAction.INDIAN_DEMAND_DONE) {
                    return serverPlayer.clientError("Result in demand: "
                        + serverPlayer.getId() + " " + result);
                }
            } else { // Reply from colony
                unit = game.getFreeColGameObject(unitId, Unit.class);
                if (unit == null) {
                    return serverPlayer.clientError("Not a unit: "
                        + unitId);
                }
                colony = serverPlayer.getOurFreeColGameObject(colonyId, Colony.class);
                if (result == null) {
                    return serverPlayer.clientError("No result in demand response: "
                        + serverPlayer.getId());
                }
            }
        } catch (Exception e) {
            return serverPlayer.clientError(e.getMessage());
        }

        int amount = getAmount();
        if (amount <= 0) {
            return serverPlayer.clientError("Bad amount: " + amount);
        }

        // Proceed to demand or respond.
        return igc(freeColServer)
            .indianDemand(serverPlayer, unit, colony, getType(game), amount,
                          result);
    }


    // Public interface

    /**
     * Client-side convenience function to get the unit in this message.
     *
     * @param game The {@code Game} to look for the unit in.
     * @return The {@code Unit} found.
     */
    public Unit getUnit(Game game) {
        return game.getFreeColGameObject(getStringAttribute(UNIT_TAG), Unit.class);
    }

    /**
     * Client-side convenience function to get the colony in this message.
     *
     * @param game The {@code Game} to look for the colony in.
     * @return The {@code Colony} found.
     */
    public Colony getColony(Game game) {
        return game.getFreeColGameObject(getStringAttribute(COLONY_TAG), Colony.class);
    }

    /**
     * Client-side convenience function to get the goods type in this message.
     *
     * @param game The {@code Game} to look for the goods type in.
     * @return The {@code GoodsType} found.
     */
    public GoodsType getType(Game game) {
        String typeId = getStringAttribute(TYPE_TAG);
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
        return getIntegerAttribute(AMOUNT_TAG, -1);
    }

    /**
     * Client-side convenience function to set the result of this message.
     *
     * @return The result of this demand.
     */
    public IndianDemandAction getResult() {
        return getEnumAttribute(RESULT_TAG, IndianDemandAction.class,
                                IndianDemandAction.INDIAN_DEMAND_DONE);
    }

    /**
     * Client-side convenience function to set the result of this message.
     *
     * @param result The new result of this demand.
     * @return This message.
     */
    public IndianDemandMessage setResult(IndianDemandAction result) {
        setEnumAttribute(RESULT_TAG, result);
        return this;
    }
}
