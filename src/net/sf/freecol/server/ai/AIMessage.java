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

package net.sf.freecol.server.ai;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.EquipmentType;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.WorkLocation;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.AttackMessage;
import net.sf.freecol.common.networking.ChangeWorkTypeMessage;
import net.sf.freecol.common.networking.DeliverGiftMessage;
import net.sf.freecol.common.networking.EquipUnitMessage;
import net.sf.freecol.common.networking.GiveIndependenceMessage;
import net.sf.freecol.common.networking.IndianDemandMessage;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.common.networking.MoveMessage;
import net.sf.freecol.common.networking.MissionaryMessage;
import net.sf.freecol.common.networking.WorkMessage;
import net.sf.freecol.server.ai.AIPlayer;
import net.sf.freecol.server.ai.AIUnit;

import org.w3c.dom.Element;


/**
 * Wrapper class for AI message handling.
 */
public class AIMessage {
    private static final Logger logger = Logger.getLogger(AIMessage.class.getName());

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
                                       Message message) {
        if (connection != null && message != null) {
            try {
                Element request = message.toXMLElement();
                Element reply = connection.ask(request);
                if (reply == null) {
                    return false;
                } else if ("error".equals(reply.getTagName())) {
                    String msgID = reply.getAttribute("messageID");
                    String msg = reply.getAttribute("message");
                    String logMessage = "AIMessage." + request.getTagName()
                        + " error,"
                        + " messageID: " + ((msgID == null) ? "(null)" : msgID)
                        + " message: " + ((msg == null) ? "(null)" : msg);
                    logger.warning(logMessage);
                    return false;
                }
                return true;
            } catch (IOException e) {
                logger.log(Level.WARNING, "Could not send \""
                           + message.getType() + "\"-message!", e);
            }
        }
        return false;
    }


    /**
     * An AIUnit attacks in the given direction.
     *
     * @param aiUnit The <code>AIUnit</code> to attack with.
     * @param direction The <code>Direction</code> to attack in.
     * @return True if the message was sent, and a non-error reply returned.
     */
    public static boolean askAttack(AIUnit aiUnit, Direction direction) {
        AIPlayer owner = aiUnit.getOwner();
        return sendMessage(owner.getConnection(),
                           new AttackMessage(aiUnit.getUnit(), direction));
    }


    /**
     * An AIUnit changes its work type.
     *
     * @param aiUnit The <code>AIUnit</code> to change the work type of.
     * @param type The <code>GoodsType</code> to produce.
     * @return True if the message was sent, and a non-error reply returned.
     */
    public static boolean askChangeWorkType(AIUnit aiUnit, GoodsType type) {
        AIPlayer owner = aiUnit.getOwner();
        return sendMessage(owner.getConnection(),
                           new ChangeWorkTypeMessage(aiUnit.getUnit(), type));
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
        AIPlayer owner = aiUnit.getOwner();
        return sendMessage(owner.getConnection(),
                           new DeliverGiftMessage(aiUnit.getUnit(),
                                                  settlement, goods));
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
        AIPlayer owner = aiUnit.getOwner();
        return sendMessage(owner.getConnection(),
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
        AIPlayer owner = aiUnit.getOwner();
        return sendMessage(owner.getConnection(),
                           new MissionaryMessage(aiUnit.getUnit(), direction,
                                                 denounce));
    }

    /**
     * Gives independence to a player.
     *
     * @param aiPlayer The <code>AIPlayer</code> granting independence.
     * @param player The <code>Player</code> gaining independence.
     * @return True if the message was sent, and a non-error reply returned.
     */
    public static boolean askGiveIndependence(AIPlayer aiPlayer,
                                              Player player) {
        return sendMessage(aiPlayer.getConnection(),
                           new GiveIndependenceMessage(player));
    }

    /**
     * Makes demands to a colony.  One and only one of goods or gold is valid.
     *
     * @param aiUnit The <code>AIUnit</code> that is demanding.
     * @param colony The <code>Colony</code> to demand of.
     * @param goods The <code>Goods</code> to demand.
     * @param gold The amount of gold to demand.
     * @return True if the message was sent, and a non-error reply returned.
     */
    public static boolean askIndianDemand(AIUnit aiUnit, Colony colony,
                                          Goods goods, int gold) {
        return sendMessage(aiUnit.getOwner().getConnection(),
                           new IndianDemandMessage(aiUnit.getUnit(), colony,
                                                   goods, gold));
    }

    /**
     * Moves an AIUnit in the given direction.
     *
     * @param aiUnit The <code>AIUnit</code> to move.
     * @param direction The <code>Direction</code> to move the unit.
     * @return True if the message was sent, and a non-error reply returned.
     */
    public static boolean askMove(AIUnit aiUnit, Direction direction) {
        AIPlayer owner = aiUnit.getOwner();
        return sendMessage(owner.getConnection(),
                           new MoveMessage(aiUnit.getUnit(), direction));
    }

    /**
     * Set a unit to work in a work location.
     *
     * @param aiUnit The <code>AIUnit</code> to work.
     * @param workLocation The <code>WorkLocation</code> to work in.
     * @return True if the message was sent, and a non-error reply returned.
     */
    public static boolean askWork(AIUnit aiUnit, WorkLocation workLocation) {
        AIPlayer owner = aiUnit.getOwner();
        return sendMessage(owner.getConnection(),
                           new WorkMessage(aiUnit.getUnit(), workLocation));
    }
}