/**
 *  Copyright (C) 2002-2012   The FreeCol Team
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

package net.sf.freecol.server.ai;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.freecol.common.model.BuildableType;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.EquipmentType;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileImprovementType;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.WorkLocation;
import net.sf.freecol.common.networking.AttackMessage;
import net.sf.freecol.common.networking.BuildColonyMessage;
import net.sf.freecol.common.networking.BuyGoodsMessage;
import net.sf.freecol.common.networking.CashInTreasureTrainMessage;
import net.sf.freecol.common.networking.ChangeStateMessage;
import net.sf.freecol.common.networking.ChangeWorkImprovementTypeMessage;
import net.sf.freecol.common.networking.ChangeWorkTypeMessage;
import net.sf.freecol.common.networking.ClaimLandMessage;
import net.sf.freecol.common.networking.ClearSpecialityMessage;
import net.sf.freecol.common.networking.CloseTransactionMessage;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.DOMMessage;
import net.sf.freecol.common.networking.DeliverGiftMessage;
import net.sf.freecol.common.networking.DisembarkMessage;
import net.sf.freecol.common.networking.EmbarkMessage;
import net.sf.freecol.common.networking.EmigrateUnitMessage;
import net.sf.freecol.common.networking.EquipUnitMessage;
import net.sf.freecol.common.networking.GetTransactionMessage;
import net.sf.freecol.common.networking.IndianDemandMessage;
import net.sf.freecol.common.networking.LoadCargoMessage;
import net.sf.freecol.common.networking.LootCargoMessage;
import net.sf.freecol.common.networking.MissionaryMessage;
import net.sf.freecol.common.networking.MoveMessage;
import net.sf.freecol.common.networking.MoveToMessage;
import net.sf.freecol.common.networking.PutOutsideColonyMessage;
import net.sf.freecol.common.networking.ScoutIndianSettlementMessage;
import net.sf.freecol.common.networking.SellGoodsMessage;
import net.sf.freecol.common.networking.SetBuildQueueMessage;
import net.sf.freecol.common.networking.TrainUnitInEuropeMessage;
import net.sf.freecol.common.networking.UnloadCargoMessage;
import net.sf.freecol.common.networking.WorkMessage;

import org.w3c.dom.Element;


/**
 * Wrapper class for AI message handling.
 */
public class AIMessage {

    private static final Logger logger = Logger.getLogger(AIMessage.class.getName());

    /**
     * Ask the server a question.
     *
     * @param connection The <code>Connection</code> to use
     *     when communicating with the server.
     * @param request The <code>Element</code> to send.
     * @return The reply element.
     */
    private static Element askMessage(Connection connection,
                                      Element request) {
        Element reply;
        try {
            reply = connection.askDumping(request);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Could not send \""
                + request.getTagName() + "\"-message.", e);
            return null;
        }
        
        if (reply != null && "error".equals(reply.getTagName())) {
            String msgID = reply.getAttribute("messageID");
            String msg = reply.getAttribute("message");
            String logMessage = "AIMessage." + request.getTagName()
                + " error,"
                + " messageID: " + ((msgID == null) ? "(null)" : msgID)
                + " message: " + ((msg == null) ? "(null)" : msg);
            logger.warning(logMessage);
            return null;
        }
        return reply;
    }

    /**
     * Sends a DOMMessage to the server.
     *
     * @param connection The <code>Connection</code> to use
     *     when communicating with the server.
     * @param request The <code>Element</code> to send.
     * @return True if the message was sent, and a non-null, non-error
     *     reply returned.
     */
    private static boolean sendMessage(Connection connection,
                                       Element request) {
        return askMessage(connection, request) != null;
    }

    /**
     * Send a message to the server.
     *
     * @param connection The <code>Connection</code> to use
     *     when communicating with the server.
     * @param message The <code>Message</code> to send.
     * @return True if the message was sent, and a non-null, non-error
     *     reply returned.
     */
    private static boolean sendMessage(Connection connection,
                                       DOMMessage message) {
        return (connection != null && message != null)
            ? sendMessage(connection, message.toXMLElement())
            : false;
    }

    /**
     * Send a trivial message.
     *
     * @param connection The <code>Connection</code> to send on.
     * @param tag The tag of the message.
     * @param attributes Attributes to add to the message.
     * @return True if the message was sent, and a non-error reply returned.
     */
    public static boolean sendTrivial(Connection connection, String tag,
                                      String... attributes) {
        return sendMessage(connection, makeTrivial(tag, attributes));
    }

    /**
     * Make a trivial message.
     *
     * @param tag The tag of the message.
     * @param attributes Attributes to add to the message.
     * @return The Element encapsulating the message.
     */
    public static Element makeTrivial(String tag, String... attributes) {
        if ((attributes.length & 1) == 1) {
            throw new IllegalArgumentException("Attributes list must have even length");
        }
        Element element = DOMMessage.createNewRootElement(tag);
        for (int i = 0; i < attributes.length; i += 2) {
            element.setAttribute(attributes[i], attributes[i+1]);
        }
        return element;
    }


    /**
     * An AIUnit attacks in the given direction.
     *
     * @param aiUnit The <code>AIUnit</code> to attack with.
     * @param direction The <code>Direction</code> to attack in.
     * @return True if the message was sent, and a non-error reply returned.
     */
    public static boolean askAttack(AIUnit aiUnit, Direction direction) {
        return sendMessage(aiUnit.getConnection(),
                           new AttackMessage(aiUnit.getUnit(), direction));
    }


    /**
     * An AIUnit builds a colony.
     *
     * @param aiUnit The <code>AIUnit</code> to build the colony.
     * @param name The name of the colony.
     * @return True if the message was sent, and a non-error reply returned.
     */
    public static boolean askBuildColony(AIUnit aiUnit, String name) {
        return sendMessage(aiUnit.getConnection(),
                           new BuildColonyMessage(name, aiUnit.getUnit()));
    }


    /**
     * An AIUnit buys goods.
     *
     * @param aiUnit The <code>AIUnit</code> buys goods.
     * @param type The <code>GoodsType</code> to buy.
     * @param amount The amount of goods to buy.
     * @return True if the message was sent, and a non-error reply returned.
     */
    public static boolean askBuyGoods(AIUnit aiUnit, GoodsType type,
                                      int amount) {
        return sendMessage(aiUnit.getConnection(),
                           new BuyGoodsMessage(aiUnit.getUnit(), type,
                                               amount));
    }


    /**
     * An AIUnit cashes in.
     *
     * @param aiUnit The <code>AIUnit</code> cashing in.
     * @return True if the message was sent, and a non-error reply returned.
     */
    public static boolean askCashInTreasureTrain(AIUnit aiUnit) {
        return sendMessage(aiUnit.getConnection(),
                           new CashInTreasureTrainMessage(aiUnit.getUnit()));
    }


    /**
     * An AIUnit changes state.
     *
     * @param aiUnit The <code>AIUnit</code> to change the state of.
     * @param state The new <code>UnitState</code>.
     * @return True if the message was sent, and a non-error reply returned.
     */
    public static boolean askChangeState(AIUnit aiUnit, UnitState state) {
        return sendMessage(aiUnit.getConnection(),
                           new ChangeStateMessage(aiUnit.getUnit(), state));
    }


    /**
     * An AIUnit changes its work type.
     *
     * @param aiUnit The <code>AIUnit</code> to change the work type of.
     * @param type The <code>GoodsType</code> to produce.
     * @return True if the message was sent, and a non-error reply returned.
     */
    public static boolean askChangeWorkType(AIUnit aiUnit, GoodsType type) {
        return sendMessage(aiUnit.getConnection(),
                           new ChangeWorkTypeMessage(aiUnit.getUnit(), type));
    }


   /**
     * An AIUnit changes its work improvement type.
     *
     * @param aiUnit The <code>AIUnit</code> to change the work type of.
     * @param type The <code>TileImprovementType</code> to produce.
     * @return True if the message was sent, and a non-error reply returned.
     */
    public static boolean askChangeWorkImprovementType(AIUnit aiUnit,
                                                  TileImprovementType type) {
        return sendMessage(aiUnit.getConnection(),
            new ChangeWorkImprovementTypeMessage(aiUnit.getUnit(), type));
    }


    /**
     * Claims a tile for a colony.
     *
     * @param tile The <code>Tile</code> to claim.
     * @param claimant The <code>AIUnit</code> or <code>AIColony</code>
     *     that is claiming.
     * @param price The price to pay.
     * @return True if the message was sent, and a non-error reply returned.
     */
    public static boolean askClaimLand(Tile tile, AIObject claimant,
                                       int price) {
        FreeColGameObject fcgo;
        Player owner;
        if (claimant instanceof AIUnit) {
            fcgo = ((AIUnit)claimant).getUnit();
            owner = ((Unit)fcgo).getOwner();
        } else if (claimant instanceof AIColony) {
            fcgo = ((AIColony)claimant).getColony();
            owner = ((Colony)fcgo).getOwner();
        } else {
            throw new IllegalArgumentException("Claimant must be an AIUnit"
                + " or AIColony: " + claimant.getId());
        }
        return sendMessage(claimant.getAIMain().getAIPlayer(owner)
            .getConnection(), new ClaimLandMessage(tile, fcgo, price));
    }


    /**
     * Clears the speciality of a unit.
     *
     * @param aiUnit The <code>AIUnit</code> to clear.
     * @return True if the message was sent, and a non-error reply returned.
     */
    public static boolean askClearSpeciality(AIUnit aiUnit) {
        return sendMessage(aiUnit.getConnection(),
                           new ClearSpecialityMessage(aiUnit.getUnit()));
    }


    /**
     * An AIUnit closes a transaction.
     *
     * @param aiUnit The <code>AIUnit</code> that closes the transaction.
     * @param settlement The target <code>Settlement</code>.
     * @return True if the message was sent, and a non-error reply returned.
     */
    public static boolean askCloseTransaction(AIUnit aiUnit,
                                              Settlement settlement) {
        return sendMessage(aiUnit.getConnection(),
                           new CloseTransactionMessage(aiUnit.getUnit(),
                                                       settlement));
    }


    /**
     * An AIUnit delivers a gift.
     *
     * @param aiUnit The <code>AIUnit</code> delivering the gift.
     * @param settlement The <code>Settlement</code> to give to.
     * @param goods The <code>Goods</code> to give.
     * @return True if the message was sent, and a non-error reply returned.
     */
    public static boolean askDeliverGift(AIUnit aiUnit, Settlement settlement,
                                         Goods goods) {
        return sendMessage(aiUnit.getConnection(),
                           new DeliverGiftMessage(aiUnit.getUnit(),
                                                  settlement, goods));
    }


    /**
     * An AIUnit disembarks.
     *
     * @param aiUnit The <code>AIUnit</code> delivering the gift.
     * @return True if the message was sent, and a non-error reply returned.
     */
    public static boolean askDisembark(AIUnit aiUnit) {
        return sendMessage(aiUnit.getConnection(),
                           new DisembarkMessage(aiUnit.getUnit()));
    }


    /**
     * An AIUnit embarks.
     *
     * @param aiUnit The <code>AIUnit</code> carrier.
     * @param unit The <code>Unit</code> that is embarking.
     * @param direction The <code>Direction</code> to embark in (may be null).
     * @return True if the message was sent, and a non-error reply returned.
     */
    public static boolean askEmbark(AIUnit aiUnit, Unit unit,
                                    Direction direction) {
        return sendMessage(aiUnit.getConnection(),
                           new EmbarkMessage(unit, aiUnit.getUnit(),
                                             direction));
    }


    /**
     * A unit in Europe emigrates.
     *
     * @param aiPlayer The <code>AIPlayer</code> requiring emigration.
     * @param slot The slot to emigrate from.
     * @return True if the message was sent, and a non-error reply returned.
     */
    public static boolean askEmigrate(AIPlayer aiPlayer, int slot) {
        return sendMessage(aiPlayer.getConnection(),
            new EmigrateUnitMessage(slot));
    }


    /**
     * Ends the player turn.
     *
     * @param aiPlayer The <code>AIPlayer</code> ending the turn.
     * @return True if the message was sent, and a non-error reply returned.
     */
    public static boolean askEndTurn(AIPlayer aiPlayer) {
        return sendTrivial(aiPlayer.getConnection(), "endTurn");
    }


    /**
     * Change the equipment of a unit.
     *
     * @param aiUnit The <code>AIUnit</code> to equip.
     * @param type The <code>EquipmentType</code> to equip with.
     * @param amount The amount to change the equipment by.
     * @return True if the message was sent, and a non-error reply returned.
     */
    public static boolean askEquipUnit(AIUnit aiUnit, EquipmentType type,
                                       int amount) {
        return sendMessage(aiUnit.getConnection(),
                           new EquipUnitMessage(aiUnit.getUnit(), type,
                                                amount));
    }


    /**
     * Establishes a mission in the given direction.
     *
     * @param aiUnit The <code>AIUnit</code> establishing the mission.
     * @param direction The <code>Direction</code> to move the unit.
     * @param denounce Is this a denunciation?
     * @return True if the message was sent, and a non-error reply returned.
     */
    public static boolean askEstablishMission(AIUnit aiUnit,
                                              Direction direction,
                                              boolean denounce) {
        return sendMessage(aiUnit.getConnection(),
                           new MissionaryMessage(aiUnit.getUnit(), direction,
                                                 denounce));
    }


    /**
     * An AIUnit gets a transaction.
     *
     * @param aiUnit The <code>AIUnit</code> that gets a transaction.
     * @param settlement The target <code>Settlement</code>.
     * @return True if the message was sent, and a non-error reply returned.
     */
    public static boolean askGetTransaction(AIUnit aiUnit,
                                            Settlement settlement) {
        return sendMessage(aiUnit.getConnection(),
                           new GetTransactionMessage(aiUnit.getUnit(),
                                                     settlement));
    }


    /**
     * Makes demands to a colony.  One and only one of goods or gold is valid.
     *
     * @param aiUnit The <code>AIUnit</code> that is demanding.
     * @param colony The <code>Colony</code> to demand of.
     * @param goods The <code>Goods</code> to demand.
     * @param gold The amount of gold to demand.
     * @return True if the message was sent, a non-error reply returned, and
     *     the demand was accepted.
     */
    public static boolean askIndianDemand(AIUnit aiUnit, Colony colony,
                                          Goods goods, int gold) {
        Element reply = askMessage(aiUnit.getConnection(),
            new IndianDemandMessage(aiUnit.getUnit(), colony,
                goods, gold).toXMLElement());
        IndianDemandMessage message
            = new IndianDemandMessage(colony.getGame(), reply);
        return (message == null) ? false : message.getResult();
    }


    /**
     * An AI unit loads some cargo.
     *
     * @param aiUnit The <code>AIUnit</code> that is loading.
     * @param goods The <code>Goods</code> to load.
     * @return True if the message was sent, and a non-error reply returned.
     */
    public static boolean askLoadCargo(AIUnit aiUnit, Goods goods) {
        return sendMessage(aiUnit.getConnection(),
                           new LoadCargoMessage(goods, aiUnit.getUnit()));
    }


    /**
     * An AI unit loots some cargo.
     *
     * @param aiUnit The <code>AIUnit</code> that is looting.
     * @param defenderId The id of the defending unit.
     * @param goods A list of <code>Goods</code> to loot.
     * @return True if the message was sent, and a non-error reply returned.
     */
    public static boolean askLoot(AIUnit aiUnit, String defenderId,
                                  List<Goods> goods) {
        return sendMessage(aiUnit.getConnection(),
            new LootCargoMessage(aiUnit.getUnit(), defenderId, goods));
    }


    /**
     * Moves an AIUnit in the given direction.
     *
     * @param aiUnit The <code>AIUnit</code> to move.
     * @param direction The <code>Direction</code> to move the unit.
     * @return True if the message was sent, and a non-error reply returned.
     */
    public static boolean askMove(AIUnit aiUnit, Direction direction) {
        return sendMessage(aiUnit.getConnection(),
                           new MoveMessage(aiUnit.getUnit(), direction));
    }


    /**
     * Moves an AIUnit across the high seas.
     *
     * @param aiUnit The <code>AIUnit</code> to move.
     * @param destination The <code>Location</code> to move to.
     * @return True if the message was sent, and a non-error reply returned.
     */
    public static boolean askMoveTo(AIUnit aiUnit, Location destination) {
        return sendMessage(aiUnit.getConnection(),
                           new MoveToMessage(aiUnit.getUnit(), destination));
    }


    /**
     * An AIUnit is put outside a colony.
     *
     * @param aiUnit The <code>AIUnit</code> to put out.
     * @return True if the message was sent, and a non-error reply returned.
     */
    public static boolean askPutOutsideColony(AIUnit aiUnit) {
        return sendMessage(aiUnit.getConnection(),
                           new PutOutsideColonyMessage(aiUnit.getUnit()));
    }


    /**
     * An AI unit scouts a native settlement.
     *
     * @param aiUnit The <code>AIUnit</code> that is scouting.
     * @param direction The <code>Direction</code> to move.
     * @return True if the message was sent, and a non-error reply returned.
     */
    public static boolean askScoutIndianSettlement(AIUnit aiUnit,
                                                   Direction direction) {
        return sendMessage(aiUnit.getConnection(),
                           new ScoutIndianSettlementMessage(aiUnit.getUnit(),
                                                            direction));
    }


    /**
     * An AI unit sells some cargo.
     *
     * @param aiUnit The <code>AIUnit</code> that is selling.
     * @param goods The <code>Goods</code> to sell.
     * @return True if the message was sent, and a non-error reply returned.
     */
    public static boolean askSellGoods(AIUnit aiUnit, Goods goods) {
        return sendMessage(aiUnit.getConnection(),
                           new SellGoodsMessage(goods, aiUnit.getUnit()));
    }


    /**
     * Set the build queue in a colony.
     *
     * @param aiColony The <code>AIColony</code> that is building.
     * @param queue The list of <code>BuildableType</code>s to build.
     * @return True if the message was sent, and a non-error reply returned.
     */
    public static boolean askSetBuildQueue(AIColony aiColony,
                                           List<BuildableType> queue) {
        return sendMessage(aiColony.getConnection(),
                           new SetBuildQueueMessage(aiColony.getColony(),
                                                    queue));
    }


    /**
     * Train unit in Europe.
     *
     * @param aiPlayer The <code>AIPlayer</code> requiring training.
     * @param type The <code>UnitType</code> to train.
     * @return True if the message was sent, and a non-error reply returned.
     */
    public static boolean askTrainUnitInEurope(AIPlayer aiPlayer,
                                               UnitType type) {
        return sendMessage(aiPlayer.getConnection(),
                           new TrainUnitInEuropeMessage(type));
    }


    /**
     * An AI unit unloads some cargo.
     *
     * @param aiUnit The <code>AIUnit</code> that is unloading.
     * @param goods The <code>Goods</code> to unload.
     * @return True if the message was sent, and a non-error reply returned.
     */
    public static boolean askUnloadCargo(AIUnit aiUnit, Goods goods) {
        return sendMessage(aiUnit.getConnection(),
                           new UnloadCargoMessage(goods));
    }


    /**
     * Set a unit to work in a work location.
     *
     * @param aiUnit The <code>AIUnit</code> to work.
     * @param workLocation The <code>WorkLocation</code> to work in.
     * @return True if the message was sent, and a non-error reply returned.
     */
    public static boolean askWork(AIUnit aiUnit, WorkLocation workLocation) {
        return sendMessage(aiUnit.getConnection(),
                           new WorkMessage(aiUnit.getUnit(), workLocation));
    }
}
